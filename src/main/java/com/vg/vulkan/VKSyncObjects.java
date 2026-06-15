package com.vg.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkDevice;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VKSyncObjects implements AutoCloseable {

    private final VkDevice device;
    private final int imageCount;
    private final int maxFramesInFlight;
    private final long[] imageAvailableSemaphores;
    private final long[] renderFinishedSemaphores;
    private final long[] inFlightFences;

    public VKSyncObjects(VkDevice device, int imageCount, int maxFramesInFlight) {
        this.device = device;
        this.imageCount = imageCount;
        this.maxFramesInFlight = maxFramesInFlight;

        this.imageAvailableSemaphores = new long[maxFramesInFlight];
        this.renderFinishedSemaphores = new long[imageCount];
        this.inFlightFences = new long[maxFramesInFlight];

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                    .flags(VK_FENCE_CREATE_SIGNALED_BIT);

            LongBuffer pSemaphore = stack.mallocLong(1);
            LongBuffer pFence = stack.mallocLong(1);

            for (int i = 0; i < maxFramesInFlight; i++) {
                vkCreateSemaphore(device, semaphoreInfo, null, pSemaphore);
                imageAvailableSemaphores[i] = pSemaphore.get(0);
            }

            for (int i = 0; i < imageCount; i++) {
                vkCreateSemaphore(device, semaphoreInfo, null, pSemaphore);
                renderFinishedSemaphores[i] = pSemaphore.get(0);
            }

            for (int i = 0; i < maxFramesInFlight; i++) {
                vkCreateFence(device, fenceInfo, null, pFence);
                inFlightFences[i] = pFence.get(0);
            }
        }
    }

    public long getImageAvailableSemaphore(int frameIndex) {
        return imageAvailableSemaphores[frameIndex];
    }

    public long getRenderFinishedSemaphore(int imageIndex) {
        return renderFinishedSemaphores[imageIndex];
    }

    public long getInFlightFence(int frameIndex) {
        return inFlightFences[frameIndex];
    }

    @Override
    public void close() {
        for (int i = 0; i < maxFramesInFlight; i++) {
            vkDestroySemaphore(device, imageAvailableSemaphores[i], null);
        }
        for (int i = 0; i < imageCount; i++) {
            vkDestroySemaphore(device, renderFinishedSemaphores[i], null);
        }
        for (int i = 0; i < maxFramesInFlight; i++) {
            vkDestroyFence(device, inFlightFences[i], null);
        }
    }
}