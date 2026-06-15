package com.vg.core.command;

/**
 * 阴影绘制命令
 */
public class ShadowCommand extends DrawCommand {
    public final float x, y, width, height, spread, blur;
    public final int color;
    
    public ShadowCommand(float x, float y, float width, float height,
                         float spread, float blur, int color) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.spread = spread;
        this.blur = blur;
        this.color = color;
    }
    
    @Override
    public int color() {
        return color;
    }
    
    @Override
    public CommandType type() {
        return CommandType.SHADOW;
    }
}