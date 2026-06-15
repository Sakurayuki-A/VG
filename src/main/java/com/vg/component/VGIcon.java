package com.vg.component;

import com.vg.core.VG;
import com.vg.core.VGMeasure;
import com.vg.core.color.VGColor;
import com.vg.core.icon.Icons;
import com.vg.core.icon.VGIconFont;
import com.vg.core.input.MouseContext;
import com.vg.core.node.VGNode;

import java.util.function.Supplier;

public class VGIcon extends VGNode {

    private Icons icon;
    private Supplier<Icons> dynamicIcon = null;
    private float size = 16f;
    private int color = VGColor.WHITE;
    private float alpha = 1f;

    public VGIcon() {}

    public VGIcon(Icons icon) {
        this.icon = icon;
    }

    public VGIcon setIcon(Icons icon) {
        this.icon = icon;
        this.dynamicIcon = null;
        return this;
    }

    public VGIcon setIcon(Supplier<Icons> supplier) {
        this.dynamicIcon = supplier;
        this.icon = null;
        return this;
    }

    public Icons getIcon() {
        return dynamicIcon != null ? dynamicIcon.get() : icon;
    }

    public VGIcon setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public VGIcon setX(float x) {
        this.x = x;
        return this;
    }

    public VGIcon setY(float y) {
        this.y = y;
        return this;
    }

    public float getX() { return x; }

    public float getY() { return y; }

    public VGIcon setSize(float size) {
        this.size = Math.max(1f, size);
        return this;
    }

    public float getSize() { return size; }

    public VGIcon setColor(int color) {
        this.color = color;
        return this;
    }

    public int getColor() { return color; }

    public VGIcon setAlpha(float alpha) {
        this.alpha = Math.max(0f, Math.min(1f, alpha));
        return this;
    }

    public float getAlpha() { return alpha; }

    public float iconWidth() {
        Icons ic = getIcon();
        if (ic == null) return 0;
        return size;
    }

    public float iconHeight() {
        return size;
    }

    public float centerY(float containerY, float containerHeight) {
        return VG.getIconFont().centerY(containerY, containerHeight, size);
    }

    public float centerBaseline(float containerY, float containerHeight) {
        return centerY(containerY, containerHeight);
    }

    @Override
    public boolean contains(float px, float py) {
        float w = iconWidth();
        float h = iconHeight();
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    // ── Lifecycle ──

    @Override
    public void update(float dt, MouseContext mouse) {
        // VGIcon has no animation state to update
    }

    // ── Render ──

    @Override
    public void render() {
        if (!visible) return;
        Icons ic = getIcon();
        if (ic == null || alpha < 0.005f) return;

        int c = VGColor.rgba(
                VGColor.r(color),
                VGColor.g(color),
                VGColor.b(color),
                VGColor.a(color) * alpha);

        VG.icon(ic, x, y, size, c);
    }
}