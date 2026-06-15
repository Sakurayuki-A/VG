package com.vg.core.icon;

import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackRange;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class VGIconFont implements AutoCloseable {

    private static final int ATLAS_WIDTH = 512;
    private static final int ATLAS_HEIGHT = 512;

    private final float fontSize;
    private final ByteBuffer ttfData;
    private final ByteBuffer rgbaPixels;
    private final int atlasWidth;
    private final int atlasHeight;
    private final Map<Integer, GlyphInfo> glyphs;
    private final float ascent;
    private final float descent;

    public VGIconFont(String fontPath, float fontSize) {
        this.fontSize = fontSize;
        this.atlasWidth = ATLAS_WIDTH;
        this.atlasHeight = ATLAS_HEIGHT;

        try {
            ttfData = loadFile(fontPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load icon font: " + fontPath, e);
        }

        Icons[] icons = Icons.values();
        int[] codepoints = new int[icons.length];
        for (int i = 0; i < icons.length; i++) {
            codepoints[i] = icons[i].codepoint();
        }

        STBTTPackedchar.Buffer chardata = STBTTPackedchar.malloc(icons.length);
        ByteBuffer atlasGray = MemoryUtil.memAlloc(atlasWidth * atlasHeight);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            STBTTPackContext pc = STBTTPackContext.malloc(stack);
            STBTruetype.stbtt_PackBegin(pc, atlasGray, atlasWidth, atlasHeight, 0, 1, MemoryUtil.NULL);

            STBTTFontinfo fontInfo = STBTTFontinfo.malloc();
            STBTruetype.stbtt_InitFont(fontInfo, ttfData, 0);
            int[] ascentArr = new int[1], descentArr = new int[1], lineGapArr = new int[1];
            STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascentArr, descentArr, lineGapArr);
            float scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, fontSize);
            this.ascent = ascentArr[0] * scale;
            this.descent = Math.abs(descentArr[0] * scale);
            fontInfo.free();

            STBTTPackRange.Buffer ranges = STBTTPackRange.malloc(1);
            IntBuffer cpBuffer = stack.ints(codepoints);
            ranges.get(0)
                    .font_size(fontSize)
                    .first_unicode_codepoint_in_range(0)
                    .array_of_unicode_codepoints(cpBuffer)
                    .num_chars(icons.length)
                    .chardata_for_range(chardata);
            STBTruetype.stbtt_PackFontRanges(pc, ttfData, 0, ranges);
            ranges.free();

            STBTruetype.stbtt_PackEnd(pc);
        }

        glyphs = new HashMap<>();
        for (int i = 0; i < icons.length; i++) {
            int cp = codepoints[i];
            STBTTPackedchar pcData = chardata.get(i);

            float u0 = (pcData.x0() & 0xFFFF) / (float) atlasWidth;
            float v0 = (pcData.y0() & 0xFFFF) / (float) atlasHeight;
            float u1 = (pcData.x1() & 0xFFFF) / (float) atlasWidth;
            float v1 = (pcData.y1() & 0xFFFF) / (float) atlasHeight;

            float offsetX = pcData.xoff();
            float offsetY = pcData.yoff();
            float width = pcData.xoff2() - pcData.xoff();
            float height = pcData.yoff2() - pcData.yoff();
            float advanceWidth = pcData.xadvance();

            glyphs.put(cp, new GlyphInfo(cp, u0, v0, u1, v1,
                    width, height, offsetX, offsetY, advanceWidth));
        }

        chardata.free();

        rgbaPixels = convertGrayToRgba(atlasGray, atlasWidth, atlasHeight);
        MemoryUtil.memFree(atlasGray);

        System.out.println("[VG] Icon font loaded: " + fontPath + " (" + fontSize + "px, " + glyphs.size() + " glyphs)");
    }

    public GlyphInfo getGlyph(int codepoint) {
        return glyphs.get(codepoint);
    }

    public float[] iconQuad(Icons icon, float x, float y, float size,
                             int fbWidth, int fbHeight, int color) {
        GlyphInfo glyph = glyphs.get(icon.codepoint());
        if (glyph == null) return new float[0];

        float scale = size / fontSize;

        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        // 图标字形实际尺寸（缩放后）
        float glyphW = glyph.width * scale;
        float glyphH = glyph.height * scale;
        // 将图标居中于 (x, y) ~ (x+size, y+size) 区域内
        float drawX = x + (size - glyphW) / 2f;
        float drawY = y + (size - glyphH) / 2f;

        float ndcX0 = pixelToNdcX(drawX, fbWidth);
        float ndcY0 = pixelToNdcY(drawY, fbHeight);
        float ndcX1 = pixelToNdcX(drawX + glyphW, fbWidth);
        float ndcY1 = pixelToNdcY(drawY + glyphH, fbHeight);

        float[] vertices = new float[6 * 7];
        int idx = 0;

        // 左上角坐标系：不翻转纹理 V 坐标，因为 top 是 ndcY0，bottom 是 ndcY1
        putVertex(vertices, idx, ndcX0, ndcY0, glyph.u0, glyph.v0, r, g, b); idx += 7;
        putVertex(vertices, idx, ndcX0, ndcY1, glyph.u0, glyph.v1, r, g, b); idx += 7;
        putVertex(vertices, idx, ndcX1, ndcY0, glyph.u1, glyph.v0, r, g, b); idx += 7;
        putVertex(vertices, idx, ndcX0, ndcY1, glyph.u0, glyph.v1, r, g, b); idx += 7;
        putVertex(vertices, idx, ndcX1, ndcY1, glyph.u1, glyph.v1, r, g, b); idx += 7;
        putVertex(vertices, idx, ndcX1, ndcY0, glyph.u1, glyph.v0, r, g, b); idx += 7;

        return vertices;
    }

    public float measureIcon(Icons icon) {
        GlyphInfo g = glyphs.get(icon.codepoint());
        return g != null ? g.advanceWidth : 0;
    }

    public ByteBuffer getRgbaPixels() {
        return rgbaPixels;
    }

    public int getAtlasWidth() {
        return atlasWidth;
    }

    public int getAtlasHeight() {
        return atlasHeight;
    }

    public float getFontSize() {
        return fontSize;
    }

    public float getAscent() {
        return ascent;
    }

    public float getDescent() {
        return descent;
    }

    public float centerY(float containerY, float containerHeight, float size) {
        return containerY + (containerHeight - size) / 2f;
    }

    public float getVisualBaseline(Icons icon, float containerY, float containerHeight, float size) {
        return centerY(containerY, containerHeight, size);
    }

    private static float pixelToNdcX(float pixelX, int fbWidth) {
        return (pixelX / fbWidth) * 2.0f - 1.0f;
    }

    private static float pixelToNdcY(float pixelY, int fbHeight) {
        // Vulkan NDC: Y=-1 在顶部，向下增加
        return (pixelY / fbHeight) * 2.0f - 1.0f;
    }

    private static void putVertex(float[] dst, int idx,
                                   float x, float y, float u, float v,
                                   float r, float g, float b) {
        dst[idx] = x;
        dst[idx + 1] = y;
        dst[idx + 2] = u;
        dst[idx + 3] = v;
        dst[idx + 4] = r;
        dst[idx + 5] = g;
        dst[idx + 6] = b;
    }

    private static ByteBuffer loadFile(String path) throws IOException {
        Path filePath = Path.of(path);
        if (Files.exists(filePath)) {
            byte[] bytes = Files.readAllBytes(filePath);
            ByteBuffer buf = MemoryUtil.memAlloc(bytes.length);
            buf.put(bytes).flip();
            return buf;
        }

        try (InputStream is = VGIconFont.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Icon font file not found: " + path);
            }
            byte[] bytes = is.readAllBytes();
            ByteBuffer buf = MemoryUtil.memAlloc(bytes.length);
            buf.put(bytes).flip();
            return buf;
        }
    }

    private static ByteBuffer convertGrayToRgba(ByteBuffer gray, int w, int h) {
        ByteBuffer rgba = MemoryUtil.memAlloc(w * h * 4);
        for (int i = 0; i < w * h; i++) {
            byte v = gray.get(i);
            int idx = i * 4;
            rgba.put(idx, (byte) 255);
            rgba.put(idx + 1, (byte) 255);
            rgba.put(idx + 2, (byte) 255);
            rgba.put(idx + 3, v);
        }
        rgba.flip();
        return rgba;
    }

    @Override
    public void close() {
        if (rgbaPixels != null) {
            MemoryUtil.memFree(rgbaPixels);
        }
        if (ttfData != null) {
            MemoryUtil.memFree(ttfData);
        }
    }

    public record GlyphInfo(int codepoint, float u0, float v0, float u1, float v1,
                            float width, float height, float offsetX, float offsetY,
                            float advanceWidth) {}
}
