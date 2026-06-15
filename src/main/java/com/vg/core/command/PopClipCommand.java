package com.vg.core.command;

/**
 * 裁剪区域弹出命令
 * <p>
 * 从渲染器的裁剪栈中弹出一个裁剪区域，
 * 恢复父裁剪区域或全屏。
 */
public class PopClipCommand extends DrawCommand {

    public PopClipCommand() {
    }

    @Override
    public int color() {
        return 0;
    }

    @Override
    public CommandType type() {
        return CommandType.CLIP_POP;
    }
}