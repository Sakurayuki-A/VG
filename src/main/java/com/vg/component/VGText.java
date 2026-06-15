package com.vg.component;

import com.vg.core.VG;
import com.vg.core.VGMeasure;
import com.vg.core.color.VGColor;
import com.vg.core.font.VGFont;
import com.vg.core.font.VGFontMetrics;
import com.vg.core.input.MouseContext;
import com.vg.core.node.VGNode;

import java.util.function.Supplier;

public class VGText extends VGNode {

    private String staticText = "";
    private Supplier<String> dynamicText = null;
    private int color = VGColor.rgb(0xf0, 0xf0, 0xf0);
    private int fontSize = 14;
    private float alpha = 1f;
    private VGFont font;

    public VGText() {}

    public VGText(String text) {
        this.staticText = text != null ? text : "";
    }

    // ── Text ──

    public VGText setText(String text) {
        this.staticText = text != null ? text : "";
        this.dynamicText = null;
        return this;
    }

    public VGText setText(Supplier<String> supplier) {
        this.dynamicText = supplier;
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

    // ── Position ──

    public VGText setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public float getX() { return x; }

    public float getY() { return y; }

    // ── Style ──

    public VGText setColor(int color) {
        this.color = color;
        return this;
    }

    public int getColor() { return color; }

    public VGText setSize(int size) {
        this.fontSize = Math.max(1, size);
        return this;
    }

    public int getSize() { return fontSize; }

    public VGText setAlpha(float alpha) {
        this.alpha = Math.max(0f, Math.min(1f, alpha));
        return this;
    }

    public float getAlpha() { return alpha; }

    public VGText setFont(VGFont font) {
        this.font = font;
        return this;
    }

    // ── Geometry ──

    public float textWidth() {
        return font != null ? VG.measureText(getText(), font) : VG.measureText(getText());
    }

    public float textHeight() {
        return VGFontMetrics.textBoxHeight(font);
    }

    @Override
    public boolean contains(float px, float py) {
        float w = textWidth();
        float h = textHeight();
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    // ── Lifecycle ──

    @Override
    public void update(float dt, MouseContext mouse) {
        // VGText has no animation state to update
    }

    // ── Render ──

    @Override
    public void render() {
        if (!visible || alpha < 0.005f || getText().isEmpty()) return;

        int c = VGColor.rgba(
                VGColor.r(color),
                VGColor.g(color),
                VGColor.b(color),
                VGColor.a(color) * alpha);

        VG.text(getText(), x, y, c);
    }

    // ── Static Utilities ──

    public static float measureWidth(String text, int fontSize) {
        return VGMeasure.measureText(text).width();
    }

    public static float measureHeight(int fontSize) {
        return VGFontMetrics.textBoxHeight(null);
    }
}
