package com.vg.vulkan;

import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VkInstance;

public class VKSurface implements AutoCloseable {

    private final VkInstance instance;
    private final long surface;

    public VKSurface(VkInstance instance, long surface) {
        this.instance = instance;
        this.surface = surface;
    }

    public long get() {
        return surface;
    }

    @Override
    public void close() {
        KHRSurface.vkDestroySurfaceKHR(instance, surface, null);
    }
}