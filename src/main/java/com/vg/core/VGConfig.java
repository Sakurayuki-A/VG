package com.vg.core;

import org.lwjgl.vulkan.VK10;

public class VGConfig {

    private String applicationName = "VG Application";
    private int applicationVersion = VK10.VK_MAKE_VERSION(1, 0, 0);
    private String engineName = "VG Engine";
    private int engineVersion = VK10.VK_MAKE_VERSION(1, 0, 0);
    private int windowWidth = 800;
    private int windowHeight = 600;
    private String windowTitle = "VG Window";
    private boolean enableValidationLayers = true;
    private int vulkanApiVersion = VK10.VK_API_VERSION_1_0;
    private boolean useSdfShadows = true;  // 使用 SDF 阴影（true）或传统多层阴影（false）

    public VGConfig() {
    }

    public String getApplicationName() {
        return applicationName;
    }

    public VGConfig setApplicationName(String applicationName) {
        this.applicationName = applicationName;
        return this;
    }

    public int getApplicationVersion() {
        return applicationVersion;
    }

    public VGConfig setApplicationVersion(int applicationVersion) {
        this.applicationVersion = applicationVersion;
        return this;
    }

    public String getEngineName() {
        return engineName;
    }

    public VGConfig setEngineName(String engineName) {
        this.engineName = engineName;
        return this;
    }

    public int getEngineVersion() {
        return engineVersion;
    }

    public VGConfig setEngineVersion(int engineVersion) {
        this.engineVersion = engineVersion;
        return this;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public VGConfig setWindowWidth(int windowWidth) {
        this.windowWidth = windowWidth;
        return this;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public VGConfig setWindowHeight(int windowHeight) {
        this.windowHeight = windowHeight;
        return this;
    }

    public String getWindowTitle() {
        return windowTitle;
    }

    public VGConfig setWindowTitle(String windowTitle) {
        this.windowTitle = windowTitle;
        return this;
    }

    public boolean isEnableValidationLayers() {
        return enableValidationLayers;
    }

    public VGConfig setEnableValidationLayers(boolean enableValidationLayers) {
        this.enableValidationLayers = enableValidationLayers;
        return this;
    }

    public int getVulkanApiVersion() {
        return vulkanApiVersion;
    }

    public VGConfig setVulkanApiVersion(int vulkanApiVersion) {
        this.vulkanApiVersion = vulkanApiVersion;
        return this;
    }

    public boolean isUseSdfShadows() {
        return useSdfShadows;
    }

    public VGConfig setUseSdfShadows(boolean useSdfShadows) {
        this.useSdfShadows = useSdfShadows;
        return this;
    }
}