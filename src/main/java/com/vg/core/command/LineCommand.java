package com.vg.core.command;

/**
 * 线条绘制命令
 */
public class LineCommand extends DrawCommand {
    public final float x1, y1, x2, y2, lineWidth;
    public final int color;
    
    public LineCommand(float x1, float y1, float x2, float y2, float lineWidth, int color) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.lineWidth = lineWidth;
        this.color = color;
    }
    
    @Override
    public int color() {
        return color;
    }
    
    @Override
    public CommandType type() {
        return CommandType.LINE;
    }
}