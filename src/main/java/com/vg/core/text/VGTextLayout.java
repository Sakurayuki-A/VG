package com.vg.core.text;

import com.vg.core.VGMeasure;
import com.vg.core.font.VGFont;
import com.vg.core.font.VGFontMetrics;

import java.util.ArrayList;
import java.util.List;

public final class VGTextLayout {

    public enum TextAlign {
        LEFT, CENTER, RIGHT
    }

    public record TextPosition(float x, float y) {}

    public record TextLine(
            String text,
            float width,
            float xOffset
    ) {}

    public record TextLayoutResult(
            List<TextLine> lines,
            float width,
            float height
    ) {}

    private VGTextLayout() {}

    public static TextPosition align(TextAlign align, String text, float x, float y, float width, float height) {
        return align(align, text, x, y, width, height, null);
    }

    public static TextPosition align(TextAlign align, String text, float x, float y, float width, float height, VGFont font) {
        return switch (align) {
            case CENTER -> center(text, x, y, width, height, font);
            case LEFT -> left(text, x, y, height, font);
            case RIGHT -> right(text, x, y, width, height, font);
        };
    }

    public static TextPosition center(String text, float x, float y, float width, float height) {
        return center(text, x, y, width, height, null);
    }

    public static TextPosition center(String text, float x, float y, float width, float height, VGFont font) {
        float textW = measureW(text, font);
        float px = x + (width - textW) / 2f;
        float py = VGFontMetrics.centerY(y, height, font);
        return new TextPosition(px, py);
    }

    public static TextPosition left(String text, float x, float y, float height) {
        return left(text, x, y, height, null);
    }

    public static TextPosition left(String text, float x, float y, float height, VGFont font) {
        float px = x;
        float py = VGFontMetrics.centerY(y, height, font);
        return new TextPosition(px, py);
    }

    public static TextPosition right(String text, float x, float y, float width, float height) {
        return right(text, x, y, width, height, null);
    }

    public static TextPosition right(String text, float x, float y, float width, float height, VGFont font) {
        float textW = measureW(text, font);
        float px = x + width - textW;
        float py = VGFontMetrics.centerY(y, height, font);
        return new TextPosition(px, py);
    }

    private static float measureW(String text, VGFont font) {
        return VGMeasure.measureText(text, font).width();
    }

    public static TextLayoutResult wrap(String text, float maxWidth, VGFont font) {
        return wrap(text, maxWidth, font, TextAlign.LEFT);
    }

    public static TextLayoutResult wrap(String text, float maxWidth, VGFont font, float lineHeight) {
        return wrap(text, maxWidth, font, lineHeight, TextAlign.LEFT);
    }

    public static TextLayoutResult wrap(String text, float maxWidth, VGFont font, TextAlign align) {
        float defaultLineHeight = VGFontMetrics.lineHeight(font);
        return wrap(text, maxWidth, font, defaultLineHeight, align);
    }

    public static TextLayoutResult wrap(String text, float maxWidth, VGFont font, float lineHeight, TextAlign align) {
        List<String> rawLines = new ArrayList<>();

        StringBuilder line = new StringBuilder();

        for (char c : text.toCharArray()) {
            String test = line.toString() + c;

            if (VGMeasure.measureText(test, font).width() > maxWidth) {
                rawLines.add(line.toString());
                line.setLength(0);
                line.append(c);
            } else {
                line.append(c);
            }
        }

        if (!line.isEmpty()) {
            rawLines.add(line.toString());
        }

        List<TextLine> lines = new ArrayList<>();
        float maxW = 0;

        for (String rawLine : rawLines) {
            float lineWidth = measureW(rawLine, font);
            if (lineWidth > maxW) maxW = lineWidth;

            float xOffset = switch (align) {
                case LEFT -> 0f;
                case CENTER -> (maxWidth - lineWidth) * 0.5f;
                case RIGHT -> maxWidth - lineWidth;
            };

            lines.add(new TextLine(rawLine, lineWidth, xOffset));
        }

        float totalH = lines.size() * lineHeight;

        return new TextLayoutResult(lines, maxW, totalH);
    }

    public static String ellipsis(String text, float maxWidth, VGFont font) {
        if (measureW(text, font) <= maxWidth) {
            return text;
        }

        String result = text;
        while (result.length() > 0 && measureW(result + "...", font) > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }

        return result + "...";
    }
}
