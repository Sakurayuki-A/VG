package com.vg.core;

/**
 * 位置对象，表示二维坐标
 */
public class VGPosition {
    
    private final float x;
    private final float y;
    
    public VGPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    public float x() {
        return x;
    }
    
    public float y() {
        return y;
    }
}
