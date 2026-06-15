package com.vg.core;

import com.vg.core.font.VGFont;
import com.vg.core.font.VGFontMetrics;
import com.vg.core.icon.Icons;
import com.vg.core.text.VGTextLayout;

public record VGMeasure(float width, float height) {

    public static final VGMeasure EMPTY = new VGMeasure(0f, 0f);

    public VGMeasure {
        width = Math.max(0f, width);
        height = Math.max(0f, height);
    }

    public boolean isEmpty() {
        return width <= 0f && height <= 0f;
    }

    public VGMeasure withPadding(float horizontal, float vertical) {
        return new VGMeasure(
                width + Math.max(0f, horizontal) * 2f,
                height + Math.max(0f, vertical) * 2f
        );
    }

    public static VGMeasure measureText(String text) {
        return measureText(text, null);
    }

    public static VGMeasure measureText(String text, VGFont font) {
        String safeText = text != null ? text : "";
        if (safeText.isEmpty()) {
            return EMPTY;
        }

        float measuredWidth = font != null ? VG.measureText(safeText, font) : VG.measureText(safeText);
        return new VGMeasure(measuredWidth, VGFontMetrics.textBoxHeight(font));
    }

    public static VGMeasure measureMultiline(String text, float maxWidth) {
        return measureMultiline(text, maxWidth, null);
    }

    public static VGMeasure measureMultiline(String text, float maxWidth, VGFont font) {
        return measureMultiline(text, maxWidth, font, VGFontMetrics.lineHeight(font), VGTextLayout.TextAlign.LEFT);
    }

    public static VGMeasure measureMultiline(String text, float maxWidth, VGFont font, float lineHeight) {
        return measureMultiline(text, maxWidth, font, lineHeight, VGTextLayout.TextAlign.LEFT);
    }

    public static VGMeasure measureMultiline(String text, float maxWidth, VGFont font, float lineHeight, VGTextLayout.TextAlign align) {
        String safeText = text != null ? text : "";
        if (safeText.isEmpty() || maxWidth <= 0f) {
            return EMPTY;
        }

        float resolvedLineHeight = lineHeight > 0f ? lineHeight : VGFontMetrics.lineHeight(font);
        VGTextLayout.TextLayoutResult result = VGTextLayout.wrap(safeText, maxWidth, font, resolvedLineHeight, align);
        return new VGMeasure(result.width(), result.height());
    }

    public static VGMeasure measureIcon(Icons icon) {
        if (icon == null) {
            return EMPTY;
        }
        return measureIcon(icon, VG.getIconFont().getFontSize());
    }

    public static VGMeasure measureIcon(Icons icon, float size) {
        if (icon == null || size <= 0f) {
            return EMPTY;
        }
        return new VGMeasure(size, size);
    }

    public static VGMeasure measureContent(VGMeasure... items) {
        return measureContent(0f, items);
    }

    public static VGMeasure measureContent(float gap, VGMeasure... items) {
        if (items == null || items.length == 0) {
            return EMPTY;
        }

        float totalWidth = 0f;
        float maxHeight = 0f;
        int visibleCount = 0;

        for (VGMeasure item : items) {
            if (item == null || item.isEmpty()) {
                continue;
            }
            totalWidth += item.width();
            maxHeight = Math.max(maxHeight, item.height());
            visibleCount++;
        }

        if (visibleCount > 1) {
            totalWidth += Math.max(0f, gap) * (visibleCount - 1);
        }

        return new VGMeasure(totalWidth, maxHeight);
    }

    public static VGMeasure measureContent(String text, VGFont font, Icons icon, float iconSize, float gap) {
        return measureContent(gap, measureIcon(icon, iconSize), measureText(text, font));
    }

    // ══════════════════════════════════════════════
    // Card & Container Measurement (Phase 6)
    // ══════════════════════════════════════════════

    /**
     * 计算卡片总尺寸（内容 + padding）
     * 
     * @param contentWidth  内容宽度
     * @param contentHeight 内容高度
     * @param padding       内边距（四周相等）
     * @return 卡片总尺寸
     */
    public static VGMeasure card(float contentWidth, float contentHeight, float padding) {
        return new VGMeasure(
            contentWidth + padding * 2f,
            contentHeight + padding * 2f
        );
    }

    /**
     * 计算卡片总尺寸（内容 + 非对称 padding）
     * 
     * @param contentWidth       内容宽度
     * @param contentHeight      内容高度
     * @param horizontalPadding  水平内边距（左右各一份）
     * @param verticalPadding    垂直内边距（上下各一份）
     * @return 卡片总尺寸
     */
    public static VGMeasure card(float contentWidth, float contentHeight, 
                                 float horizontalPadding, float verticalPadding) {
        return new VGMeasure(
            contentWidth + horizontalPadding * 2f,
            contentHeight + verticalPadding * 2f
        );
    }

    /**
     * 计算卡片总尺寸（从已测量的内容 + padding）
     * 
     * @param content 已测量的内容尺寸
     * @param padding 内边距
     * @return 卡片总尺寸
     */
    public static VGMeasure card(VGMeasure content, float padding) {
        if (content == null || content.isEmpty()) {
            return new VGMeasure(padding * 2f, padding * 2f);
        }
        return card(content.width(), content.height(), padding);
    }

    /**
     * 计算垂直布局的卡片高度（多个内容项 + padding + spacing）
     * 
     * @param padding     卡片内边距
     * @param itemSpacing 内容项之间的间距
     * @param items       内容项尺寸列表
     * @return 卡片总尺寸（宽度取最大项宽度）
     */
    public static VGMeasure cardVertical(float padding, float itemSpacing, VGMeasure... items) {
        if (items == null || items.length == 0) {
            return new VGMeasure(padding * 2f, padding * 2f);
        }

        float maxWidth = 0f;
        float totalHeight = 0f;
        int visibleCount = 0;

        for (VGMeasure item : items) {
            if (item == null || item.isEmpty()) {
                continue;
            }
            maxWidth = Math.max(maxWidth, item.width());
            totalHeight += item.height();
            visibleCount++;
        }

        // 添加项之间的间距
        if (visibleCount > 1) {
            totalHeight += itemSpacing * (visibleCount - 1);
        }

        return new VGMeasure(
            maxWidth + padding * 2f,
            totalHeight + padding * 2f
        );
    }

    /**
     * 计算水平布局的卡片宽度（多个内容项 + padding + spacing）
     * 
     * @param padding     卡片内边距
     * @param itemSpacing 内容项之间的间距
     * @param items       内容项尺寸列表
     * @return 卡片总尺寸（高度取最大项高度）
     */
    public static VGMeasure cardHorizontal(float padding, float itemSpacing, VGMeasure... items) {
        if (items == null || items.length == 0) {
            return new VGMeasure(padding * 2f, padding * 2f);
        }

        float totalWidth = 0f;
        float maxHeight = 0f;
        int visibleCount = 0;

        for (VGMeasure item : items) {
            if (item == null || item.isEmpty()) {
                continue;
            }
            totalWidth += item.width();
            maxHeight = Math.max(maxHeight, item.height());
            visibleCount++;
        }

        // 添加项之间的间距
        if (visibleCount > 1) {
            totalWidth += itemSpacing * (visibleCount - 1);
        }

        return new VGMeasure(
            totalWidth + padding * 2f,
            maxHeight + padding * 2f
        );
    }

    /**
     * 计算网格布局的卡片尺寸
     * 
     * @param padding    卡片内边距
     * @param itemWidth  单个网格项宽度
     * @param itemHeight 单个网格项高度
     * @param columns    列数
     * @param rows       行数
     * @param gapX       列间距
     * @param gapY       行间距
     * @return 卡片总尺寸
     */
    public static VGMeasure cardGrid(float padding, float itemWidth, float itemHeight,
                                     int columns, int rows, float gapX, float gapY) {
        if (columns <= 0 || rows <= 0) {
            return new VGMeasure(padding * 2f, padding * 2f);
        }

        float gridWidth = itemWidth * columns + gapX * Math.max(0, columns - 1);
        float gridHeight = itemHeight * rows + gapY * Math.max(0, rows - 1);

        return new VGMeasure(
            gridWidth + padding * 2f,
            gridHeight + padding * 2f
        );
    }

    /**
     * 计算容器内容区域尺寸（总尺寸 - padding）
     * 
     * @param totalWidth  容器总宽度
     * @param totalHeight 容器总高度
     * @param padding     内边距
     * @return 内容区域尺寸
     */
    public static VGMeasure contentArea(float totalWidth, float totalHeight, float padding) {
        return new VGMeasure(
            Math.max(0f, totalWidth - padding * 2f),
            Math.max(0f, totalHeight - padding * 2f)
        );
    }
}
