package com.vg.core;

import com.vg.core.color.VGColor;
import com.vg.core.font.VGFont;
import com.vg.core.font.VGFontMetrics;

/**
 * VG Debug Overlay System - 可视化调试工具
 * <p>
 * Phase 7: 显示组件边界、文本基线、内边距和间距，一眼看出布局问题
 * <p>
 * 功能：
 * - 显示组件边界框（Bounds）
 * - 显示文本边界和基线（Text Bounds & Baseline）
 * - 显示内边距区域（Padding）
 * - 显示间距指示器（Spacing）
 * - 显示坐标网格（Grid）
 * <p>
 * 使用示例：
 * <pre>
 * VGDebug.enable(true);  // 启用调试覆盖层
 * 
 * // 绘制卡片并显示调试信息
 * VG.rect(x, y, width, height, color);
 * VGDebug.bounds(x, y, width, height, "Card");
 * VGDebug.padding(x, y, width, height, VGPadding.CARD);
 * 
 * // 绘制文本并显示基线
 * VG.text(text, x, y, color);
 * VGDebug.textBounds(text, x, y, font);
 * VGDebug.baseline(x, y, textWidth, font);
 * </pre>
 */
public final class VGDebug {

    private VGDebug() {
        throw new UnsupportedOperationException("VGDebug is a utility class and cannot be instantiated");
    }

    // ══════════════════════════════════════════════
    // 调试状态 (Debug State)
    // ══════════════════════════════════════════════

    /** 全局调试开关 */
    private static boolean debugEnabled = false;

    /** 显示组件边界 */
    private static boolean showBounds = true;

    /** 显示文本边界 */
    private static boolean showTextBounds = true;

    /** 显示基线 */
    private static boolean showBaseline = true;

    /** 显示内边距 */
    private static boolean showPadding = true;

    /** 显示间距 */
    private static boolean showSpacing = true;

    /** 显示坐标网格 */
    private static boolean showGrid = false;

    /** 显示标签 */
    private static boolean showLabels = true;

    // ══════════════════════════════════════════════
    // 调试颜色 (Debug Colors)
    // ══════════════════════════════════════════════

    /** 组件边界颜色 - 蓝色半透明 */
    public static final int COLOR_BOUNDS = VGColor.rgba(0.0f, 0.5f, 1.0f, 0.6f);

    /** 文本边界颜色 - 绿色半透明 */
    public static final int COLOR_TEXT_BOUNDS = VGColor.rgba(0.0f, 1.0f, 0.5f, 0.5f);

    /** 基线颜色 - 红色半透明 */
    public static final int COLOR_BASELINE = VGColor.rgba(1.0f, 0.0f, 0.0f, 0.7f);

    /** 内边距颜色 - 橙色半透明 */
    public static final int COLOR_PADDING = VGColor.rgba(1.0f, 0.6f, 0.0f, 0.3f);

    /** 间距颜色 - 紫色半透明 */
    public static final int COLOR_SPACING = VGColor.rgba(0.8f, 0.0f, 1.0f, 0.5f);

    /** 网格颜色 - 灰色半透明 */
    public static final int COLOR_GRID = VGColor.rgba(0.5f, 0.5f, 0.5f, 0.2f);

    /** 标签背景颜色 - 黑色半透明 */
    public static final int COLOR_LABEL_BG = VGColor.rgba(0.0f, 0.0f, 0.0f, 0.7f);

    /** 标签文字颜色 - 白色 */
    public static final int COLOR_LABEL_TEXT = VGColor.WHITE;

    /** 线条宽度 */
    private static final float LINE_WIDTH = 1.5f;

    /** 虚线间隔 */
    private static final float DASH_LENGTH = 4.0f;

    // ══════════════════════════════════════════════
    // 全局控制 (Global Control)
    // ══════════════════════════════════════════════

    /**
     * 启用或禁用调试覆盖层
     * 
     * @param enabled true = 启用，false = 禁用
     */
    public static void enable(boolean enabled) {
        debugEnabled = enabled;
    }

    /**
     * 检查调试覆盖层是否已启用
     * 
     * @return 是否启用
     */
    public static boolean isEnabled() {
        return debugEnabled;
    }

    /**
     * 配置调试显示选项
     * 
     * @param bounds      显示组件边界
     * @param textBounds  显示文本边界
     * @param baseline    显示基线
     * @param padding     显示内边距
     * @param spacing     显示间距
     * @param grid        显示坐标网格
     * @param labels      显示标签
     */
    public static void configure(boolean bounds, boolean textBounds, boolean baseline,
                                  boolean padding, boolean spacing, boolean grid, boolean labels) {
        showBounds = bounds;
        showTextBounds = textBounds;
        showBaseline = baseline;
        showPadding = padding;
        showSpacing = spacing;
        showGrid = grid;
        showLabels = labels;
    }

    /**
     * 快速配置：显示所有调试信息
     */
    public static void showAll() {
        configure(true, true, true, true, true, false, true);
    }

    /**
     * 快速配置：仅显示边界
     */
    public static void showBoundsOnly() {
        configure(true, false, false, false, false, false, true);
    }

    /**
     * 快速配置：仅显示文本调试信息
     */
    public static void showTextOnly() {
        configure(false, true, true, false, false, false, true);
    }

    // ══════════════════════════════════════════════
    // 组件边界 (Component Bounds)
    // ══════════════════════════════════════════════

    /**
     * 绘制组件边界框
     * 
     * @param x      组件左上角 X
     * @param y      组件左上角 Y
     * @param width  组件宽度
     * @param height 组件高度
     */
    public static void bounds(float x, float y, float width, float height) {
        bounds(x, y, width, height, null);
    }

    /**
     * 绘制组件边界框（带标签）
     * 
     * @param x      组件左上角 X
     * @param y      组件左上角 Y
     * @param width  组件宽度
     * @param height 组件高度
     * @param label  标签文字（可选）
     */
    public static void bounds(float x, float y, float width, float height, String label) {
        if (!debugEnabled || !showBounds) return;

        // 绘制边界矩形（空心）
        drawRectOutline(x, y, width, height, COLOR_BOUNDS, LINE_WIDTH);

        // 绘制角点标记
        drawCornerMarkers(x, y, width, height, COLOR_BOUNDS, 4.0f);

        // 绘制标签
        if (showLabels && label != null && !label.isEmpty()) {
            String text = String.format("%s (%.0f×%.0f)", label, width, height);
            drawLabel(text, x, y - 16, COLOR_LABEL_BG, COLOR_LABEL_TEXT);
        }
    }

    /**
     * 绘制卡片调试信息（包含边界和内边距）
     * 
     * @param x       卡片左上角 X
     * @param y       卡片左上角 Y
     * @param width   卡片宽度
     * @param height  卡片高度
     * @param padding 内边距值
     */
    public static void card(float x, float y, float width, float height, float padding) {
        if (!debugEnabled) return;

        bounds(x, y, width, height, "Card");
        padding(x, y, width, height, padding);
    }

    // ══════════════════════════════════════════════
    // 文本调试 (Text Debug)
    // ══════════════════════════════════════════════

    /**
     * 绘制文本边界框和基线
     * 
     * @param text 文本内容
     * @param x    文本左上角 X
     * @param y    文本左上角 Y
     */
    public static void textBounds(String text, float x, float y) {
        textBounds(text, x, y, null);
    }

    /**
     * 绘制文本边界框和基线（指定字体）
     * 
     * @param text 文本内容
     * @param x    文本左上角 X
     * @param y    文本左上角 Y
     * @param font 字体（null = 使用当前字体）
     */
    public static void textBounds(String text, float x, float y, VGFont font) {
        if (!debugEnabled || (!showTextBounds && !showBaseline)) return;
        if (text == null || text.isEmpty()) return;

        VGFont targetFont = font != null ? font : VG.getInstance().getCurrentFont();
        float textWidth = VG.measureText(text, targetFont);
        float textHeight = VGFontMetrics.textBoxHeight(targetFont);
        float baselineY = y + VGFontMetrics.baselineOffset(targetFont);

        // 绘制文本边界框
        if (showTextBounds) {
            drawRectOutline(x, y, textWidth, textHeight, COLOR_TEXT_BOUNDS, LINE_WIDTH);
        }

        // 绘制基线
        if (showBaseline) {
            baseline(x, baselineY, textWidth);
        }

        // 绘制标签
        if (showLabels && showTextBounds) {
            String label = String.format("Text (%.0f×%.0f)", textWidth, textHeight);
            drawLabel(label, x, y - 14, COLOR_LABEL_BG, COLOR_LABEL_TEXT);
        }
    }

    /**
     * 绘制基线
     * 
     * @param x     起始 X
     * @param y     基线 Y（已经是基线位置，不是文本顶部）
     * @param width 基线宽度
     */
    public static void baseline(float x, float y, float width) {
        if (!debugEnabled || !showBaseline) return;

        // 绘制实线基线
        VG.line(x, y, x + width, y, LINE_WIDTH, COLOR_BASELINE);

        // 绘制基线端点标记
        VG.rect(x - 2, y - 2, 4, 4, COLOR_BASELINE);
        VG.rect(x + width - 2, y - 2, 4, 4, COLOR_BASELINE);
    }

    // ══════════════════════════════════════════════
    // 内边距 (Padding)
    // ══════════════════════════════════════════════

    /**
     * 绘制内边距区域（四周相等）
     * 
     * @param x       容器左上角 X
     * @param y       容器左上角 Y
     * @param width   容器宽度
     * @param height  容器高度
     * @param padding 内边距值
     */
    public static void padding(float x, float y, float width, float height, float padding) {
        padding(x, y, width, height, padding, padding, padding, padding);
    }

    /**
     * 绘制内边距区域（非对称）
     * 
     * @param x      容器左上角 X
     * @param y      容器左上角 Y
     * @param width  容器宽度
     * @param height 容器高度
     * @param top    上内边距
     * @param right  右内边距
     * @param bottom 下内边距
     * @param left   左内边距
     */
    public static void padding(float x, float y, float width, float height,
                               float top, float right, float bottom, float left) {
        if (!debugEnabled || !showPadding) return;

        // 绘制内边距区域（填充半透明）
        // 上
        if (top > 0) {
            VG.rect(x, y, width, top, COLOR_PADDING);
        }
        // 右
        if (right > 0) {
            VG.rect(x + width - right, y, right, height, COLOR_PADDING);
        }
        // 下
        if (bottom > 0) {
            VG.rect(x, y + height - bottom, width, bottom, COLOR_PADDING);
        }
        // 左
        if (left > 0) {
            VG.rect(x, y, left, height, COLOR_PADDING);
        }

        // 绘制内容区域边界（虚线）
        float contentX = x + left;
        float contentY = y + top;
        float contentW = width - left - right;
        float contentH = height - top - bottom;
        drawDashedRect(contentX, contentY, contentW, contentH, COLOR_PADDING, LINE_WIDTH);

        // 绘制标签
        if (showLabels) {
            String label = String.format("Padding: T%.0f R%.0f B%.0f L%.0f", top, right, bottom, left);
            drawLabel(label, x, y + height + 4, COLOR_LABEL_BG, COLOR_LABEL_TEXT);
        }
    }

    // ══════════════════════════════════════════════
    // 间距 (Spacing)
    // ══════════════════════════════════════════════

    /**
     * 绘制垂直间距指示器
     * 
     * @param x       X 位置
     * @param y       起始 Y
     * @param spacing 间距值
     */
    public static void verticalSpacing(float x, float y, float spacing) {
        verticalSpacing(x, y, spacing, null);
    }

    /**
     * 绘制垂直间距指示器（带标签）
     * 
     * @param x       X 位置
     * @param y       起始 Y
     * @param spacing 间距值
     * @param label   标签（可选）
     */
    public static void verticalSpacing(float x, float y, float spacing, String label) {
        if (!debugEnabled || !showSpacing) return;
        if (spacing <= 0) return;

        // 绘制间距区域（填充）
        VG.rect(x - 20, y, 40, spacing, COLOR_SPACING);

        // 绘制箭头指示
        drawVerticalArrow(x, y, spacing, COLOR_SPACING);

        // 绘制标签
        if (showLabels) {
            String text = label != null ? String.format("%s: %.0f", label, spacing) : String.format("%.0f", spacing);
            drawLabel(text, x + 4, y + spacing / 2 - 6, COLOR_LABEL_BG, COLOR_LABEL_TEXT);
        }
    }

    /**
     * 绘制水平间距指示器
     * 
     * @param x       起始 X
     * @param y       Y 位置
     * @param spacing 间距值
     */
    public static void horizontalSpacing(float x, float y, float spacing) {
        horizontalSpacing(x, y, spacing, null);
    }

    /**
     * 绘制水平间距指示器（带标签）
     * 
     * @param x       起始 X
     * @param y       Y 位置
     * @param spacing 间距值
     * @param label   标签（可选）
     */
    public static void horizontalSpacing(float x, float y, float spacing, String label) {
        if (!debugEnabled || !showSpacing) return;
        if (spacing <= 0) return;

        // 绘制间距区域（填充）
        VG.rect(x, y - 20, spacing, 40, COLOR_SPACING);

        // 绘制箭头指示
        drawHorizontalArrow(x, y, spacing, COLOR_SPACING);

        // 绘制标签
        if (showLabels) {
            String text = label != null ? String.format("%s: %.0f", label, spacing) : String.format("%.0f", spacing);
            drawLabel(text, x + spacing / 2 - 20, y - 26, COLOR_LABEL_BG, COLOR_LABEL_TEXT);
        }
    }

    // ══════════════════════════════════════════════
    // 坐标网格 (Grid)
    // ══════════════════════════════════════════════

    /**
     * 绘制坐标网格（用于对齐检查）
     * 
     * @param gridSize 网格大小（像素）
     */
    public static void grid(float gridSize) {
        if (!debugEnabled || !showGrid) return;

        int screenWidth = VG.width();
        int screenHeight = VG.height();

        // 绘制垂直线
        for (float x = 0; x < screenWidth; x += gridSize) {
            VG.line(x, 0, x, screenHeight, 1.0f, COLOR_GRID);
        }

        // 绘制水平线
        for (float y = 0; y < screenHeight; y += gridSize) {
            VG.line(0, y, screenWidth, y, 1.0f, COLOR_GRID);
        }
    }

    /**
     * 绘制坐标轴（原点标记）
     * 
     * @param originX 原点 X
     * @param originY 原点 Y
     * @param size    坐标轴长度
     */
    public static void origin(float originX, float originY, float size) {
        if (!debugEnabled) return;

        // X 轴（红色）
        VG.line(originX, originY, originX + size, originY, 2.0f, VGColor.RED);
        // Y 轴（绿色）
        VG.line(originX, originY, originX, originY + size, 2.0f, VGColor.GREEN);

        // 原点标记
        VG.rect(originX - 3, originY - 3, 6, 6, VGColor.YELLOW);

        if (showLabels) {
            drawLabel(String.format("(%.0f, %.0f)", originX, originY), originX + 8, originY - 16,
                    COLOR_LABEL_BG, COLOR_LABEL_TEXT);
        }
    }

    // ══════════════════════════════════════════════
    // 辅助绘制方法 (Helper Drawing Methods)
    // ══════════════════════════════════════════════

    /**
     * 绘制空心矩形（边框）
     */
    private static void drawRectOutline(float x, float y, float width, float height, int color, float lineWidth) {
        // 上
        VG.line(x, y, x + width, y, lineWidth, color);
        // 右
        VG.line(x + width, y, x + width, y + height, lineWidth, color);
        // 下
        VG.line(x + width, y + height, x, y + height, lineWidth, color);
        // 左
        VG.line(x, y + height, x, y, lineWidth, color);
    }

    /**
     * 绘制虚线矩形
     */
    private static void drawDashedRect(float x, float y, float width, float height, int color, float lineWidth) {
        // 上
        drawDashedLine(x, y, x + width, y, lineWidth, color);
        // 右
        drawDashedLine(x + width, y, x + width, y + height, lineWidth, color);
        // 下
        drawDashedLine(x + width, y + height, x, y + height, lineWidth, color);
        // 左
        drawDashedLine(x, y + height, x, y, lineWidth, color);
    }

    /**
     * 绘制虚线
     */
    private static void drawDashedLine(float x1, float y1, float x2, float y2, float lineWidth, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float dirX = dx / length;
        float dirY = dy / length;

        float currentLength = 0;
        boolean draw = true;

        while (currentLength < length) {
            float segmentLength = Math.min(DASH_LENGTH, length - currentLength);
            if (draw) {
                float startX = x1 + dirX * currentLength;
                float startY = y1 + dirY * currentLength;
                float endX = x1 + dirX * (currentLength + segmentLength);
                float endY = y1 + dirY * (currentLength + segmentLength);
                VG.line(startX, startY, endX, endY, lineWidth, color);
            }
            currentLength += segmentLength;
            draw = !draw;
        }
    }

    /**
     * 绘制角点标记（用于精确定位）
     */
    private static void drawCornerMarkers(float x, float y, float width, float height, int color, float size) {
        // 左上
        VG.line(x, y, x + size, y, LINE_WIDTH, color);
        VG.line(x, y, x, y + size, LINE_WIDTH, color);
        // 右上
        VG.line(x + width, y, x + width - size, y, LINE_WIDTH, color);
        VG.line(x + width, y, x + width, y + size, LINE_WIDTH, color);
        // 右下
        VG.line(x + width, y + height, x + width - size, y + height, LINE_WIDTH, color);
        VG.line(x + width, y + height, x + width, y + height - size, LINE_WIDTH, color);
        // 左下
        VG.line(x, y + height, x + size, y + height, LINE_WIDTH, color);
        VG.line(x, y + height, x, y + height - size, LINE_WIDTH, color);
    }

    /**
     * 绘制垂直箭头
     */
    private static void drawVerticalArrow(float x, float y, float length, int color) {
        // 中心线
        VG.line(x, y, x, y + length, LINE_WIDTH, color);
        // 上箭头
        VG.line(x, y, x - 3, y + 5, LINE_WIDTH, color);
        VG.line(x, y, x + 3, y + 5, LINE_WIDTH, color);
        // 下箭头
        VG.line(x, y + length, x - 3, y + length - 5, LINE_WIDTH, color);
        VG.line(x, y + length, x + 3, y + length - 5, LINE_WIDTH, color);
    }

    /**
     * 绘制水平箭头
     */
    private static void drawHorizontalArrow(float x, float y, float length, int color) {
        // 中心线
        VG.line(x, y, x + length, y, LINE_WIDTH, color);
        // 左箭头
        VG.line(x, y, x + 5, y - 3, LINE_WIDTH, color);
        VG.line(x, y, x + 5, y + 3, LINE_WIDTH, color);
        // 右箭头
        VG.line(x + length, y, x + length - 5, y - 3, LINE_WIDTH, color);
        VG.line(x + length, y, x + length - 5, y + 3, LINE_WIDTH, color);
    }

    /**
     * 绘制标签（带背景）
     */
    private static void drawLabel(String text, float x, float y, int bgColor, int textColor) {
        if (text == null || text.isEmpty()) return;

        float textWidth = VG.measureText(text);
        float textHeight = 12; // 固定高度，避免循环依赖
        float paddingH = 4;
        float paddingV = 2;

        // 绘制背景
        VG.rect(x, y, textWidth + paddingH * 2, textHeight + paddingV * 2, bgColor);

        // 绘制文字（需要小字体，这里假设已经设置了合适的字体）
        VG.text(text, x + paddingH, y + paddingV, textColor);
    }
}
