package com.vg.vulkan;

import com.vg.core.util.VKUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class VKSwapchain implements AutoCloseable {

    private final VKDevice device;
    private final long surface;
    private long swapchain;
    private long[] swapchainImages;
    private long[] swapchainImageViews;
    private final int imageFormat;
    private final VkExtent2D extent;
    private final int minImageCount;

    public VKSwapchain(VKDevice device, long surface, int initialWidth, int initialHeight) {
        this.device = device;
        this.surface = surface;

        SwapchainSupportDetails details = querySwapchainSupport(device.getPhysicalDevice(), surface);
        int format = chooseSurfaceFormat(details);
        int presentMode = choosePresentMode(details);
        VkExtent2D chosenExtent = chooseExtent(details, initialWidth, initialHeight);

        int imageCount = details.capabilities.minImageCount() + 1;
        if (details.capabilities.maxImageCount() > 0 && imageCount > details.capabilities.maxImageCount()) {
            imageCount = details.capabilities.maxImageCount();
        }

        this.minImageCount = imageCount;
        this.imageFormat = format;
        this.extent = VkExtent2D.calloc().set(chosenExtent);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                    .surface(surface)
                    .minImageCount(imageCount)
                    .imageFormat(format)
                    .imageColorSpace(VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                    .imageExtent(chosenExtent)
                    .imageArrayLayers(1)
                    .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);

            int[] queueFamilies = {device.getGraphicsQueueFamily(), device.getPresentQueueFamily()};
            if (queueFamilies[0] != queueFamilies[1]) {
                createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT)
                        .pQueueFamilyIndices(stack.ints(queueFamilies));
            } else {
                createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            }

            createInfo.preTransform(details.capabilities.currentTransform())
                    .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                    .presentMode(presentMode)
                    .clipped(true)
                    .oldSwapchain(VK_NULL_HANDLE);

            LongBuffer pSwapchain = stack.mallocLong(1);
            int result = vkCreateSwapchainKHR(device.get(), createInfo, null, pSwapchain);
            VKUtil.check(result, "Failed to create swapchain");

            swapchain = pSwapchain.get(0);

            IntBuffer pImageCount = stack.mallocInt(1);
            vkGetSwapchainImagesKHR(device.get(), swapchain, pImageCount, null);
            LongBuffer pImages = stack.mallocLong(pImageCount.get(0));
            vkGetSwapchainImagesKHR(device.get(), swapchain, pImageCount, pImages);

            swapchainImages = new long[pImageCount.get(0)];
            swapchainImageViews = new long[pImageCount.get(0)];

            for (int i = 0; i < pImageCount.get(0); i++) {
                swapchainImages[i] = pImages.get(i);
                swapchainImageViews[i] = createImageView(swapchainImages[i], format);
            }
        }
    }

    private long createImageView(long image, int format) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageViewCreateInfo createInfo = VkImageViewCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .image(image)
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(format)
                    .subresourceRange(subresourceRange -> subresourceRange
                            .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                            .baseMipLevel(0)
                            .levelCount(1)
                            .baseArrayLayer(0)
                            .layerCount(1));

            LongBuffer pImageView = stack.mallocLong(1);
            int result = vkCreateImageView(device.get(), createInfo, null, pImageView);
            VKUtil.check(result, "Failed to create image view");

            return pImageView.get(0);
        }
    }

    private SwapchainSupportDetails querySwapchainSupport(VkPhysicalDevice physicalDevice, long surface) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSurfaceCapabilitiesKHR capabilities = VkSurfaceCapabilitiesKHR.calloc();
            vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface, capabilities);

            IntBuffer formatCount = stack.mallocInt(1);
            vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, formatCount, null);
            VkSurfaceFormatKHR.Buffer formats = VkSurfaceFormatKHR.calloc(formatCount.get(0));
            vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, formatCount, formats);

            IntBuffer presentModeCount = stack.mallocInt(1);
            vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, presentModeCount, null);
            IntBuffer presentModes = stack.mallocInt(presentModeCount.get(0));
            vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, presentModeCount, presentModes);

            return new SwapchainSupportDetails(capabilities, formats, presentModes);
        }
    }

    private int chooseSurfaceFormat(SwapchainSupportDetails details) {
        VkSurfaceFormatKHR.Buffer formats = details.formats;
        for (int i = 0; i < formats.capacity(); i++) {
            VkSurfaceFormatKHR format = formats.get(i);
            if (format.format() == VK_FORMAT_B8G8R8A8_SRGB
                    && format.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
                return format.format();
            }
        }
        return formats.get(0).format();
    }

    private int choosePresentMode(SwapchainSupportDetails details) {
        IntBuffer modes = details.presentModes;
        for (int i = 0; i < modes.capacity(); i++) {
            if (modes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR) {
                return VK_PRESENT_MODE_MAILBOX_KHR;
            }
        }
        return VK_PRESENT_MODE_FIFO_KHR;
    }

    private VkExtent2D chooseExtent(SwapchainSupportDetails details, int width, int height) {
        VkExtent2D extent = VkExtent2D.calloc();
        VkSurfaceCapabilitiesKHR caps = details.capabilities;

        if (caps.currentExtent().width() != 0xFFFFFFFF) {
            extent.set(caps.currentExtent());
        } else {
            extent.width(Math.max(caps.minImageExtent().width(),
                    Math.min(caps.maxImageExtent().width(), width)));
            extent.height(Math.max(caps.minImageExtent().height(),
                    Math.min(caps.maxImageExtent().height(), height)));
        }
        return extent;
    }

    private static class SwapchainSupportDetails {
        final VkSurfaceCapabilitiesKHR capabilities;
        final VkSurfaceFormatKHR.Buffer formats;
        final IntBuffer presentModes;

        SwapchainSupportDetails(VkSurfaceCapabilitiesKHR capabilities,
                                VkSurfaceFormatKHR.Buffer formats,
                                IntBuffer presentModes) {
            this.capabilities = capabilities;
            this.formats = formats;
            this.presentModes = presentModes;
        }
    }

    public long get() {
        return swapchain;
    }

    public long[] getImages() {
        return swapchainImages;
    }

    public long[] getImageViews() {
        return swapchainImageViews;
    }

    public int getImageFormat() {
        return imageFormat;
    }

    public VkExtent2D getExtent() {
        return extent;
    }

    public int getImageCount() {
        return swapchainImages.length;
    }

    public int getMinImageCount() {
        return minImageCount;
    }

    public int acquireNextImage(long semaphore) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pImageIndex = stack.mallocInt(1);
            int result = vkAcquireNextImageKHR(
                    device.get(), swapchain, Long.MAX_VALUE, semaphore, VK_NULL_HANDLE, pImageIndex);
            if (result == VK_ERROR_OUT_OF_DATE_KHR || result == VK_SUBOPTIMAL_KHR) {
                return -1;
            }
            VKUtil.check(result, "Failed to acquire next image");
            return pImageIndex.get(0);
        }
    }

    @Override
    public void close() {
        for (long imageView : swapchainImageViews) {
            vkDestroyImageView(device.get(), imageView, null);
        }
        vkDestroySwapchainKHR(device.get(), swapchain, null);
        if (extent != null) {
            extent.free();
        }
    }
}