package com.vg.vulkan;

import com.vg.core.util.VKUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

import java.nio.FloatBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.VK10.*;

/**
 * 动态顶点缓冲区，专用于 SDF 阴影渲染
 * 顶点格式：position(2) + color(3) + alpha(1) + shadowRect(4) + blurParams(2) = 12 floats
 */
public class VKDynamicShadowVertexBuffer implements AutoCloseable {

    private final VkDevice device;
    private final VKDevice vkDevice;
    private final long buffer;
    private final long bufferMemory;
    private final int maxVertexCount;

    public VKDynamicShadowVertexBuffer(VkDevice device, VKDevice vkDevice, int maxVertexCount) {
        this.device = device;
        this.vkDevice = vkDevice;
        this.maxVertexCount = maxVertexCount;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            // 每个顶点 12 floats，每个 float 4 bytes
            long bufferSize = maxVertexCount * 12L * 4L;

            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(bufferSize)
                    .usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            LongBuffer pBuffer = stack.mallocLong(1);
            int result = vkCreateBuffer(device, bufferInfo, null, pBuffer);
            VKUtil.check(result, "Failed to create shadow vertex buffer");
            buffer = pBuffer.get(0);

            VkMemoryRequirements memReqs = VkMemoryRequirements.calloc(stack);
            vkGetBufferMemoryRequirements(device, buffer, memReqs);

            int memTypeIndex = findMemoryType(memReqs.memoryTypeBits(),
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memReqs.size())
                    .memoryTypeIndex(memTypeIndex);

            LongBuffer pMemory = stack.mallocLong(1);
            result = vkAllocateMemory(device, allocInfo, null, pMemory);
            VKUtil.check(result, "Failed to allocate shadow vertex buffer memory");
            bufferMemory = pMemory.get(0);

            result = vkBindBufferMemory(device, buffer, bufferMemory, 0);
            VKUtil.check(result, "Failed to bind shadow vertex buffer memory");
        }
    }

    public void updateVertices(float[] vertices) {
        if (vertices.length == 0) {
            return;
        }

        int vertexCount = vertices.length / 12;
        if (vertexCount > maxVertexCount) {
            throw new IllegalArgumentException("Too many vertices: " + vertexCount + " (max: " + maxVertexCount + ")");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pData = stack.mallocPointer(1);
            int result = vkMapMemory(device, bufferMemory, 0, vertices.length * 4L, 0, pData);
            VKUtil.check(result, "Failed to map shadow vertex buffer memory");

            long dataPtr = pData.get(0);
            FloatBuffer fb = memAllocFloat(vertices.length);
            fb.put(vertices).flip();
            
            org.lwjgl.system.MemoryUtil.memCopy(org.lwjgl.system.MemoryUtil.memAddress(fb), dataPtr, vertices.length * 4L);
            
            memFree(fb);

            vkUnmapMemory(device, bufferMemory);
        }
    }

    private int findMemoryType(int typeFilter, int properties) {
        VkPhysicalDeviceMemoryProperties memProps = vkDevice.getMemoryProperties();
        for (int i = 0; i < memProps.memoryTypeCount(); i++) {
            if ((typeFilter & (1 << i)) != 0
                    && (memProps.memoryTypes(i).propertyFlags() & properties) == properties) {
                return i;
            }
        }
        throw new RuntimeException("Failed to find suitable memory type for shadow vertex buffer");
    }

    public long get() {
        return buffer;
    }

    @Override
    public void close() {
        if (bufferMemory != 0) {
            vkFreeMemory(device, bufferMemory, null);
        }
        if (buffer != 0) {
            vkDestroyBuffer(device, buffer, null);
        }
    }
}
