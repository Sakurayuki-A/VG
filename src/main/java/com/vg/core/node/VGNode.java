package com.vg.core.node;

import com.vg.core.input.MouseContext;

public abstract class VGNode {

    protected float x;
    protected float y;
    protected float width;
    protected float height;

    protected boolean visible = true;
    protected boolean enabled = true;

    protected int zIndex = 0;

    protected VGContainer parent;

    public abstract void update(float dt, MouseContext mouse);

    public abstract void render();

    public float worldX() {
        return parent == null ? x : parent.worldX() + x;
    }

    public float worldY() {
        return parent == null ? y : parent.worldY() + y;
    }

    public boolean contains(float px, float py) {
        if (!visible) return false;
        return px >= worldX()
                && px <= worldX() + width
                && py >= worldY()
                && py <= worldY() + height;
    }

    public float right() { return x + width; }
    public float bottom() { return y + height; }
    public float centerX() { return x + width / 2f; }
    public float centerY() { return y + height / 2f; }

    // --- Getters & Setters ---

    public float x() { return x; }
    public void x(float x) { this.x = x; }

    public float y() { return y; }
    public void y(float y) { this.y = y; }

    public float width() { return width; }
    public void width(float width) { this.width = width; }

    public float height() { return height; }
    public void height(float height) { this.height = height; }

    public boolean isVisible() { return visible; }
    public VGNode setVisible(boolean visible) { this.visible = visible; return this; }

    public boolean isEnabled() { return enabled; }
    public VGNode setEnabled(boolean enabled) { this.enabled = enabled; return this; }

    public int zIndex() { return zIndex; }
    public void zIndex(int zIndex) { this.zIndex = zIndex; }

    public VGContainer parent() { return parent; }
    public void parent(VGContainer parent) { this.parent = parent; }

    // ── Focus hooks (被 RenderContext.setFocus 调用，子类可重写) ──

    public void requestFocus() {}
    public void loseFocus() {}
}