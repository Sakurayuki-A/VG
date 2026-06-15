package com.vg.core.command;

/**
 * 存储预计算顶点数据的命令
 * 用于批量提交大量已经计算好 NDC 坐标的顶点数据
 * 顶点格式：(x, y, r, g, b, a) 共 6 个 float 每个顶点
 */
public class RawVertexCommand extends DrawCommand {
    public final float[] vertices;

    public RawVertexCommand(float[] vertices) {
        this.vertices = vertices;
    }

    @Override
    public int color() {
        return 0;
    }

    @Override
    public CommandType type() {
        return CommandType.RAW_VERTICES;
    }
}