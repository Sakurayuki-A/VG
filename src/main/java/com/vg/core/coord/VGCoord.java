package com.vg.core.coord;

public final class VGCoord {

    private VGCoord() {
    }

    public static float pixelToNdcX(float pixelX, int framebufferWidth) {
        return (pixelX / framebufferWidth) * 2.0f - 1.0f;
    }

    public static float pixelToNdcY(float pixelY, int framebufferHeight) {
        // Vulkan NDC: Y=-1 在顶部，Y=1 在底部
        // 映射：pixelY=0 → NDC=-1, pixelY=height → NDC=1
        return (pixelY / framebufferHeight) * 2.0f - 1.0f;
    }

    public static float ndcToPixelX(float ndcX, int framebufferWidth) {
        return (ndcX + 1.0f) * 0.5f * framebufferWidth;
    }

    public static float ndcToPixelY(float ndcY, int framebufferHeight) {
        return (ndcY + 1.0f) * 0.5f * framebufferHeight;
    }
}