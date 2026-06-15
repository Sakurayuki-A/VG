package com.vg.core.batch;

import com.vg.core.color.VGColor;
import com.vg.core.command.*;
import com.vg.core.coord.Matrix3;
import com.vg.core.coord.VGCoord;
import com.vg.core.font.VGFont;
import com.vg.core.icon.VGIconFont;

import java.util.ArrayList;
import java.util.List;

public final class VertexBatcher {

    private static final int CORNER_SEGMENTS = 12;

    private VertexBatcher() {
    }

    public static float[] batch(List<? extends DrawCommand> commands, int fbWidth, int fbHeight) {
        if (commands.isEmpty()) {
            return new float[0];
        }

        int totalVertices = 0;
        for (DrawCommand cmd : commands) {
            totalVertices += estimateVertexCount(cmd);
        }

        float[] vertices = new float[totalVertices * 6];
        int idx = 0;

        // 按照命令顺序渲染，保持绘制顺序（后面的覆盖前面的）
        // 阴影应该在它对应的形状之前绘制
        for (DrawCommand cmd : commands) {
            if (cmd instanceof ShadowCommand) {
                idx = emitShadow((ShadowCommand) cmd, fbWidth, fbHeight, vertices, idx);
            } else {
                idx = emitVertices(cmd, fbWidth, fbHeight, vertices, idx);
            }
        }

        return vertices;
    }

    private static int estimateVertexCount(DrawCommand cmd) {
        return switch (cmd) {
            case RectCommand r -> 6;
            case RoundRectCommand rr -> (4 + CORNER_SEGMENTS * 4) * 3;
            case LineCommand l -> 6;
            case TextCommand t -> t.text.codePointCount(0, t.text.length()) * 6;
            case GradientRectCommand g -> 6;
            case GradientRoundRectCommand g -> estimateRoundRectVertices(g);
            case ShadowCommand s -> estimateShadowVertices(s);
            case RawVertexCommand rv -> rv.vertices.length / 6;
            default -> 0;
        };
    }

    private static int emitVertices(DrawCommand cmd, int fbWidth, int fbHeight, float[] dst, int idx) {
        return switch (cmd) {
            case RectCommand r -> emitRect(r, fbWidth, fbHeight, dst, idx);
            case RoundRectCommand rr -> emitRoundRect(rr, fbWidth, fbHeight, dst, idx);
            case LineCommand l -> emitLine(l, fbWidth, fbHeight, dst, idx);
            case TextCommand ignored -> idx;
            case GradientRectCommand g -> emitGradientRect(g, fbWidth, fbHeight, dst, idx);
            case GradientRoundRectCommand g -> emitGradientRoundRect(g, fbWidth, fbHeight, dst, idx);
            case ShadowCommand ignored -> idx;
            case RawVertexCommand rv -> emitRawVertices(rv, dst, idx);
            default -> idx;
        };
    }

    // ── 2D 变换辅助 ──

    /** 应用矩阵变换（像素坐标 → NDC），m=null 时直接转换 */
    private static float tx(float px, float py, float[] m, int fbW) {
        if (m == null) return VGCoord.pixelToNdcX(px, fbW);
        return VGCoord.pixelToNdcX(m[0] * px + m[1] * py + m[2], fbW);
    }

    private static float ty(float px, float py, float[] m, int fbH) {
        if (m == null) return VGCoord.pixelToNdcY(py, fbH);
        return VGCoord.pixelToNdcY(m[3] * px + m[4] * py + m[5], fbH);
    }

    private static int emitRect(RectCommand cmd, int fbWidth, int fbHeight, float[] dst, int idx) {
        float[] m = cmd.matrix;
        float r = VGColor.r(cmd.color());
        float g = VGColor.g(cmd.color());
        float b = VGColor.b(cmd.color());
        float alpha = VGColor.a(cmd.color());

        // 有矩阵时：变换四角为任意四边形（支持旋转）
        if (m != null) {
            float x0 = tx(cmd.x, cmd.y, m, fbWidth);
            float y0 = ty(cmd.x, cmd.y, m, fbHeight);
            float x1 = tx(cmd.x + cmd.width, cmd.y, m, fbWidth);
            float y1 = ty(cmd.x + cmd.width, cmd.y, m, fbHeight);
            float x2 = tx(cmd.x + cmd.width, cmd.y + cmd.height, m, fbWidth);
            float y2 = ty(cmd.x + cmd.width, cmd.y + cmd.height, m, fbHeight);
            float x3 = tx(cmd.x, cmd.y + cmd.height, m, fbWidth);
            float y3 = ty(cmd.x, cmd.y + cmd.height, m, fbHeight);
            return emitQuad4(dst, idx, x0, y0, x1, y1, x2, y2, x3, y3, r, g, b, alpha);
        }

        float x0 = VGCoord.pixelToNdcX(cmd.x, fbWidth);
        float y0 = VGCoord.pixelToNdcY(cmd.y, fbHeight);
        float x1 = VGCoord.pixelToNdcX(cmd.x + cmd.width, fbWidth);
        float y1 = VGCoord.pixelToNdcY(cmd.y + cmd.height, fbHeight);

        return emitQuad(dst, idx, x0, y0, x1, y1, r, g, b, alpha);
    }

    private static int emitRawVertices(RawVertexCommand cmd, float[] dst, int idx) {
        System.arraycopy(cmd.vertices, 0, dst, idx, cmd.vertices.length);
        return idx + cmd.vertices.length;
    }

    private static int emitRoundRect(RoundRectCommand cmd, int fbWidth, int fbHeight, float[] dst, int idx) {
        float[] m = cmd.matrix;
        float px = cmd.x;
        float py = cmd.y;
        float pw = cmd.width;
        float ph = cmd.height;
        float pr = cmd.radius;

        float r = VGColor.r(cmd.color());
        float g = VGColor.g(cmd.color());
        float b = VGColor.b(cmd.color());
        float alpha = VGColor.a(cmd.color());

        if (m != null) {
            // 像素空间生成周长 → 矩阵变换 → NDC
            float innerX0 = px + pr;
            float innerY0 = py + pr;
            float innerX1 = px + pw - pr;
            float innerY1 = py + ph - pr;
            float centerX = px + pw * 0.5f;
            float centerY = py + ph * 0.5f;
            float ndcCx = tx(centerX, centerY, m, fbWidth);
            float ndcCy = ty(centerX, centerY, m, fbHeight);

            int perimeterCount = 4 + 4 * CORNER_SEGMENTS;
            float[] perimX = new float[perimeterCount];
            float[] perimY = new float[perimeterCount];
            int p = 0;

            // TL
            for (int i = 0; i < CORNER_SEGMENTS; i++) {
                float a = (float)(Math.PI - Math.PI * 0.5 * i / CORNER_SEGMENTS);
                float cx = innerX0 + (float)Math.cos(a) * pr;
                float cy = innerY0 - (float)Math.sin(a) * pr;
                perimX[p] = tx(cx, cy, m, fbWidth);
                perimY[p] = ty(cx, cy, m, fbHeight);
                p++;
            }
            perimX[p] = tx(innerX0, py, m, fbWidth);
            perimY[p] = ty(innerX0, py, m, fbHeight);
            p++;

            // TR
            for (int i = 0; i < CORNER_SEGMENTS; i++) {
                float a = (float)(Math.PI * 0.5 - Math.PI * 0.5 * i / CORNER_SEGMENTS);
                float cx = innerX1 + (float)Math.cos(a) * pr;
                float cy = innerY0 - (float)Math.sin(a) * pr;
                perimX[p] = tx(cx, cy, m, fbWidth);
                perimY[p] = ty(cx, cy, m, fbHeight);
                p++;
            }
            perimX[p] = tx(px + pw, innerY0, m, fbWidth);
            perimY[p] = ty(px + pw, innerY0, m, fbHeight);
            p++;

            // BR
            for (int i = 0; i < CORNER_SEGMENTS; i++) {
                float a = (float)(-Math.PI * 0.5 * i / CORNER_SEGMENTS);
                float cx = innerX1 + (float)Math.cos(a) * pr;
                float cy = innerY1 - (float)Math.sin(a) * pr;
                perimX[p] = tx(cx, cy, m, fbWidth);
                perimY[p] = ty(cx, cy, m, fbHeight);
                p++;
            }
            perimX[p] = tx(innerX1, py + ph, m, fbWidth);
            perimY[p] = ty(innerX1, py + ph, m, fbHeight);
            p++;

            // BL
            for (int i = 0; i < CORNER_SEGMENTS; i++) {
                float a = (float)(-Math.PI * 0.5 - Math.PI * 0.5 * i / CORNER_SEGMENTS);
                float cx = innerX0 + (float)Math.cos(a) * pr;
                float cy = innerY1 - (float)Math.sin(a) * pr;
                perimX[p] = tx(cx, cy, m, fbWidth);
                perimY[p] = ty(cx, cy, m, fbHeight);
                p++;
            }
            perimX[p] = tx(px, innerY1, m, fbWidth);
            perimY[p] = ty(px, innerY1, m, fbHeight);
            p++;

            for (int i = 0; i < perimeterCount; i++) {
                int next = (i + 1) % perimeterCount;
                putVertex(dst, idx, ndcCx, ndcCy, r, g, b, alpha); idx += 6;
                putVertex(dst, idx, perimX[i], perimY[i], r, g, b, alpha); idx += 6;
                putVertex(dst, idx, perimX[next], perimY[next], r, g, b, alpha); idx += 6;
            }
            return idx;
        }

        float ax0 = VGCoord.pixelToNdcX(px, fbWidth);
        float ay0 = VGCoord.pixelToNdcY(py, fbHeight);
        float ax1 = VGCoord.pixelToNdcX(px + pw, fbWidth);
        float ay1 = VGCoord.pixelToNdcY(py + ph, fbHeight);

        float arx = pr / fbWidth * 2.0f;
        float ary = pr / fbHeight * 2.0f;

        float cx0 = ax0 + arx;
        float cy0 = ay0 + ary;
        float cx1 = ax1 - arx;
        float cy1 = ay1 - ary;

        float centerX = (ax0 + ax1) * 0.5f;
        float centerY = (ay0 + ay1) * 0.5f;

        int perimeterCount = 4 + 4 * CORNER_SEGMENTS;
        float[] perimeterX = new float[perimeterCount];
        float[] perimeterY = new float[perimeterCount];
        int p = 0;

        for (int i = 0; i < CORNER_SEGMENTS; i++) {
            float a = (float)(Math.PI - Math.PI * 0.5 * i / CORNER_SEGMENTS);
            perimeterX[p] = cx0 + (float)Math.cos(a) * arx;
            perimeterY[p] = cy0 - (float)Math.sin(a) * ary;
            p++;
        }

        perimeterX[p] = cx0; perimeterY[p] = ay0; p++;

        for (int i = 0; i < CORNER_SEGMENTS; i++) {
            float a = (float)(Math.PI * 0.5 - Math.PI * 0.5 * i / CORNER_SEGMENTS);
            perimeterX[p] = cx1 + (float)Math.cos(a) * arx;
            perimeterY[p] = cy0 - (float)Math.sin(a) * ary;
            p++;
        }

        perimeterX[p] = ax1; perimeterY[p] = cy0; p++;

        for (int i = 0; i < CORNER_SEGMENTS; i++) {
            float a = (float)(-Math.PI * 0.5 * i / CORNER_SEGMENTS);
            perimeterX[p] = cx1 + (float)Math.cos(a) * arx;
            perimeterY[p] = cy1 - (float)Math.sin(a) * ary;
            p++;
        }

        perimeterX[p] = cx1; perimeterY[p] = ay1; p++;

        for (int i = 0; i < CORNER_SEGMENTS; i++) {
            float a = (float)(-Math.PI * 0.5 - Math.PI * 0.5 * i / CORNER_SEGMENTS);
            perimeterX[p] = cx0 + (float)Math.cos(a) * arx;
            perimeterY[p] = cy1 - (float)Math.sin(a) * ary;
            p++;
        }

        perimeterX[p] = ax0; perimeterY[p] = cy1; p++;

        for (int i = 0; i < perimeterCount; i++) {
            int next = (i + 1) % perimeterCount;
            putVertex(dst, idx, centerX, centerY, r, g, b, alpha);
            idx += 6;
            putVertex(dst, idx, perimeterX[i], perimeterY[i], r, g, b, alpha);
            idx += 6;
            putVertex(dst, idx, perimeterX[next], perimeterY[next], r, g, b, alpha);
            idx += 6;
        }

        return idx;
    }

    private static int emitLine(LineCommand cmd, int fbWidth, int fbHeight, float[] dst, int idx) {
        float[] m = cmd.matrix;
        float x1 = cmd.x1;
        float y1 = cmd.y1;
        float x2 = cmd.x2;
        float y2 = cmd.y2;
        float lw = cmd.lineWidth * 0.5f;

        float r = VGColor.r(cmd.color());
        float g = VGColor.g(cmd.color());
        float b = VGColor.b(cmd.color());
        float a = VGColor.a(cmd.color());

        float nx1, ny1, nx2, ny2;

        if (m != null) {
            // 变换端点至 NDC，在 NDC 空间计算垂线
            nx1 = tx(x1, y1, m, fbWidth);
            ny1 = ty(x1, y1, m, fbHeight);
            nx2 = tx(x2, y2, m, fbWidth);
            ny2 = ty(x2, y2, m, fbHeight);
        } else {
            nx1 = VGCoord.pixelToNdcX(x1, fbWidth);
            ny1 = VGCoord.pixelToNdcY(y1, fbHeight);
            nx2 = VGCoord.pixelToNdcX(x2, fbWidth);
            ny2 = VGCoord.pixelToNdcY(y2, fbHeight);
        }

        float dx = nx2 - nx1;
        float dy = ny2 - ny1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);

        if (len < 0.0001f) {
            return idx;
        }

        float pnx = -dy / len * lw / fbWidth * 2.0f;
        float pny = -dx / len * lw / fbHeight * 2.0f;

        float qx0 = nx1 + pnx;
        float qy0 = ny1 + pny;
        float qx1 = nx2 + pnx;
        float qy1 = ny2 + pny;
        float qx2 = nx1 - pnx;
        float qy2 = ny1 - pny;
        float qx3 = nx2 - pnx;
        float qy3 = ny2 - pny;

        putVertex(dst, idx, qx0, qy0, r, g, b, a); idx += 6;
        putVertex(dst, idx, qx2, qy2, r, g, b, a); idx += 6;
        putVertex(dst, idx, qx1, qy1, r, g, b, a); idx += 6;
        putVertex(dst, idx, qx1, qy1, r, g, b, a); idx += 6;
        putVertex(dst, idx, qx2, qy2, r, g, b, a); idx += 6;
        putVertex(dst, idx, qx3, qy3, r, g, b, a); idx += 6;

        return idx;
    }

    private static int emitQuad(float[] dst, int idx,
                                float x0, float y0, float x1, float y1,
                                float r, float g, float b, float a) {
        putVertex(dst, idx, x0, y0, r, g, b, a);
        idx += 6;
        putVertex(dst, idx, x1, y0, r, g, b, a);
        idx += 6;
        putVertex(dst, idx, x0, y1, r, g, b, a);
        idx += 6;
        putVertex(dst, idx, x1, y0, r, g, b, a);
        idx += 6;
        putVertex(dst, idx, x1, y1, r, g, b, a);
        idx += 6;
        putVertex(dst, idx, x0, y1, r, g, b, a);
        idx += 6;
        return idx;
    }

    /** 用 4 个独立角点绘制四边形（支持非轴对齐） */
    private static int emitQuad4(float[] dst, int idx,
                                 float x0, float y0, float x1, float y1,
                                 float x2, float y2, float x3, float y3,
                                 float r, float g, float b, float a) {
        putVertex(dst, idx, x0, y0, r, g, b, a); idx += 6;
        putVertex(dst, idx, x1, y1, r, g, b, a); idx += 6;
        putVertex(dst, idx, x2, y2, r, g, b, a); idx += 6;
        putVertex(dst, idx, x0, y0, r, g, b, a); idx += 6;
        putVertex(dst, idx, x2, y2, r, g, b, a); idx += 6;
        putVertex(dst, idx, x3, y3, r, g, b, a); idx += 6;
        return idx;
    }

    private static int emitGradientRect(GradientRectCommand cmd, int fbWidth, int fbHeight,
                                        float[] dst, int idx) {
        float[] m = cmd.matrix;
        float tlR = VGColor.r(cmd.topLeft);
        float tlG = VGColor.g(cmd.topLeft);
        float tlB = VGColor.b(cmd.topLeft);
        float tlA = VGColor.a(cmd.topLeft);

        float trR = VGColor.r(cmd.topRight);
        float trG = VGColor.g(cmd.topRight);
        float trB = VGColor.b(cmd.topRight);
        float trA = VGColor.a(cmd.topRight);

        float blR = VGColor.r(cmd.bottomLeft);
        float blG = VGColor.g(cmd.bottomLeft);
        float blB = VGColor.b(cmd.bottomLeft);
        float blA = VGColor.a(cmd.bottomLeft);

        float brR = VGColor.r(cmd.bottomRight);
        float brG = VGColor.g(cmd.bottomRight);
        float brB = VGColor.b(cmd.bottomRight);
        float brA = VGColor.a(cmd.bottomRight);

        // 有矩阵时：变换四角
        if (m != null) {
            float x0 = tx(cmd.x, cmd.y, m, fbWidth);
            float y0 = ty(cmd.x, cmd.y, m, fbHeight);
            float x1 = tx(cmd.x + cmd.width, cmd.y, m, fbWidth);
            float y1 = ty(cmd.x + cmd.width, cmd.y, m, fbHeight);
            float x2 = tx(cmd.x + cmd.width, cmd.y + cmd.height, m, fbWidth);
            float y2 = ty(cmd.x + cmd.width, cmd.y + cmd.height, m, fbHeight);
            float x3 = tx(cmd.x, cmd.y + cmd.height, m, fbWidth);
            float y3 = ty(cmd.x, cmd.y + cmd.height, m, fbHeight);

            putVertex(dst, idx, x0, y0, tlR, tlG, tlB, tlA); idx += 6;
            putVertex(dst, idx, x1, y1, trR, trG, trB, trA); idx += 6;
            putVertex(dst, idx, x2, y2, brR, brG, brB, brA); idx += 6;
            putVertex(dst, idx, x0, y0, tlR, tlG, tlB, tlA); idx += 6;
            putVertex(dst, idx, x2, y2, brR, brG, brB, brA); idx += 6;
            putVertex(dst, idx, x3, y3, blR, blG, blB, blA); idx += 6;
            return idx;
        }

        float x0 = VGCoord.pixelToNdcX(cmd.x, fbWidth);
        float y0 = VGCoord.pixelToNdcY(cmd.y, fbHeight);
        float x1 = VGCoord.pixelToNdcX(cmd.x + cmd.width, fbWidth);
        float y1 = VGCoord.pixelToNdcY(cmd.y + cmd.height, fbHeight);

        putVertex(dst, idx, x0, y0, tlR, tlG, tlB, tlA); idx += 6;
        putVertex(dst, idx, x1, y0, trR, trG, trB, trA); idx += 6;
        putVertex(dst, idx, x0, y1, blR, blG, blB, blA); idx += 6;
        putVertex(dst, idx, x1, y0, trR, trG, trB, trA); idx += 6;
        putVertex(dst, idx, x1, y1, brR, brG, brB, brA); idx += 6;
        putVertex(dst, idx, x0, y1, blR, blG, blB, blA); idx += 6;

        return idx;
    }

    private static int emitGradientRoundRect(GradientRoundRectCommand cmd, int fbWidth, int fbHeight,
                                             float[] dst, int idx) {
        float[] m = cmd.matrix;
        float px = cmd.x;
        float py = cmd.y;
        float pw = cmd.width;
        float ph = cmd.height;
        float pr = cmd.radius;

        // 提取四角颜色分量
        float tlR = VGColor.r(cmd.topLeft), tlG = VGColor.g(cmd.topLeft), tlB = VGColor.b(cmd.topLeft), tlA = VGColor.a(cmd.topLeft);
        float trR = VGColor.r(cmd.topRight), trG = VGColor.g(cmd.topRight), trB = VGColor.b(cmd.topRight), trA = VGColor.a(cmd.topRight);
        float blR = VGColor.r(cmd.bottomLeft), blG = VGColor.g(cmd.bottomLeft), blB = VGColor.b(cmd.bottomLeft), blA = VGColor.a(cmd.bottomLeft);
        float brR = VGColor.r(cmd.bottomRight), brG = VGColor.g(cmd.bottomRight), brB = VGColor.b(cmd.bottomRight), brA = VGColor.a(cmd.bottomRight);

        // 中心颜色（四角平均）
        float cR = (tlR + trR + blR + brR) * 0.25f;
        float cG = (tlG + trG + blG + brG) * 0.25f;
        float cB = (tlB + trB + blB + brB) * 0.25f;
        float cA = (tlA + trA + blA + brA) * 0.25f;

        // 像素空间的内矩形边界（用于生成周长）
        float innerX0 = px + pr;
        float innerY0 = py + pr;
        float innerX1 = px + pw - pr;
        float innerY1 = py + ph - pr;

        int perimeterCount = 4 + 4 * CORNER_SEGMENTS;
        float[] perimX = new float[perimeterCount];
        float[] perimY = new float[perimeterCount];
        int p = 0;

        // 在像素空间生成周长点，同时或之后变换
        float[] rawX = new float[perimeterCount];
        float[] rawY = new float[perimeterCount];

        // TL arc
        for (int i = 0; i < CORNER_SEGMENTS; i++) {
            float a = (float)(Math.PI - Math.PI * 0.5 * i / CORNER_SEGMENTS);
            rawX[p] = innerX0 + (float)Math.cos(a) * pr;
            rawY[p] = innerY0 - (float)Math.sin(a) * pr;
            p++;
        }
        rawX[p] = innerX0; rawY[p] = py; p++;

        // TR arc
        for (int i = 0; i < CORNER_SEGMENTS; i++) {
            float a = (float)(Math.PI * 0.5 - Math.PI * 0.5 * i / CORNER_SEGMENTS);
            rawX[p] = innerX1 + (float)Math.cos(a) * pr;
            rawY[p] = innerY0 - (float)Math.sin(a) * pr;
            p++;
        }
        rawX[p] = px + pw; rawY[p] = innerY0; p++;

        // BR arc
        for (int i = 0; i < CORNER_SEGMENTS; i++) {
            float a = (float)(-Math.PI * 0.5 * i / CORNER_SEGMENTS);
            rawX[p] = innerX1 + (float)Math.cos(a) * pr;
            rawY[p] = innerY1 - (float)Math.sin(a) * pr;
            p++;
        }
        rawX[p] = innerX1; rawY[p] = py + ph; p++;

        // BL arc
        for (int i = 0; i < CORNER_SEGMENTS; i++) {
            float a = (float)(-Math.PI * 0.5 - Math.PI * 0.5 * i / CORNER_SEGMENTS);
            rawX[p] = innerX0 + (float)Math.cos(a) * pr;
            rawY[p] = innerY1 - (float)Math.sin(a) * pr;
            p++;
        }
        rawX[p] = px; rawY[p] = innerY1; p++;

        // 变换所有点并准备 UV 插值
        if (m != null) {
            float ndcCx = tx(px + pw * 0.5f, py + ph * 0.5f, m, fbWidth);
            float ndcCy = ty(px + pw * 0.5f, py + ph * 0.5f, m, fbHeight);
            for (int i = 0; i < perimeterCount; i++) {
                perimX[i] = tx(rawX[i], rawY[i], m, fbWidth);
                perimY[i] = ty(rawX[i], rawY[i], m, fbHeight);
            }
            float invW = 1f / pw;
            float invH = 1f / ph;
            for (int i = 0; i < perimeterCount; i++) {
                int next = (i + 1) % perimeterCount;
                putVertex(dst, idx, ndcCx, ndcCy, cR, cG, cB, cA); idx += 6;

                float u0 = (rawX[i] - px) * invW;
                float v0 = (rawY[i] - py) * invH;
                float r0 = lerp(lerp(tlR, trR, u0), lerp(blR, brR, u0), v0);
                float g0 = lerp(lerp(tlG, trG, u0), lerp(blG, brG, u0), v0);
                float b0 = lerp(lerp(tlB, trB, u0), lerp(blB, brB, u0), v0);
                float a0 = lerp(lerp(tlA, trA, u0), lerp(blA, brA, u0), v0);
                putVertex(dst, idx, perimX[i], perimY[i], r0, g0, b0, a0); idx += 6;

                float u1 = (rawX[next] - px) * invW;
                float v1 = (rawY[next] - py) * invH;
                float r1 = lerp(lerp(tlR, trR, u1), lerp(blR, brR, u1), v1);
                float g1 = lerp(lerp(tlG, trG, u1), lerp(blG, brG, u1), v1);
                float b1 = lerp(lerp(tlB, trB, u1), lerp(blB, brB, u1), v1);
                float a1 = lerp(lerp(tlA, trA, u1), lerp(blA, brA, u1), v1);
                putVertex(dst, idx, perimX[next], perimY[next], r1, g1, b1, a1); idx += 6;
            }
        } else {
            float ndcCx = VGCoord.pixelToNdcX(px + pw * 0.5f, fbWidth);
            float ndcCy = VGCoord.pixelToNdcY(py + ph * 0.5f, fbHeight);
            for (int i = 0; i < perimeterCount; i++) {
                perimX[i] = VGCoord.pixelToNdcX(rawX[i], fbWidth);
                perimY[i] = VGCoord.pixelToNdcY(rawY[i], fbHeight);
            }
            float invW = 1f / pw;
            float invH = 1f / ph;
            for (int i = 0; i < perimeterCount; i++) {
                int next = (i + 1) % perimeterCount;
                putVertex(dst, idx, ndcCx, ndcCy, cR, cG, cB, cA); idx += 6;

                float u0 = (rawX[i] - px) * invW;
                float v0 = (rawY[i] - py) * invH;
                float r0 = lerp(lerp(tlR, trR, u0), lerp(blR, brR, u0), v0);
                float g0 = lerp(lerp(tlG, trG, u0), lerp(blG, brG, u0), v0);
                float b0 = lerp(lerp(tlB, trB, u0), lerp(blB, brB, u0), v0);
                float a0 = lerp(lerp(tlA, trA, u0), lerp(blA, brA, u0), v0);
                putVertex(dst, idx, perimX[i], perimY[i], r0, g0, b0, a0); idx += 6;

                float u1 = (rawX[next] - px) * invW;
                float v1 = (rawY[next] - py) * invH;
                float r1 = lerp(lerp(tlR, trR, u1), lerp(blR, brR, u1), v1);
                float g1 = lerp(lerp(tlG, trG, u1), lerp(blG, brG, u1), v1);
                float b1 = lerp(lerp(tlB, trB, u1), lerp(blB, brB, u1), v1);
                float a1 = lerp(lerp(tlA, trA, u1), lerp(blA, brA, u1), v1);
                putVertex(dst, idx, perimX[next], perimY[next], r1, g1, b1, a1); idx += 6;
            }
        }

        return idx;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static int emitShadow(ShadowCommand cmd, int fbWidth, int fbHeight,
                                   float[] dst, int idx) {
        float[] m = cmd.matrix;

        // 优化的多层模糊：增加层数，使用更平滑的衰减函数
        float blur = cmd.blur;
        float spread = cmd.spread;
        
        // 大幅增加层数以减少阶梯效果
        // 层数与模糊半径成正比，最少 20 层
        int layers = Math.max(20, (int)(blur * 1.5f));
        
        float colorR = VGColor.r(cmd.color());
        float colorG = VGColor.g(cmd.color());
        float colorB = VGColor.b(cmd.color());
        float baseAlpha = VGColor.a(cmd.color());
        
        // 内部矩形（实际内容区域）
        float innerX = cmd.x;
        float innerY = cmd.y;
        float innerW = cmd.width;
        float innerH = cmd.height;
        
        // 从内到外绘制多层，alpha 逐渐降低
        for (int i = 0; i < layers; i++) {
            float t = (float)i / (float)(layers - 1); // 0.0 到 1.0
            
            // 扩展量：从 spread 到 blur + spread
            float expansion = spread + t * blur;
            
            // 当前层的边界
            float x0 = innerX - expansion;
            float y0 = innerY - expansion;
            float x1 = innerX + innerW + expansion;
            float y1 = innerY + innerH + expansion;

            if (m != null) {
                float smoothT = t * t * (3.0f - 2.0f * t);
                float alpha = baseAlpha * (1.0f - smoothT);

                float nx0 = tx(x0, y0, m, fbWidth);
                float ny0 = ty(x0, y0, m, fbHeight);
                float nx1 = tx(x1, y0, m, fbWidth);
                float ny1 = ty(x1, y0, m, fbHeight);
                float nx2 = tx(x1, y1, m, fbWidth);
                float ny2 = ty(x1, y1, m, fbHeight);
                float nx3 = tx(x0, y1, m, fbWidth);
                float ny3 = ty(x0, y1, m, fbHeight);

                putVertex(dst, idx, nx0, ny0, colorR, colorG, colorB, alpha); idx += 6;
                putVertex(dst, idx, nx1, ny1, colorR, colorG, colorB, alpha); idx += 6;
                putVertex(dst, idx, nx2, ny2, colorR, colorG, colorB, alpha); idx += 6;
                putVertex(dst, idx, nx0, ny0, colorR, colorG, colorB, alpha); idx += 6;
                putVertex(dst, idx, nx2, ny2, colorR, colorG, colorB, alpha); idx += 6;
                putVertex(dst, idx, nx3, ny3, colorR, colorG, colorB, alpha); idx += 6;
            } else {
                // 使用三次方衰减，比二次方更平滑
                // smoothstep 函数：3t² - 2t³ 提供更自然的过渡
                float smoothT = t * t * (3.0f - 2.0f * t);
                float alpha = baseAlpha * (1.0f - smoothT);
                
                // 转换到 NDC
                float rx0 = VGCoord.pixelToNdcX(x0, fbWidth);
                float ry0 = VGCoord.pixelToNdcY(y0, fbHeight);
                float rx1 = VGCoord.pixelToNdcX(x1, fbWidth);
                float ry1 = VGCoord.pixelToNdcY(y1, fbHeight);
                
                // 绘制矩形（两个三角形）
                putVertex(dst, idx, rx0, ry0, colorR, colorG, colorB, alpha); idx += 6;
                putVertex(dst, idx, rx0, ry1, colorR, colorG, colorB, alpha); idx += 6;
                putVertex(dst, idx, rx1, ry0, colorR, colorG, colorB, alpha); idx += 6;
                putVertex(dst, idx, rx0, ry1, colorR, colorG, colorB, alpha); idx += 6;
                putVertex(dst, idx, rx1, ry1, colorR, colorG, colorB, alpha); idx += 6;
                putVertex(dst, idx, rx1, ry0, colorR, colorG, colorB, alpha); idx += 6;
            }
        }

        return idx;
    }

    private static int estimateShadowVertices(ShadowCommand cmd) {
        // 估算层数
        int layers = Math.max(20, (int)(cmd.blur * 1.5f));
        // 每层 2 个三角形 = 6 个顶点
        return layers * 6;
    }

    private static int estimateRoundRectVertices(DrawCommand cmd) {
        // 圆角矩形使用扇形三角化，顶点数 = 中心1 + 周长点
        int perimeterCount = 4 + 4 * CORNER_SEGMENTS;
        return perimeterCount * 3; // 每个周长点与中心形成三角形
    }

    private static int max(int a, int b) {
        return a >= b ? a : b;
    }

    private static void putVertex(float[] dst, int idx, float x, float y, float r, float g, float b, float a) {
        dst[idx] = x;
        dst[idx + 1] = y;
        dst[idx + 2] = r;
        dst[idx + 3] = g;
        dst[idx + 4] = b;
        dst[idx + 5] = a;
    }

    public static List<DrawCommand> filterShapeCommands(List<? extends DrawCommand> commands) {
        List<DrawCommand> shapes = new ArrayList<>();
        for (DrawCommand cmd : commands) {
            if (!(cmd instanceof TextCommand) && !(cmd instanceof IconCommand) && !(cmd instanceof ShadowCommand)) {
                shapes.add(cmd);
            }
        }
        return shapes;
    }

    public static List<DrawCommand> filterTextCommands(List<? extends DrawCommand> commands) {
        List<DrawCommand> texts = new ArrayList<>();
        for (DrawCommand cmd : commands) {
            if (cmd instanceof TextCommand) {
                texts.add(cmd);
            }
        }
        return texts;
    }

    public static List<DrawCommand> filterIconCommands(List<? extends DrawCommand> commands) {
        List<DrawCommand> icons = new ArrayList<>();
        for (DrawCommand cmd : commands) {
            if (cmd instanceof IconCommand) {
                icons.add(cmd);
            }
        }
        return icons;
    }
    
    public static List<DrawCommand> filterShadowCommands(List<? extends DrawCommand> commands) {
        List<DrawCommand> shadows = new ArrayList<>();
        for (DrawCommand cmd : commands) {
            if (cmd instanceof ShadowCommand) {
                shadows.add(cmd);
            }
        }
        return shadows;
    }

    /**
     * 使用 SDF 方式批处理阴影命令
     * 顶点格式：position(2) + color(3) + alpha(1) + shadowRect(4) + blurParams(2) = 12 floats
     * 
     * 注意：这需要使用专用的阴影 pipeline (VKPipelineShadow)
     */
    public static float[] batchShadowsSDF(List<DrawCommand> shadowCommands, int fbWidth, int fbHeight) {
        if (shadowCommands.isEmpty()) {
            return new float[0];
        }

        // 每个阴影 1 个矩形 = 6 个顶点，每个顶点 12 floats
        int totalFloats = shadowCommands.size() * 6 * 12;
        float[] vertices = new float[totalFloats];
        int idx = 0;

        for (DrawCommand cmd : shadowCommands) {
            if (cmd instanceof ShadowCommand shadow) {
                float colorR = VGColor.r(shadow.color());
                float colorG = VGColor.g(shadow.color());
                float colorB = VGColor.b(shadow.color());
                float baseAlpha = VGColor.a(shadow.color());
                
                // 阴影区域：原矩形 + spread + blur
                float totalExpansion = shadow.spread + shadow.blur;
                
                float x0 = shadow.x - totalExpansion;
                float y0 = shadow.y - totalExpansion;
                float x1 = shadow.x + shadow.width + totalExpansion;
                float y1 = shadow.y + shadow.height + totalExpansion;
                
                // 转换到 NDC
                float rx0 = VGCoord.pixelToNdcX(x0, fbWidth);
                float ry0 = VGCoord.pixelToNdcY(y0, fbHeight);
                float rx1 = VGCoord.pixelToNdcX(x1, fbWidth);
                float ry1 = VGCoord.pixelToNdcY(y1, fbHeight);
                
                // 阴影矩形参数（像素坐标）
                float shadowX = shadow.x;
                float shadowY = shadow.y;
                float shadowW = shadow.width;
                float shadowH = shadow.height;
                
                // 绘制矩形（两个三角形），每个顶点包含完整的阴影参数
                idx = putShadowVertex(vertices, idx, rx0, ry0, colorR, colorG, colorB, baseAlpha,
                        shadowX, shadowY, shadowW, shadowH, shadow.spread, shadow.blur);
                idx = putShadowVertex(vertices, idx, rx0, ry1, colorR, colorG, colorB, baseAlpha,
                        shadowX, shadowY, shadowW, shadowH, shadow.spread, shadow.blur);
                idx = putShadowVertex(vertices, idx, rx1, ry0, colorR, colorG, colorB, baseAlpha,
                        shadowX, shadowY, shadowW, shadowH, shadow.spread, shadow.blur);
                
                idx = putShadowVertex(vertices, idx, rx0, ry1, colorR, colorG, colorB, baseAlpha,
                        shadowX, shadowY, shadowW, shadowH, shadow.spread, shadow.blur);
                idx = putShadowVertex(vertices, idx, rx1, ry1, colorR, colorG, colorB, baseAlpha,
                        shadowX, shadowY, shadowW, shadowH, shadow.spread, shadow.blur);
                idx = putShadowVertex(vertices, idx, rx1, ry0, colorR, colorG, colorB, baseAlpha,
                        shadowX, shadowY, shadowW, shadowH, shadow.spread, shadow.blur);
            }
        }

        return vertices;
    }

    private static int putShadowVertex(float[] dst, int idx, 
                                       float x, float y, float r, float g, float b, float a,
                                       float shadowX, float shadowY, float shadowW, float shadowH,
                                       float spread, float blur) {
        dst[idx++] = x;         // position.x
        dst[idx++] = y;         // position.y
        dst[idx++] = r;         // color.r
        dst[idx++] = g;         // color.g
        dst[idx++] = b;         // color.b
        dst[idx++] = a;         // alpha
        dst[idx++] = shadowX;   // shadowRect.x
        dst[idx++] = shadowY;   // shadowRect.y
        dst[idx++] = shadowW;   // shadowRect.width
        dst[idx++] = shadowH;   // shadowRect.height
        dst[idx++] = spread;    // spread
        dst[idx++] = blur;      // blur
        return idx;
    }

    public static float[] batchText(List<DrawCommand> textCommands, int fbWidth, int fbHeight, VGFont defaultFont) {
        if (textCommands.isEmpty()) {
            return new float[0];
        }

        int totalVertices = 0;
        for (DrawCommand cmd : textCommands) {
            if (cmd instanceof TextCommand t) {
                totalVertices += t.text.codePointCount(0, t.text.length()) * 6;
            }
        }

        float[] vertices = new float[totalVertices * 7];
        int idx = 0;

        for (DrawCommand cmd : textCommands) {
            if (cmd instanceof TextCommand t) {
                VGFont font = t.font != null ? t.font : defaultFont;
                if (font == null) continue;
                float[] quads = font.textQuads(t.text, t.x, t.y, fbWidth, fbHeight, t.color());
                float[] m = t.matrix;
                if (m != null) {
                    for (int j = 0; j < quads.length; j += 7) {
                        float px = VGCoord.ndcToPixelX(quads[j], fbWidth);
                        float py = VGCoord.ndcToPixelY(quads[j + 1], fbHeight);
                        quads[j]     = VGCoord.pixelToNdcX(m[0] * px + m[1] * py + m[2], fbWidth);
                        quads[j + 1] = VGCoord.pixelToNdcY(m[3] * px + m[4] * py + m[5], fbHeight);
                    }
                }
                System.arraycopy(quads, 0, vertices, idx, quads.length);
                idx += quads.length;
            }
        }

        return vertices;
    }

    public static float[] batchIcons(List<DrawCommand> iconCommands, int fbWidth, int fbHeight, VGIconFont iconFont) {
        if (iconCommands.isEmpty()) {
            return new float[0];
        }

        int totalVertices = iconCommands.size() * 6;

        float[] vertices = new float[totalVertices * 7];
        int idx = 0;

        for (DrawCommand cmd : iconCommands) {
            if (cmd instanceof IconCommand ic) {
                float[] quads = iconFont.iconQuad(ic.icon, ic.x, ic.y, ic.size, fbWidth, fbHeight, ic.color);
                if (quads.length > 0) {
                    float[] m = ic.matrix;
                    if (m != null) {
                        for (int j = 0; j < quads.length; j += 7) {
                            float px = VGCoord.ndcToPixelX(quads[j], fbWidth);
                            float py = VGCoord.ndcToPixelY(quads[j + 1], fbHeight);
                            quads[j]     = VGCoord.pixelToNdcX(m[0] * px + m[1] * py + m[2], fbWidth);
                            quads[j + 1] = VGCoord.pixelToNdcY(m[3] * px + m[4] * py + m[5], fbHeight);
                        }
                    }
                    System.arraycopy(quads, 0, vertices, idx, quads.length);
                    idx += quads.length;
                }
            }
        }

        if (idx < vertices.length) {
            float[] trimmed = new float[idx];
            System.arraycopy(vertices, 0, trimmed, 0, idx);
            return trimmed;
        }

        return vertices;
    }
}
