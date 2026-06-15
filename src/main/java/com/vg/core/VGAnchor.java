package com.vg.core;

/**
 * 兼容性锚点枚举（5 点简化版）
 *
 * @deprecated 请使用 {@link Anchor}（9 点完整版）。本枚举仅保留兼容映射，不再添加新功能。
 */
@Deprecated(since = "1.0", forRemoval = false)
public enum VGAnchor {
    TOP_LEFT,
    TOP_RIGHT,
    CENTER,
    BOTTOM_LEFT,
    BOTTOM_RIGHT;

    public Anchor toAnchor() {
        return switch (this) {
            case TOP_LEFT -> Anchor.TOP_LEFT;
            case TOP_RIGHT -> Anchor.TOP_RIGHT;
            case CENTER -> Anchor.CENTER;
            case BOTTOM_LEFT -> Anchor.BOTTOM_LEFT;
            case BOTTOM_RIGHT -> Anchor.BOTTOM_RIGHT;
        };
    }
}
