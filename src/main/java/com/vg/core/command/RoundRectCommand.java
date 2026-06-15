package com.vg.core.command;

/**
 * 圆角矩形绘制命令
 */
public class RoundRectCommand extends DrawCommand {
    public final float x, y, width, height, radius;
    public final int color;
    
    public RoundRectCommand(float x, float y, float width, float height, float radius, int color) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.radius = radius;
        this.color = color;
    }
    
    @Override
    public int color() {
        return color;
    }
    
    @Override
    public CommandType type() {
        return CommandType.ROUND_RECT;
    }
}