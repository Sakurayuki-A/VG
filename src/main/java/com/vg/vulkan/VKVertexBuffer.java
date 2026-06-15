package com.vg.vulkan;

import com.vg.core.util.VKUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.FloatBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VKVertexBuffer implements AutoCloseable {

    private final VkDevice device;
    private final long vertexBuffer;
    private final long vertexBufferMemory;
    private final int vertexCount;

    private static final float[] VERTICES = {
        0.0f, -0.5f,  1.0f, 0.0f, 0.0f,
       -0.5f,  0.5f,  0.0f, 1.0f, 0.0f,
        0.5f,  0.5f,  0.0f, 0.0f, 1.0f
    };

    public VKVertexBuffer(VkDevice device, VKDevice vkDevice) {
        this.device = device;
        this.vertexCount = VERTICES.length / 5;

        long bufferSize = VERTICES.length * 4L;

        long stagingBuffer;
        long stagingBufferMemory;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffer = stack.mallocLong(1);
            LongBuffer pMemory = stack.mallocLong(1);

            createBuffer(device, vkDevice, bufferSize,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    pBuffer, pMemory);
            stagingBuffer = pBuffer.get(0);
            stagingBufferMemory = pMemory.get(0);

            FloatBuffer vertexData = stack.mallocFloat(VERTICES.length);
            vertexData.put(VERTICES).flip();

            PointerBuffer pMappedMemory = stack.mallocPointer(1);
            VKUtil.check(vkMapMemory(device, stagingBufferMemory, 0, bufferSize, 0, pMappedMemory),
                    "Failed to map staging buffer memory");

            MemoryUtil.memCopy(MemoryUtil.memAddress(vertexData), pMappedMemory.get(0), bufferSize);
            vkUnmapMemory(device, stagingBufferMemory);

            createBuffer(device, vkDevice, bufferSize,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                    pBuffer, pMemory);
            vertexBuffer = pBuffer.get(0);
            vertexBufferMemory = pMemory.get(0);

            copyBuffer(device, vkDevice, stagingBuffer, vertexBuffer, bufferSize);
        }

        vkDestroyBuffer(device, stagingBuffer, null);
        vkFreeMemory(device, stagingBufferMemory, null);
    }

    private void createBuffer(VkDevice device, VKDevice vkDevice, long size, int usage,
                              int properties, LongBuffer pBuffer, LongBuffer pMemory) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(size)
                    .usage(usage)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            int result = vkCreateBuffer(device, bufferInfo, null, pBuffer);
            VKUtil.check(result, "Failed to create buffer");

            VkMemoryRequirements memRequirements = VkMemoryRequirements.calloc(stack);
            vkGetBufferMemoryRequirements(device, pBuffer.get(0), memRequirements);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memRequirements.size())
                    .memoryTypeIndex(findMemoryType(vkDevice, memRequirements.memoryTypeBits(), properties));

            result = vkAllocateMemory(device, allocInfo, null, pMemory);
            VKUtil.check(result, "Failed to allocate buffer memory");

            vkBindBufferMemory(device, pBuffer.get(0), pMemory.get(0), 0);
        }
    }

    private void copyBuffer(VkDevice device, VKDevice vkDevice, long srcBuffer, long dstBuffer, long size) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long commandPool = vkDevice.createTransientCommandPool();

            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandPool(commandPool)
                    .commandBufferCount(1);

            PointerBuffer pCommandBuffer = stack.mallocPointer(1);
            int result = vkAllocateCommandBuffers(device, allocInfo, pCommandBuffer);
            VKUtil.check(result, "Failed to allocate command buffer");

            VkCommandBuffer commandBuffer = new VkCommandBuffer(pCommandBuffer.get(0), device);

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

            vkBeginCommandBuffer(commandBuffer, beginInfo);

            VkBufferCopy.Buffer copyRegion = VkBufferCopy.calloc(1, stack)
                    .size(size);
            vkCmdCopyBuffer(commandBuffer, srcBuffer, dstBuffer, copyRegion);

            vkEndCommandBuffer(commandBuffer);

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(stack.pointers(commandBuffer.address()));

            vkQueueSubmit(vkDevice.getGraphicsQueue(), submitInfo, VK_NULL_HANDLE);
            vkQueueWaitIdle(vkDevice.getGraphicsQueue());

            vkFreeCommandBuffers(device, commandPool, stack.pointers(commandBuffer.address()));
            vkDevice.destroyTransientCommandPool(commandPool);
        }
    }

    private int findMemoryType(VKDevice vkDevice, int typeFilter, int properties) {
        VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.calloc();
        vkGetPhysicalDeviceMemoryProperties(vkDevice.getPhysicalDevice(), memProperties);

        for (int i = 0; i < memProperties.memoryTypeCount(); i++) {
            if ((typeFilter & (1 << i)) != 0 &&
                    (memProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
                return i;
            }
        }

        throw new RuntimeException("Failed to find suitable memory type");
    }

    public long get() {
        return vertexBuffer;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    @Override
    public void close() {
        vkDestroyBuffer(device, vertexBuffer, null);
        vkFreeMemory(device, vertexBufferMemory, null);
    }
}
