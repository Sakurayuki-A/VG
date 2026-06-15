package com.vg.core.command;

/**
 * 绘图命令基类
 * 支持图层系统，layer 数值越小越先绘制（在底层）
 */
public abstract class DrawCommand {
    
    public enum CommandType {
        RECT,
        ROUND_RECT,
        LINE,
        TEXT,
        ICON,
        GRADIENT_RECT,
        GRADIENT_ROUND_RECT,
        SHADOW,
        RAW_VERTICES,
        CLIP_PUSH,
        CLIP_POP
    }
    
    /** 图层索引，默认 0（背景层） */
    protected int layer = 0;
    
    /**
     * 3×3 仿射变换矩阵（行主序 9 元素），null 或单位矩阵 = 无变换。
     * 在命令创建时由 VG façade 设置，VertexBatcher 应用。
     */
    public float[] matrix;
    
    public abstract int color();
    public abstract CommandType type();
    
    public int getLayer() {
        return layer;
    }
    
    public void setLayer(int layer) {
        this.layer = layer;
    }
}