package com.vg.core.component;

import com.vg.core.Anchor;
import com.vg.core.AnimatedState;
import com.vg.core.VG;
import com.vg.core.VGMeasure;
import com.vg.core.VGPadding;
import com.vg.core.color.VGColor;
import com.vg.core.input.MouseContext;
import com.vg.core.node.VGNode;

public class VGCard extends VGNode {

    private float radius = 0;
    private int backgroundColor = VGColor.WHITE;

    private float paddingLeft = 0;
    private float paddingRight = 0;
    private float paddingTop = 0;
    private float paddingBottom = 0;

    private boolean hasShadow = false;
    private float shadowSpread = 0;
    private float shadowBlur = 0;
    private int shadowColor = VGColor.rgba(0, 0, 0, 0.3f);

    private boolean hasGradient = false;
    private int gradientTL, gradientTR, gradientBL, gradientBR;

    private VGCard anchorTarget = null;
    private Anchor anchorPoint = null;

    private final AnimatedState hoverState = new AnimatedState(65f, 20f);
    private final AnimatedState pressState = new AnimatedState(90f, 26f);

    public VGCard(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // ══════════════════════════════════════════════
    // 静态工厂方法 (Static Factory Methods)
    // ══════════════════════════════════════════════

    /**
     * 创建带标准 Card padding 的卡片 (16px)
     */
    public static VGCard withStandardPadding(float x, float y, float width, float height) {
        return new VGCard(x, y, width, height).setPadding(VGPadding.CARD);
    }

    /**
     * 创建带小 padding 的卡片 (12px)
     */
    public static VGCard withSmallPadding(float x, float y, float width, float height) {
        return new VGCard(x, y, width, height).setPadding(VGPadding.CARD_SMALL);
    }

    /**
     * 创建带大 padding 的卡片 (24px)
     */
    public static VGCard withLargePadding(float x, float y, float width, float height) {
        return new VGCard(x, y, width, height).setPadding(VGPadding.CARD_LARGE);
    }

    // ══════════════════════════════════════════════
    // Auto Size Methods (Phase 6)
    // ══════════════════════════════════════════════

    /**
     * 根据内容尺寸自动调整卡片大小
     * 
     * @param contentWidth  内容宽度
     * @param contentHeight 内容高度
     * @return this（链式调用）
     */
    public VGCard autoSize(float contentWidth, float contentHeight) {
        this.width = contentWidth + paddingLeft + paddingRight;
        this.height = contentHeight + paddingTop + paddingBottom;
        return this;
    }

    /**
     * 根据已测量的内容自动调整卡片大小
     * 
     * @param contentMeasure 已测量的内容尺寸
     * @return this（链式调用）
     */
    public VGCard autoSize(VGMeasure contentMeasure) {
        if (contentMeasure == null || contentMeasure.isEmpty()) {
            this.width = paddingLeft + paddingRight;
            this.height = paddingTop + paddingBottom;
        } else {
            autoSize(contentMeasure.width(), contentMeasure.height());
        }
        return this;
    }

    /**
     * 根据内容宽度自动调整卡片宽度（保持高度不变）
     * 
     * @param contentWidth 内容宽度
     * @return this（链式调用）
     */
    public VGCard autoWidth(float contentWidth) {
        this.width = contentWidth + paddingLeft + paddingRight;
        return this;
    }

    /**
     * 根据内容高度自动调整卡片高度（保持宽度不变）
     * 
     * @param contentHeight 内容高度
     * @return this（链式调用）
     */
    public VGCard autoHeight(float contentHeight) {
        this.height = contentHeight + paddingTop + paddingBottom;
        return this;
    }

    /**
     * 根据多个垂直排列的内容项自动调整高度
     * 
     * @param itemSpacing 内容项之间的间距
     * @param items       内容项高度列表
     * @return this（链式调用）
     */
    public VGCard autoHeightVertical(float itemSpacing, float... items) {
        if (items == null || items.length == 0) {
            this.height = paddingTop + paddingBottom;
            return this;
        }

        float totalHeight = 0f;
        int validCount = 0;

        for (float item : items) {
            if (item > 0f) {
                totalHeight += item;
                validCount++;
            }
        }

        if (validCount > 1) {
            totalHeight += itemSpacing * (validCount - 1);
        }

        this.height = totalHeight + paddingTop + paddingBottom;
        return this;
    }

    /**
     * 根据多个水平排列的内容项自动调整宽度
     * 
     * @param itemSpacing 内容项之间的间距
     * @param items       内容项宽度列表
     * @return this（链式调用）
     */
    public VGCard autoWidthHorizontal(float itemSpacing, float... items) {
        if (items == null || items.length == 0) {
            this.width = paddingLeft + paddingRight;
            return this;
        }

        float totalWidth = 0f;
        int validCount = 0;

        for (float item : items) {
            if (item > 0f) {
                totalWidth += item;
                validCount++;
            }
        }

        if (validCount > 1) {
            totalWidth += itemSpacing * (validCount - 1);
        }

        this.width = totalWidth + paddingLeft + paddingRight;
        return this;
    }

    /**
     * 根据网格布局自动调整尺寸
     * 
     * @param itemWidth  单个网格项宽度
     * @param itemHeight 单个网格项高度
     * @param columns    列数
     * @param rows       行数
     * @param gapX       列间距
     * @param gapY       行间距
     * @return this（链式调用）
     */
    public VGCard autoSizeGrid(float itemWidth, float itemHeight,
                               int columns, int rows, float gapX, float gapY) {
        if (columns <= 0 || rows <= 0) {
            this.width = paddingLeft + paddingRight;
            this.height = paddingTop + paddingBottom;
            return this;
        }

        float gridWidth = itemWidth * columns + gapX * Math.max(0, columns - 1);
        float gridHeight = itemHeight * rows + gapY * Math.max(0, rows - 1);

        this.width = gridWidth + paddingLeft + paddingRight;
        this.height = gridHeight + paddingTop + paddingBottom;
        return this;
    }

    /**
     * 固定宽度，根据内容自动计算最小高度
     * 
     * @param fixedWidth 固定的卡片宽度
     * @param minHeight  最小高度（可选，传入 0 表示无限制）
     * @return this（链式调用）
     */
    public VGCard fitHeight(float fixedWidth, float minHeight) {
        this.width = fixedWidth;
        if (minHeight > 0f) {
            this.height = Math.max(minHeight, paddingTop + paddingBottom);
        }
        return this;
    }

    /**
     * 固定高度，根据内容自动计算最小宽度
     * 
     * @param fixedHeight 固定的卡片高度
     * @param minWidth    最小宽度（可选，传入 0 表示无限制）
     * @return this（链式调用）
     */
    public VGCard fitWidth(float fixedHeight, float minWidth) {
        this.height = fixedHeight;
        if (minWidth > 0f) {
            this.width = Math.max(minWidth, paddingLeft + paddingRight);
        }
        return this;
    }

    @Override
    public void update(float dt, MouseContext mouse) {
        updateAnimation(mouse, dt);
    }

    @Override
    public void render() {
        if (anchorTarget != null && anchorPoint != null) {
            resolveAnchor();
        }
        if (hasShadow) {
            VG.shadow(x, y, width, height, shadowSpread, shadowBlur, shadowColor);
        }

        if (hasGradient && radius > 0) {
            VG.gradientRoundRect(x, y, width, height, radius, gradientTL, gradientTR, gradientBL, gradientBR);
        } else if (hasGradient) {
            VG.gradientRect(x, y, width, height, gradientTL, gradientTR, gradientBL, gradientBR);
        } else if (radius > 0) {
            VG.roundRect(x, y, width, height, radius, backgroundColor);
        } else {
            VG.rect(x, y, width, height, backgroundColor);
        }
    }

    public VGCard setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public VGCard setSize(float width, float height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public boolean contains(float px, float py) {
        return px >= x && px <= x + width
            && py >= y && py <= y + height;
    }

    public boolean isHovered(MouseContext mouse) {
        return contains(mouse.x(), mouse.y());
    }

    public void updateAnimation(MouseContext mouse, float dt) {
        boolean hovered = contains(mouse.x(), mouse.y());
        boolean pressed = hovered && mouse.leftDown();

        hoverState.setTarget(hovered);
        pressState.setTarget(pressed);

        hoverState.update(dt);
        pressState.update(dt);
    }

    public float hoverProgress() {
        return hoverState.get();
    }

    public float pressProgress() {
        return pressState.get();
    }

    public VGCard setRadius(float radius) {
        this.radius = radius;
        return this;
    }

    public float getRadius() {
        return radius;
    }

    public VGCard setBackgroundColor(int color) {
        this.backgroundColor = color;
        return this;
    }

    public int backgroundColor() {
        return backgroundColor;
    }

    public VGCard setPadding(float padding) {
        this.paddingLeft = padding;
        this.paddingRight = padding;
        this.paddingTop = padding;
        this.paddingBottom = padding;
        return this;
    }

    public VGCard setPadding(float horizontal, float vertical) {
        this.paddingLeft = horizontal;
        this.paddingRight = horizontal;
        this.paddingTop = vertical;
        this.paddingBottom = vertical;
        return this;
    }

    public VGCard setPadding(float top, float right, float bottom, float left) {
        this.paddingTop = top;
        this.paddingRight = right;
        this.paddingBottom = bottom;
        this.paddingLeft = left;
        return this;
    }

    public float contentX() {
        return x + paddingLeft;
    }

    public float contentY() {
        return y + paddingTop;
    }

    public float contentWidth() {
        return width - paddingLeft - paddingRight;
    }

    public float contentHeight() {
        return height - paddingTop - paddingBottom;
    }

    public VGCard setShadow(float spread, float blur, int color) {
        this.hasShadow = true;
        this.shadowSpread = spread;
        this.shadowBlur = blur;
        this.shadowColor = color;
        return this;
    }

    public VGCard setGradient(int topLeft, int topRight, int bottomLeft, int bottomRight) {
        this.hasGradient = true;
        this.gradientTL = topLeft;
        this.gradientTR = topRight;
        this.gradientBL = bottomLeft;
        this.gradientBR = bottomRight;
        return this;
    }

    public VGCard anchorTo(VGCard target, Anchor anchor) {
        if (target == null) throw new IllegalArgumentException("anchor target cannot be null");
        if (anchor == null) throw new IllegalArgumentException("anchor cannot be null");
        this.anchorTarget = target;
        this.anchorPoint = anchor;
        return this;
    }

    private void resolveAnchor() {
        float tx = anchorTarget.x();
        float ty = anchorTarget.y();
        float tw = anchorTarget.width();
        float th = anchorTarget.height();

        switch (anchorPoint) {
            case TOP_LEFT -> { x = tx; y = ty; }
            case TOP_CENTER -> { x = tx + tw * 0.5f - width * 0.5f; y = ty; }
            case TOP_RIGHT -> { x = tx + tw - width; y = ty; }
            case CENTER_LEFT -> { x = tx; y = ty + th * 0.5f - height * 0.5f; }
            case CENTER -> { x = tx + tw * 0.5f - width * 0.5f; y = ty + th * 0.5f - height * 0.5f; }
            case CENTER_RIGHT -> { x = tx + tw - width; y = ty + th * 0.5f - height * 0.5f; }
            case BOTTOM_LEFT -> { x = tx; y = ty + th - height; }
            case BOTTOM_CENTER -> { x = tx + tw * 0.5f - width * 0.5f; y = ty + th - height; }
            case BOTTOM_RIGHT -> { x = tx + tw - width; y = ty + th - height; }
        }
    }
}