package com.vg.vulkan;

import com.vg.core.util.VKUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;

public class VKDevice implements AutoCloseable {

    private final VkPhysicalDevice physicalDevice;
    private final VkDevice device;
    private final int graphicsQueueFamily;
    private final int presentQueueFamily;
    private final VkQueue graphicsQueue;
    private final VkQueue presentQueue;
    private final VkPhysicalDeviceProperties properties;
    private final VkPhysicalDeviceMemoryProperties memoryProperties;

    public VKDevice(VkInstance vkInstance, long surface) {
        this.physicalDevice = selectPhysicalDevice(vkInstance);
        this.properties = queryPhysicalDeviceProperties(physicalDevice);
        this.memoryProperties = queryMemoryProperties(physicalDevice);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            QueueFamilyIndices indices = findQueueFamilies(physicalDevice, surface, stack);
            this.graphicsQueueFamily = indices.graphicsFamily;
            this.presentQueueFamily = indices.presentFamily;

            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = createQueueCreateInfos(indices, stack);
            String[] deviceExtensions = {VK_KHR_SWAPCHAIN_EXTENSION_NAME};

            var ppEnabledExtensionNames = stack.pointers(
                    Arrays.stream(deviceExtensions).map(stack::UTF8).toArray(java.nio.ByteBuffer[]::new)
            );

            VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack);

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                    .pQueueCreateInfos(queueCreateInfos)
                    .ppEnabledExtensionNames(ppEnabledExtensionNames)
                    .pEnabledFeatures(deviceFeatures);

            PointerBuffer pDevice = stack.mallocPointer(1);
            int result = vkCreateDevice(physicalDevice, createInfo, null, pDevice);
            VKUtil.check(result, "Failed to create logical device");

            this.device = new VkDevice(pDevice.get(0), physicalDevice, createInfo);

            PointerBuffer pQueue = stack.mallocPointer(1);
            vkGetDeviceQueue(device, graphicsQueueFamily, 0, pQueue);
            this.graphicsQueue = new VkQueue(pQueue.get(0), device);

            vkGetDeviceQueue(device, presentQueueFamily, 0, pQueue);
            this.presentQueue = new VkQueue(pQueue.get(0), device);
        }

        System.out.println("[VG] Physical device: " + properties.deviceNameString());
    }

    private VkPhysicalDevice selectPhysicalDevice(VkInstance instance) {
        int[] deviceCount = new int[1];
        vkEnumeratePhysicalDevices(instance, deviceCount, null);
        if (deviceCount[0] == 0) {
            throw new RuntimeException("No Vulkan-capable physical devices found");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer ppPhysicalDevices = stack.mallocPointer(deviceCount[0]);
            vkEnumeratePhysicalDevices(instance, deviceCount, ppPhysicalDevices);

            VkPhysicalDevice bestDevice = null;
            int bestScore = -1;

            for (int i = 0; i < deviceCount[0]; i++) {
                VkPhysicalDevice device = new VkPhysicalDevice(ppPhysicalDevices.get(i), instance);
                int score = scoreDevice(device);
                if (score > bestScore) {
                    bestScore = score;
                    bestDevice = device;
                }
            }

            if (bestDevice == null) {
                throw new RuntimeException("No suitable physical device found");
            }

            return bestDevice;
        }
    }

    private int scoreDevice(VkPhysicalDevice device) {
        VkPhysicalDeviceProperties props = queryPhysicalDeviceProperties(device);
        VkPhysicalDeviceFeatures features = queryPhysicalDeviceFeatures(device);

        int score = 0;

        if (props.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) {
            score += 1000;
        } else if (props.deviceType() == VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU) {
            score += 500;
        }

        score += (int) props.limits().maxImageDimension2D();

        if (!features.geometryShader()) {
            return -1;
        }

        return score;
    }

    private VkPhysicalDeviceProperties queryPhysicalDeviceProperties(VkPhysicalDevice device) {
        VkPhysicalDeviceProperties props = VkPhysicalDeviceProperties.calloc();
        vkGetPhysicalDeviceProperties(device, props);
        return props;
    }

    private VkPhysicalDeviceFeatures queryPhysicalDeviceFeatures(VkPhysicalDevice device) {
        VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.calloc();
        vkGetPhysicalDeviceFeatures(device, features);
        return features;
    }

    private VkPhysicalDeviceMemoryProperties queryMemoryProperties(VkPhysicalDevice device) {
        VkPhysicalDeviceMemoryProperties memProps = VkPhysicalDeviceMemoryProperties.calloc();
        vkGetPhysicalDeviceMemoryProperties(device, memProps);
        return memProps;
    }

    private QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device, long surface, MemoryStack stack) {
        int[] queueFamilyCount = new int[1];
        vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);

        VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.calloc(queueFamilyCount[0]);
        vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);

        Integer graphicsFamily = null;
        Integer presentFamily = null;

        for (int i = 0; i < queueFamilyCount[0]; i++) {
            VkQueueFamilyProperties props = queueFamilies.get(i);

            if ((props.queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
                graphicsFamily = i;
            }

            IntBuffer pPresentSupport = stack.mallocInt(1);
            KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(device, i, surface, pPresentSupport);
            if (pPresentSupport.get(0) == VK_TRUE) {
                presentFamily = i;
            }

            if (graphicsFamily != null && presentFamily != null) {
                break;
            }
        }

        queueFamilies.free();

        if (graphicsFamily == null) {
            throw new RuntimeException("No graphics queue family found");
        }
        if (presentFamily == null) {
            throw new RuntimeException("No present queue family found");
        }

        return new QueueFamilyIndices(graphicsFamily, presentFamily);
    }

    private VkDeviceQueueCreateInfo.Buffer createQueueCreateInfos(QueueFamilyIndices indices, MemoryStack stack) {
        Set<Integer> uniqueFamilies = new LinkedHashSet<>();
        uniqueFamilies.add(indices.graphicsFamily);
        uniqueFamilies.add(indices.presentFamily);

        VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(
                uniqueFamilies.size(), stack);

        FloatBuffer queuePriority = stack.floats(1.0f);

        int j = 0;
        for (int family : uniqueFamilies) {
            queueCreateInfos.get(j)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .queueFamilyIndex(family)
                    .pQueuePriorities(queuePriority);
            j++;
        }

        return queueCreateInfos;
    }

    private static class QueueFamilyIndices {
        final int graphicsFamily;
        final int presentFamily;

        QueueFamilyIndices(int graphicsFamily, int presentFamily) {
            this.graphicsFamily = graphicsFamily;
            this.presentFamily = presentFamily;
        }
    }

    public VkPhysicalDevice getPhysicalDevice() {
        return physicalDevice;
    }

    public VkDevice get() {
        return device;
    }

    public int getGraphicsQueueFamily() {
        return graphicsQueueFamily;
    }

    public int getPresentQueueFamily() {
        return presentQueueFamily;
    }

    public VkQueue getGraphicsQueue() {
        return graphicsQueue;
    }

    public VkQueue getPresentQueue() {
        return presentQueue;
    }

    public VkPhysicalDeviceProperties getProperties() {
        return properties;
    }

    public VkPhysicalDeviceMemoryProperties getMemoryProperties() {
        return memoryProperties;
    }

    public int getMaxSampleCount() {
        VkPhysicalDeviceProperties props = properties;
        VkPhysicalDeviceLimits limits = props.limits();
        int count = limits.framebufferColorSampleCounts()
                & limits.framebufferDepthSampleCounts()
                & limits.framebufferStencilSampleCounts();

        if ((count & VK_SAMPLE_COUNT_64_BIT) != 0) return 64;
        if ((count & VK_SAMPLE_COUNT_32_BIT) != 0) return 32;
        if ((count & VK_SAMPLE_COUNT_16_BIT) != 0) return 16;
        if ((count & VK_SAMPLE_COUNT_8_BIT) != 0) return 8;
        if ((count & VK_SAMPLE_COUNT_4_BIT) != 0) return 4;
        if ((count & VK_SAMPLE_COUNT_2_BIT) != 0) return 2;
        return 1;
    }

    public void waitIdle() {
        vkDeviceWaitIdle(device);
    }

    public long createTransientCommandPool() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .flags(VK_COMMAND_POOL_CREATE_TRANSIENT_BIT)
                    .queueFamilyIndex(graphicsQueueFamily);

            LongBuffer pPool = stack.mallocLong(1);
            int result = vkCreateCommandPool(device, poolInfo, null, pPool);
            VKUtil.check(result, "Failed to create transient command pool");
            return pPool.get(0);
        }
    }

    public void destroyTransientCommandPool(long pool) {
        vkDestroyCommandPool(device, pool, null);
    }

    @Override
    public void close() {
        vkDestroyDevice(device, null);
        if (properties != null) {
            properties.free();
        }
        if (memoryProperties != null) {
            memoryProperties.free();
        }
    }
}