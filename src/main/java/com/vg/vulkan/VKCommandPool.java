package com.vg.vulkan;

import com.vg.core.util.VKUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VKCommandPool implements AutoCloseable {

    private final VkDevice device;
    private final long commandPool;

    public VKCommandPool(VkDevice device, int queueFamilyIndex) {
        this.device = device;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                    .queueFamilyIndex(queueFamilyIndex);

            LongBuffer pCommandPool = stack.mallocLong(1);
            int result = vkCreateCommandPool(device, poolInfo, null, pCommandPool);
            VKUtil.check(result, "Failed to create command pool");

            this.commandPool = pCommandPool.get(0);
        }
    }

    public VkCommandBuffer[] allocateCommandBuffers(int count) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(commandPool)
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(count);

            var pCommandBuffers = stack.mallocPointer(count);
            int result = vkAllocateCommandBuffers(device, allocInfo, pCommandBuffers);
            VKUtil.check(result, "Failed to allocate command buffers");

            VkCommandBuffer[] buffers = new VkCommandBuffer[count];
            for (int i = 0; i < count; i++) {
                buffers[i] = new VkCommandBuffer(pCommandBuffers.get(i), device);
            }
            return buffers;
        }
    }

    public void freeCommandBuffers(VkCommandBuffer[] buffers) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pBuffers = stack.mallocPointer(buffers.length);
            for (VkCommandBuffer buffer : buffers) {
                pBuffers.put(buffer.address());
            }
            pBuffers.flip();
            vkFreeCommandBuffers(device, commandPool, pBuffers);
        }
    }

    public long get() {
        return commandPool;
    }

    @Override
    public void close() {
        vkDestroyCommandPool(device, commandPool, null);
    }
}