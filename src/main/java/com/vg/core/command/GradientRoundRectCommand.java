package com.vg.core.command;

public class GradientRoundRectCommand extends DrawCommand {
    public final float x, y, width, height, radius;
    public final int topLeft, topRight, bottomLeft, bottomRight;
    
    public GradientRoundRectCommand(float x, float y, float width, float height, float radius,
                                    int topLeft, int topRight, int bottomLeft, int bottomRight) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.radius = radius;
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
        return CommandType.GRADIENT_ROUND_RECT;
    }
}
