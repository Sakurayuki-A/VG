package com.vg.core.coord;

/**
 * 3×3 仿射变换矩阵（行主序）。
 * <p>
 * 布局：{ m00, m01, m02, m10, m11, m12, 0, 0, 1 }
 * <pre>
 * | m00  m01  m02 |   | x |
 * | m10  m11  m12 | * | y |
 * |  0    0    1  |   | 1 |
 * </pre>
 * x' = m00·x + m01·y + m02
 * y' = m10·x + m11·y + m12
 */
public final class Matrix3 {

    private Matrix3() {
    }

    /** 返回 9 元素单位矩阵：{ 1,0,0, 0,1,0, 0,0,1 } */
    public static float[] identity() {
        return new float[]{1, 0, 0, 0, 1, 0, 0, 0, 1};
    }

    /** 复制矩阵 */
    public static float[] copy(float[] a) {
        return new float[]{a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8]};
    }

    /** 判断是否为单位矩阵 */
    public static boolean isIdentity(float[] m) {
        return m == null || (m[0] == 1 && m[1] == 0 && m[2] == 0
                          && m[3] == 0 && m[4] == 1 && m[5] == 0
                          && m[6] == 0 && m[7] == 0 && m[8] == 1);
    }

    // ── 构造基本变换矩阵 ──

    /** 平移矩阵：out = translate(tx, ty) */
    public static float[] translation(float tx, float ty) {
        return new float[]{1, 0, tx, 0, 1, ty, 0, 0, 1};
    }

    /** 缩放矩阵：out = scale(sx, sy) */
    public static float[] scaling(float sx, float sy) {
        return new float[]{sx, 0, 0, 0, sy, 0, 0, 0, 1};
    }

    /** 旋转矩阵：out = rotate(degrees) — 围绕原点顺时针旋转 */
    public static float[] rotation(float degrees) {
        float rad = (float) Math.toRadians(degrees);
        float c = (float) Math.cos(rad);
        float s = (float) Math.sin(rad);
        // 顺时针旋转 (y 轴向下是屏幕坐标惯例)
        return new float[]{c, s, 0, -s, c, 0, 0, 0, 1};
    }

    // ── 复合（原地修改） ──

    /** out = out * translate(tx, ty) */
    public static void translate(float[] out, float tx, float ty) {
        multiplyRight(out, translation(tx, ty));
    }

    /** out = out * scale(sx, sy) */
    public static void scale(float[] out, float sx, float sy) {
        multiplyRight(out, scaling(sx, sy));
    }

    /** out = out * rotate(degrees) */
    public static void rotate(float[] out, float degrees) {
        multiplyRight(out, rotation(degrees));
    }

    // ── 矩阵乘法 ──

    /** out = a * b（行主序 3×3） */
    public static void multiply(float[] out, float[] a, float[] b) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                float sum = 0;
                for (int k = 0; k < 3; k++) {
                    sum += a[row * 3 + k] * b[k * 3 + col];
                }
                out[row * 3 + col] = sum;
            }
        }
    }

    /** out = out * b */
    public static void multiplyRight(float[] out, float[] b) {
        float[] tmp = copy(out);
        multiply(out, tmp, b);
    }

    // ── 点变换 ──

    /** 变换一个像素坐标点，返回变换后的新坐标 (x', y') */
    public static float[] transform(float[] m, float px, float py) {
        if (m == null) return new float[]{px, py};
        return new float[]{
                m[0] * px + m[1] * py + m[2],
                m[3] * px + m[4] * py + m[5]
        };
    }

    /** 变换一个点（原地修改 float[2]） */
    public static void transformInPlace(float[] m, float[] point) {
        if (m == null) return;
        float x = point[0], y = point[1];
        point[0] = m[0] * x + m[1] * y + m[2];
        point[1] = m[3] * x + m[4] * y + m[5];
    }
}