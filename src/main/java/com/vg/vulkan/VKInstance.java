package com.vg.vulkan;

import com.vg.core.VGConfig;
import com.vg.core.util.VKUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;

import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class VKInstance implements AutoCloseable {

    private final VkInstance instance;
    private final long debugMessenger;
    private final boolean validationEnabled;

    public VKInstance(VGConfig config, String[] requiredExtensions) {
        this.validationEnabled = config.isEnableValidationLayers();
        this.instance = createInstance(config, requiredExtensions);
        this.debugMessenger = validationEnabled ? setupDebugMessenger(instance) : VK_NULL_HANDLE;
    }

    private VkInstance createInstance(VGConfig config, String[] requiredExtensions) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                    .pApplicationName(stack.UTF8(config.getApplicationName()))
                    .applicationVersion(config.getApplicationVersion())
                    .pEngineName(stack.UTF8(config.getEngineName()))
                    .engineVersion(config.getEngineVersion())
                    .apiVersion(config.getVulkanApiVersion());

            Set<String> extensionSet = new LinkedHashSet<>(Arrays.asList(requiredExtensions));
            if (validationEnabled) {
                extensionSet.add(VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
            }

            String[] extensions = extensionSet.toArray(new String[0]);
            var ppEnabledExtensionNames = stack.pointers(
                    Arrays.stream(extensions).map(stack::UTF8).toArray(java.nio.ByteBuffer[]::new)
            );

            List<String> enabledLayers = new ArrayList<>();
            if (validationEnabled) {
                String[] validationLayers = {"VK_LAYER_KHRONOS_validation"};
                String[] availableLayers = getAvailableLayers();
                for (String layer : validationLayers) {
                    if (containsLayer(availableLayers, layer)) {
                        enabledLayers.add(layer);
                    } else {
                        System.err.println("[VG] WARNING: Validation layer not available: " + layer);
                    }
                }
            }

            var ppEnabledLayerNames = stack.pointers(
                    enabledLayers.stream().map(stack::UTF8).toArray(java.nio.ByteBuffer[]::new)
            );

            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                    .pApplicationInfo(appInfo)
                    .ppEnabledExtensionNames(ppEnabledExtensionNames)
                    .ppEnabledLayerNames(ppEnabledLayerNames);

            if (validationEnabled && !enabledLayers.isEmpty()) {
                VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = createDebugMessengerCreateInfo(stack);
                createInfo.pNext(debugCreateInfo.address());
            }

            PointerBuffer pInstance = stack.mallocPointer(1);
            int result = vkCreateInstance(createInfo, null, pInstance);
            VKUtil.check(result, "Failed to create Vulkan instance");

            long instanceHandle = pInstance.get(0);
            return new VkInstance(instanceHandle, createInfo);
        }
    }

    private VkDebugUtilsMessengerCreateInfoEXT createDebugMessengerCreateInfo(MemoryStack stack) {
        return VkDebugUtilsMessengerCreateInfoEXT.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                .messageSeverity(
                        VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT |
                                VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
                                VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
                )
                .messageType(
                        VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                                VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                                VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
                )
                .pfnUserCallback((messageSeverity, messageTypes, pCallbackData, pUserData) -> {
                    VkDebugUtilsMessengerCallbackDataEXT callbackData =
                            VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
                    System.err.println("[Vulkan] " + callbackData.pMessageString());
                    return VK_FALSE;
                });
    }

    private long setupDebugMessenger(VkInstance instance) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDebugUtilsMessengerCreateInfoEXT createInfo = createDebugMessengerCreateInfo(stack);
            LongBuffer pMessenger = stack.mallocLong(1);
            int result = vkCreateDebugUtilsMessengerEXT(instance, createInfo, null, pMessenger);
            if (result != VK_SUCCESS) {
                System.err.println("[VG] WARNING: Failed to set up debug messenger");
                return VK_NULL_HANDLE;
            }
            return pMessenger.get(0);
        }
    }

    private String[] getAvailableLayers() {
        int[] layerCount = new int[1];
        vkEnumerateInstanceLayerProperties(layerCount, null);

        VkLayerProperties.Buffer layers = VkLayerProperties.calloc(layerCount[0]);
        vkEnumerateInstanceLayerProperties(layerCount, layers);

        String[] layerNames = new String[layerCount[0]];
        for (int i = 0; i < layerCount[0]; i++) {
            layerNames[i] = layers.get(i).layerNameString();
        }
        layers.free();
        return layerNames;
    }

    private boolean containsLayer(String[] available, String layer) {
        for (String s : available) {
            if (s.equals(layer)) {
                return true;
            }
        }
        return false;
    }

    public VkInstance get() {
        return instance;
    }

    public long address() {
        return instance.address();
    }

    public boolean isValidationEnabled() {
        return validationEnabled;
    }

    @Override
    public void close() {
        if (debugMessenger != VK_NULL_HANDLE) {
            vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, null);
        }
        vkDestroyInstance(instance, null);
    }
}