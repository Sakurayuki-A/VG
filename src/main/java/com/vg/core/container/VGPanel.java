package com.vg.core.container;

import com.vg.core.VG;
import com.vg.core.color.VGColor;
import com.vg.core.node.VGContainer;

public class VGPanel extends VGContainer {

    private float radius = 0;
    private int backgroundColor = VGColor.rgba(0, 0, 0, 0);

    private boolean hasShadow = false;
    private float shadowSpread = 0;
    private float shadowBlur = 0;
    private int shadowColor = VGColor.rgba(0, 0, 0, 0.3f);

    public VGPanel(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // ── Style ──

    public VGPanel setRadius(float radius) {
        this.radius = radius;
        return this;
    }

    public float getRadius() { return radius; }

    public VGPanel setBackgroundColor(int color) {
        this.backgroundColor = color;
        return this;
    }

    public int getBackgroundColor() { return backgroundColor; }

    public VGPanel setShadow(float spread, float blur, int color) {
        this.hasShadow = true;
        this.shadowSpread = spread;
        this.shadowBlur = blur;
        this.shadowColor = color;
        return this;
    }

    // ── Render ──

    @Override
    public void render() {
        if (!visible) return;

        // 1. Shadow
        if (hasShadow) {
            VG.shadow(x, y, width, height, shadowSpread, shadowBlur, shadowColor);
        }

        // 2. Background
        if (radius > 0) {
            VG.roundRect(x, y, width, height, radius, backgroundColor);
        } else {
            VG.rect(x, y, width, height, backgroundColor);
        }

        // 3. Children
        super.render();
    }
}