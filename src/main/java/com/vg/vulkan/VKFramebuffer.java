package com.vg.vulkan;

import com.vg.core.util.VKUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VKFramebuffer implements AutoCloseable {

    private final VkDevice device;
    private final long[] framebuffers;

    public VKFramebuffer(VkDevice device, VKSwapchain swapchain, VKRenderPass renderPass,
                         long[] msaaImageViews) {
        this.device = device;

        int imageCount = swapchain.getImageCount();
        this.framebuffers = new long[imageCount];

        try (MemoryStack stack = MemoryStack.stackPush()) {
            for (int i = 0; i < imageCount; i++) {
                LongBuffer pAttachments = stack.longs(
                        msaaImageViews[i],
                        swapchain.getImageViews()[i]
                );

                VkFramebufferCreateInfo createInfo = VkFramebufferCreateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                        .renderPass(renderPass.get())
                        .pAttachments(pAttachments)
                        .width(swapchain.getExtent().width())
                        .height(swapchain.getExtent().height())
                        .layers(1);

                LongBuffer pFramebuffer = stack.mallocLong(1);
                int result = vkCreateFramebuffer(device, createInfo, null, pFramebuffer);
                VKUtil.check(result, "Failed to create framebuffer " + i);

                framebuffers[i] = pFramebuffer.get(0);
            }
        }
    }

    public long get(int index) {
        return framebuffers[index];
    }

    @Override
    public void close() {
        for (long fb : framebuffers) {
            vkDestroyFramebuffer(device, fb, null);
        }
    }
}
