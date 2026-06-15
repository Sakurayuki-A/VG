package com.vg.core.command;

/**
 * 矩形绘制命令
 */
public class RectCommand extends DrawCommand {
    public final float x, y, width, height;
    public final int color;
    
    public RectCommand(float x, float y, float width, float height, int color) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color;
    }
    
    @Override
    public int color() {
        return color;
    }
    
    @Override
    public CommandType type() {
        return CommandType.RECT;
    }
}