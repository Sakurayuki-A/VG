package com.vg.core;

public class AnchorPosition {
    
    private final RenderContext ctx;
    private final Anchor anchor;
    private float offsetX = 0;
    private float offsetY = 0;
    
    public AnchorPosition(RenderContext ctx, Anchor anchor) {
        this.ctx = ctx;
        this.anchor = anchor;
    }
    
    public AnchorPosition offset(float x, float y) {
        this.offsetX = x;
        this.offsetY = y;
        return this;
    }
    
    public VGPosition position(float w, float h) {
        VGPosition base = ctx.align(anchor, w, h);
        return new VGPosition(base.x() + offsetX, base.y() + offsetY);
    }
}
