package com.vg.core.command;

import com.vg.core.font.VGFont;

/**
 * 文本绘制命令
 */
public class TextCommand extends DrawCommand {
    public final String text;
    public final float x, y;
    public final int color;
    public final VGFont font;

    public TextCommand(String text, float x, float y, int color, VGFont font) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.color = color;
        this.font = font;
    }

    @Override
    public int color() {
        return color;
    }

    @Override
    public CommandType type() {
        return CommandType.TEXT;
    }
}