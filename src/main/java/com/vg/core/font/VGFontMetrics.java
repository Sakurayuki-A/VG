package com.vg.core.font;

import com.vg.core.VG;
import com.vg.core.icon.VGIconFont;

public final class VGFontMetrics {

    private VGFontMetrics() {}

    public static float ascent(VGFont font) {
        return font != null ? font.getAscent() : VG.getInstance().getCurrentFont().getAscent();
    }

    public static float descent(VGFont font) {
        return font != null ? font.getDescent() : VG.getInstance().getCurrentFont().getDescent();
    }

    public static float lineHeight(VGFont font) {
        return font != null ? font.getLineHeight() : VG.getInstance().getCurrentFont().getLineHeight();
    }

    public static float baselineOffset(VGFont font) {
        return font != null ? font.getBaselineOffset() : VG.getInstance().getCurrentFont().getBaselineOffset();
    }

    public static float textBoxHeight(VGFont font) {
        return baselineOffset(font) + descent(font);
    }

    public static float centerY(float y, float height, VGFont font) {
        return y + (height - textBoxHeight(font)) / 2f;
    }

    public static float centerBaseline(float y, float height, VGFont font) {
        return centerY(y, height, font);
    }

    public static float topToBaseline(float topY, VGFont font) {
        return topY + baselineOffset(font);
    }

    public static float baselineToTop(float baselineY, VGFont font) {
        return baselineY - baselineOffset(font);
    }

    public static float centerY(float y, float height, VGIconFont iconFont, float iconSize) {
        return y + (height - iconSize) / 2f;
    }

    public static float centerBaseline(float y, float height, VGIconFont iconFont, float iconSize) {
        return centerY(y, height, iconFont, iconSize);
    }
}
