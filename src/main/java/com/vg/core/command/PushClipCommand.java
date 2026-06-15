package com.vg.core.command;

/**
 * 裁剪区域推入命令
 * <p>
 * 将指定的裁剪矩形推入渲染器的裁剪栈，
 * 与父裁剪区域求交集后生效。
 */
public class PushClipCommand extends DrawCommand {
    public final float x, y, width, height;

    public PushClipCommand(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public int color() {
        return 0;
    }

    @Override
    public CommandType type() {
        return CommandType.CLIP_PUSH;
    }
}