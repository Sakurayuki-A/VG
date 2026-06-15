package com.vg.core.command;

/**
 * 渐变矩形绘制命令
 */
public class GradientRectCommand extends DrawCommand {
    public final float x, y, width, height;
    public final int topLeft, topRight, bottomLeft, bottomRight;
    
    public GradientRectCommand(float x, float y, float width, float height,
                               int topLeft, int topRight, int bottomLeft, int bottomRight) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.topLeft = topLeft;
        this.topRight = topRight;
        this.bottomLeft = bottomLeft;
        this.bottomRight = bottomRight;
    }
    
    @Override
    public int color() {
        return topLeft;
    }
    
    @Override
    public CommandType type() {
        return CommandType.GRADIENT_RECT;
    }
}