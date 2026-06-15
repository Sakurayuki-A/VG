package com.vg.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class VKShaderModule implements AutoCloseable {

    private final VkDevice device;
    private final long shaderModule;

    public VKShaderModule(VkDevice device, String resourcePath) {
        this.device = device;

        byte[] spirvCode = loadResource(resourcePath);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer code = stack.malloc(spirvCode.length);
            code.put(spirvCode).flip();

            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                    .pCode(code);

            LongBuffer pShaderModule = stack.mallocLong(1);
            int result = vkCreateShaderModule(device, createInfo, null, pShaderModule);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create shader module from " + resourcePath + ": " + result);
            }
            this.shaderModule = pShaderModule.get(0);
        }
    }

    private byte[] loadResource(String path) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Shader resource not found: " + path);
            }
            return is.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load shader resource: " + path, e);
        }
    }

    public long get() {
        return shaderModule;
    }

    @Override
    public void close() {
        vkDestroyShaderModule(device, shaderModule, null);
    }
}
