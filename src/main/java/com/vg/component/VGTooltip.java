package com.vg.component;

import com.vg.core.VG;
import com.vg.core.VGMeasure;
import com.vg.core.color.VGColor;
import com.vg.core.component.VGCard;
import com.vg.core.font.VGFont;
import com.vg.core.font.VGFontMetrics;
import com.vg.core.input.MouseContext;
import com.vg.core.node.VGNode;
import com.vg.core.text.VGTextLayout;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class VGTooltip extends VGNode {

    public enum Direction {
        TOP, BOTTOM, LEFT, RIGHT
    }

    @FunctionalInterface
    public interface BoundsProvider {
        float[] get();
    }

    @FunctionalInterface
    public interface ToFloatFunc<T> {
        float apply(T t);
    }

    public static final class PositionResult {
        public final float x, y;
        public final Direction direction;

        public PositionResult(float x, float y, Direction direction) {
            this.x = x;
            this.y = y;
            this.direction = direction;
        }
    }

    private String staticText = "";
    private Supplier<String> dynamicText = null;
    private int textColor = VGColor.rgba(0xf0, 0xf0, 0xf0, 255);
    private int backgroundColor = VGColor.rgba(40, 44, 52, 230);
    private int fontSize = 14;
    private float paddingX = 12f;
    private float paddingY = 8f;
    private float alpha = 1f;
    private float maxWidth = 0f;

    private VGFont font;

    // ── 策略字段 ──

    private BoundsProvider boundsProvider;
    private BooleanSupplier showCondition;
    private Direction preferredDirection = Direction.TOP;

    // ── Text ──

    public VGTooltip setText(String text) {
        this.staticText = text != null ? text : "";
        this.dynamicText = null;
        return this;
    }

    public VGTooltip setText(Supplier<String> textSupplier) {
        this.dynamicText = textSupplier;
        this.staticText = "";
        return this;
    }

    public String getText() {
        if (dynamicText != null) {
            String result = dynamicText.get();
            return result != null ? result : "";
        }
        return staticText;
    }

    // ── Style ──

    public VGTooltip setTextColor(int color) {
        this.textColor = color;
        return this;
    }

    public VGTooltip setBackgroundColor(int color) {
        this.backgroundColor = color;
        return this;
    }

    public VGTooltip setFontSize(int size) {
        this.fontSize = size;
        return this;
    }

    public int getFontSize() {
        return fontSize;
    }

    public VGTooltip setFont(VGFont font) {
        this.font = font;
        return this;
    }

    public VGTooltip setPadding(float x, float y) {
        this.paddingX = x;
        this.paddingY = y;
        return this;
    }

    public float getPaddingX() {
        return paddingX;
    }

    public float getPaddingY() {
        return paddingY;
    }

    public VGTooltip setMaxWidth(float maxWidth) {
        this.maxWidth = maxWidth;
        return this;
    }

    public float getMaxWidth() {
        return maxWidth;
    }

    // ── Geometry ──

    public VGTooltip setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public float getX() { return x; }

    public float getY() { return y; }

    public VGTooltip setSize(float w, float h) {
        this.width = w;
        this.height = h;
        return this;
    }

    public float getWidth() { return width; }

    public float getHeight() { return height; }

    // ── Alpha ──

    public VGTooltip setAlpha(float alpha) {
        this.alpha = Math.max(0f, Math.min(1f, alpha));
        return this;
    }

    public float getAlpha() { return alpha; }

    // ── 策略配置（链式） ──

    public <T> VGTooltip attach(T trigger,
                                ToFloatFunc<T> getX,
                                ToFloatFunc<T> getY,
                                ToFloatFunc<T> getW,
                                ToFloatFunc<T> getH) {
        this.boundsProvider = () -> new float[]{
                getX.apply(trigger),
                getY.apply(trigger),
                getW.apply(trigger),
                getH.apply(trigger)
        };
        return this;
    }

    public VGTooltip direction(Direction dir) {
        this.preferredDirection = dir;
        return this;
    }

    public VGTooltip showWhen(BooleanSupplier condition) {
        this.showCondition = condition;
        return this;
    }

    // ── 自动更新 ──

    @Override
    public void update(float dt, MouseContext mouse) {
        // VGTooltip 的更新需要屏幕尺寸，通过 update(dt, screenW, screenH) 调用
    }

    public void update(float dt, int screenW, int screenH) {
        if (showCondition == null || boundsProvider == null) {
            setAlpha(0f);
            return;
        }

        if (!showCondition.getAsBoolean()) {
            setAlpha(0f);
            return;
        }

        String text = getText();
        VGMeasure measure;

        if (maxWidth > 0) {
            measure = VGMeasure.measureMultiline(text, maxWidth - paddingX * 2, font)
                    .withPadding(paddingX, paddingY);
        } else {
            measure = VGMeasure.measureText(text, font)
                    .withPadding(paddingX, paddingY);
        }

        float w = maxWidth > 0 ? Math.min(measure.width(), maxWidth) : measure.width();
        float h = measure.height();

        float[] bounds = boundsProvider.get();
        PositionResult pos = calculatePosition(w, h,
                bounds[0], bounds[1], bounds[2], bounds[3],
                preferredDirection, screenW, screenH);

        setPosition(pos.x, pos.y);
        setSize(w, h);
        setAlpha(1f);
    }

    // ── Interaction ──

    @Override
    public boolean contains(float px, float py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }

    // ── Render ──

    @Override
    public void render() {
        if (!visible || alpha < 0.005f) return;

        int bgWithAlpha = VGColor.rgba(
                VGColor.r(backgroundColor),
                VGColor.g(backgroundColor),
                VGColor.b(backgroundColor),
                VGColor.a(backgroundColor) * alpha);

        int textWithAlpha = VGColor.rgba(
                VGColor.r(textColor),
                VGColor.g(textColor),
                VGColor.b(textColor),
                VGColor.a(textColor) * alpha);

        new VGCard(x, y, width, height)
                .setRadius(6f)
                .setBackgroundColor(bgWithAlpha)
                .render();

        if (maxWidth > 0) {
            VG.multilineText(getText(), x + paddingX, y + paddingY, maxWidth - paddingX * 2, textWithAlpha);
        } else {
            VGTextLayout.TextPosition pos = VGTextLayout.left(getText(), x + paddingX, y, height, font);
            VG.text(getText(), pos.x(), pos.y(), textWithAlpha);
        }
    }

    // ── Static Utilities ──

    public static float textWidth(String text, int fontSize, float paddingX) {
        return textWidth(text, fontSize, paddingX, null);
    }

    public static float textWidth(String text, int fontSize, float paddingX, VGFont font) {
        return VGMeasure.measureText(text, font).withPadding(paddingX, 0f).width();
    }

    public static float textHeight(int fontSize, float paddingY) {
        return textHeight(fontSize, paddingY, null);
    }

    public static float textHeight(int fontSize, float paddingY, VGFont font) {
        return VGFontMetrics.lineHeight(font) + paddingY * 2;
    }

    public static PositionResult calculatePosition(
            float tipW, float tipH,
            float srcX, float srcY, float srcW, float srcH,
            Direction preferred,
            float screenW, float screenH) {

        float gap = 8f;
        float margin = 10f;
        Direction[] order = {preferred, Direction.BOTTOM, Direction.LEFT, Direction.RIGHT};

        for (Direction dir : order) {
            float tx = calcTipX(dir, tipW, srcX, srcW);
            float ty = calcTipY(dir, tipH, srcY, srcH, gap);

            if (tx >= margin && ty >= margin
                    && tx + tipW <= screenW - margin
                    && ty + tipH <= screenH - margin) {
                return new PositionResult(tx, ty, dir);
            }
        }

        return new PositionResult(
                calcTipX(preferred, tipW, srcX, srcW),
                calcTipY(preferred, tipH, srcY, srcH, gap),
                preferred);
    }

    private static float calcTipX(Direction dir, float w, float srcX, float srcW) {
        return switch (dir) {
            case TOP, BOTTOM -> srcX + (srcW - w) / 2;
            case RIGHT -> srcX + srcW + 12;
            case LEFT -> srcX - w - 12;
        };
    }

    private static float calcTipY(Direction dir, float h, float srcY, float srcH, float gap) {
        return switch (dir) {
            case TOP -> srcY - h - gap;
            case BOTTOM -> srcY + srcH + gap;
            case LEFT, RIGHT -> srcY + (srcH - h) / 2;
        };
    }
}