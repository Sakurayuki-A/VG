package com.vg.core.command;

import com.vg.core.icon.Icons;

public class IconCommand extends DrawCommand {
    public final Icons icon;
    public final float x, y;
    public final float size;
    public final int color;

    public IconCommand(Icons icon, float x, float y, float size, int color) {
        this.icon = icon;
        this.x = x;
        this.y = y;
        this.size = size;
        this.color = color;
    }

    @Override
    public int color() {
        return color;
    }
    
    @Override
    public CommandType type() {
        return CommandType.ICON;
    }
}
