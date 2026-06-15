package com.vg.vulkan;

import com.vg.core.util.VKUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public final class VKFontTexture implements AutoCloseable {

    private final VkDevice device;
    private final VKDevice vkDevice;
    private final long textureImage;
    private final long textureImageMemory;
    private final long textureImageView;
    private final long textureSampler;

    private final int width;
    private final int height;

    // 每个字体纹理独立持有描述符集，避免在命令缓冲区录制期间更新
    private long descriptorSet = VK_NULL_HANDLE;

    public VKFontTexture(VkDevice device, VKDevice vkDevice, ByteBuffer pixels,
                         int width, int height) {
        this.device = device;
        this.vkDevice = vkDevice;
        this.width = width;
        this.height = height;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pImage = stack.mallocLong(1);
            LongBuffer pImageMemory = stack.mallocLong(1);

            createImage(device, vkDevice, width, height,
                    VK_FORMAT_R8G8B8A8_UNORM,
                    VK_IMAGE_TILING_OPTIMAL,
                    VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                    pImage, pImageMemory);

            textureImage = pImage.get(0);
            textureImageMemory = pImageMemory.get(0);

            uploadPixelData(device, vkDevice, textureImage, width, height, pixels);

            textureImageView = createImageView(device, textureImage, VK_FORMAT_R8G8B8A8_UNORM);
            textureSampler = createTextureSampler(device);
        }
    }

    private void createImage(VkDevice device, VKDevice vkDevice, int width, int height,
                             int format, int tiling, int usage, int properties,
                             LongBuffer pImage, LongBuffer pImageMemory) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                    .imageType(VK_IMAGE_TYPE_2D)
                    .extent(extent -> extent.width(width).height(height).depth(1))
                    .mipLevels(1)
                    .arrayLayers(1)
                    .format(format)
                    .tiling(tiling)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .usage(usage)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .samples(VK_SAMPLE_COUNT_1_BIT);

            int result = vkCreateImage(device, imageInfo, null, pImage);
            VKUtil.check(result, "Failed to create texture image");

            VkMemoryRequirements memRequirements = VkMemoryRequirements.calloc(stack);
            vkGetImageMemoryRequirements(device, pImage.get(0), memRequirements);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memRequirements.size())
                    .memoryTypeIndex(findMemoryType(vkDevice, memRequirements.memoryTypeBits(), properties));

            result = vkAllocateMemory(device, allocInfo, null, pImageMemory);
            VKUtil.check(result, "Failed to allocate texture image memory");

            vkBindImageMemory(device, pImage.get(0), pImageMemory.get(0), 0);
        }
    }

    private void uploadPixelData(VkDevice device, VKDevice vkDevice, long image,
                                  int width, int height, ByteBuffer pixels) {
        long imageSize = (long) width * height * 4;

        // Use capacity() instead of remaining() since the buffer may have been flipped
        long pixelBufferSize = pixels.capacity();
        if (pixelBufferSize != imageSize) {
            throw new IllegalArgumentException(
                "Pixel buffer size mismatch: expected " + imageSize + 
                " but got " + pixelBufferSize);
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pStagingBuffer = stack.mallocLong(1);
            LongBuffer pStagingMemory = stack.mallocLong(1);

            createBuffer(device, vkDevice, imageSize,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    pStagingBuffer, pStagingMemory);

            long stagingBuffer = pStagingBuffer.get(0);
            long stagingMemory = pStagingMemory.get(0);

            PointerBuffer pData = stack.mallocPointer(1);
            VKUtil.check(vkMapMemory(device, stagingMemory, 0, imageSize, 0, pData),
                    "Failed to map texture staging memory");
            MemoryUtil.memCopy(MemoryUtil.memAddress(pixels), pData.get(0), imageSize);
            vkUnmapMemory(device, stagingMemory);

            transitionImageLayout(device, vkDevice, image,
                    VK_IMAGE_LAYOUT_UNDEFINED,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);

            copyBufferToImage(device, vkDevice, stagingBuffer, image, width, height);

            transitionImageLayout(device, vkDevice, image,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

            vkDestroyBuffer(device, stagingBuffer, null);
            vkFreeMemory(device, stagingMemory, null);
        }
    }

    private void transitionImageLayout(VkDevice device, VKDevice vkDevice, long image,
                                        int oldLayout, int newLayout) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long commandPool = vkDevice.createTransientCommandPool();

            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandPool(commandPool)
                    .commandBufferCount(1);

            PointerBuffer pCommandBuffer = stack.mallocPointer(1);
            vkAllocateCommandBuffers(device, allocInfo, pCommandBuffer);
            VkCommandBuffer commandBuffer = new VkCommandBuffer(pCommandBuffer.get(0), device);

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

            vkBeginCommandBuffer(commandBuffer, beginInfo);

            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .oldLayout(oldLayout)
                    .newLayout(newLayout)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .image(image)
                    .subresourceRange(subresource -> subresource
                            .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                            .baseMipLevel(0)
                            .levelCount(1)
                            .baseArrayLayer(0)
                            .layerCount(1));

            int srcStage;
            int dstStage;
            int srcAccess = 0;
            int dstAccess = 0;

            if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
                srcAccess = 0;
                dstAccess = VK_ACCESS_TRANSFER_WRITE_BIT;
                srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                dstStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            } else if (oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
                srcAccess = VK_ACCESS_TRANSFER_WRITE_BIT;
                dstAccess = VK_ACCESS_SHADER_READ_BIT;
                srcStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
                dstStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
            } else {
                throw new RuntimeException("Unsupported layout transition");
            }

            barrier.srcAccessMask(srcAccess);
            barrier.dstAccessMask(dstAccess);

            vkCmdPipelineBarrier(commandBuffer,
                    srcStage, dstStage,
                    0,
                    null,
                    null,
                    barrier);

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

    private void copyBufferToImage(VkDevice device, VKDevice vkDevice, long buffer,
                                    long image, int width, int height) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long commandPool = vkDevice.createTransientCommandPool();

            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandPool(commandPool)
                    .commandBufferCount(1);

            PointerBuffer pCommandBuffer = stack.mallocPointer(1);
            vkAllocateCommandBuffers(device, allocInfo, pCommandBuffer);
            VkCommandBuffer commandBuffer = new VkCommandBuffer(pCommandBuffer.get(0), device);

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

            vkBeginCommandBuffer(commandBuffer, beginInfo);

            VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack)
                    .bufferOffset(0)
                    .bufferRowLength(0)
                    .bufferImageHeight(0)
                    .imageSubresource(subresource -> subresource
                            .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                            .mipLevel(0)
                            .baseArrayLayer(0)
                            .layerCount(1))
                    .imageOffset(offset -> offset.x(0).y(0).z(0))
                    .imageExtent(extent -> extent.width(width).height(height).depth(1));

            vkCmdCopyBufferToImage(commandBuffer, buffer, image,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);

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

    private long createImageView(VkDevice device, long image, int format) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .image(image)
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(format)
                    .subresourceRange(subresource -> subresource
                            .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                            .baseMipLevel(0)
                            .levelCount(1)
                            .baseArrayLayer(0)
                            .layerCount(1));

            LongBuffer pImageView = stack.mallocLong(1);
            int result = vkCreateImageView(device, viewInfo, null, pImageView);
            VKUtil.check(result, "Failed to create texture image view");
            return pImageView.get(0);
        }
    }

    private long createTextureSampler(VkDevice device) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                    .magFilter(VK_FILTER_LINEAR)
                    .minFilter(VK_FILTER_LINEAR)
                    .addressModeU(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                    .addressModeV(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                    .addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                    .anisotropyEnable(false)
                    .maxAnisotropy(1.0f)
                    .borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK)
                    .unnormalizedCoordinates(false)
                    .compareEnable(false)
                    .compareOp(VK_COMPARE_OP_ALWAYS)
                    .mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
                    .mipLodBias(0.0f)
                    .minLod(0.0f)
                    .maxLod(0.0f);

            LongBuffer pSampler = stack.mallocLong(1);
            int result = vkCreateSampler(device, samplerInfo, null, pSampler);
            VKUtil.check(result, "Failed to create texture sampler");
            return pSampler.get(0);
        }
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

    private int findMemoryType(VKDevice vkDevice, int typeFilter, int properties) {
        VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.calloc();
        vkGetPhysicalDeviceMemoryProperties(vkDevice.getPhysicalDevice(), memProperties);

        int result = -1;
        for (int i = 0; i < memProperties.memoryTypeCount(); i++) {
            if ((typeFilter & (1 << i)) != 0 &&
                    (memProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
                result = i;
                break;
            }
        }

        memProperties.free();

        if (result == -1) {
            throw new RuntimeException("Failed to find suitable memory type");
        }
        return result;
    }

    public long getImageView() {
        return textureImageView;
    }

    public long getSampler() {
        return textureSampler;
    }

    public long getImage() {
        return textureImage;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * 从指定的描述符池中为此纹理分配并写入一个描述符集。
     * 必须在命令缓冲区录制之前调用。
     */
    public void initDescriptorSet(VkDevice device, long pool, long layout) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pDescriptorSet = stack.mallocLong(1);
            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                    .descriptorPool(pool)
                    .pSetLayouts(stack.longs(layout));
            int result = vkAllocateDescriptorSets(device, allocInfo, pDescriptorSet);
            VKUtil.check(result, "Failed to allocate font descriptor set");
            this.descriptorSet = pDescriptorSet.get(0);

            VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack);
            imageInfo.get(0)
                    .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                    .imageView(textureImageView)
                    .sampler(textureSampler);

            VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(1, stack);
            descriptorWrites.get(0)
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(descriptorSet)
                    .dstBinding(0)
                    .dstArrayElement(0)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1)
                    .pImageInfo(imageInfo);

            vkUpdateDescriptorSets(device, descriptorWrites, null);
        }
    }

    public long getDescriptorSet() {
        return descriptorSet;
    }

    @Override
    public void close() {
        vkDestroySampler(device, textureSampler, null);
        vkDestroyImageView(device, textureImageView, null);
        vkDestroyImage(device, textureImage, null);
        vkFreeMemory(device, textureImageMemory, null);
    }
}