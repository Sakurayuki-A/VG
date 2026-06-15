package com.vg.window;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;

public class GlfwWindow implements VGWindow {

    private final long handle;
    private final String title;
    private int width;
    private int height;
    private ResizeCallback resizeCallback;
    private KeyCallback keyCallback;
    private CharCallback charCallback;
    private MouseButtonCallback mouseButtonCallback;
    private CursorPosCallback cursorPosCallback;
    private ScrollCallback scrollCallback;

    public GlfwWindow(String title, int width, int height) {
        this.title = title;
        this.width = width;
        this.height = height;

        initGLFW();
        this.handle = createWindow();
        setupCallbacks();
    }

    private void initGLFW() {
        if (!GLFW.glfwInit()) {
            throw new RuntimeException("Failed to initialize GLFW");
        }

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        GLFW.glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
    }

    private long createWindow() {
        if (Platform.get() == Platform.MACOSX) {
            GLFW.glfwWindowHint(GLFW_COCOA_RETINA_FRAMEBUFFER, GLFW_FALSE);
        }

        long window = GLFW.glfwCreateWindow(width, height, title, 0, 0);
        if (window == 0) {
            throw new RuntimeException("Failed to create GLFW window");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            GLFW.glfwGetFramebufferSize(window, pWidth, pHeight);

            GLFWVidMode vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
            if (vidMode != null) {
                GLFW.glfwSetWindowPos(
                        window,
                        (vidMode.width() - pWidth.get(0)) / 2,
                        (vidMode.height() - pHeight.get(0)) / 2
                );
            }
        }

        return window;
    }

    private void setupCallbacks() {
        GLFW.glfwSetFramebufferSizeCallback(handle, (window, w, h) -> {
            this.width = w;
            this.height = h;
            if (resizeCallback != null) {
                resizeCallback.onResize(w, h);
            }
        });

        GLFW.glfwSetWindowSizeCallback(handle, (window, w, h) -> {
            if (resizeCallback != null) {
                resizeCallback.onResize(w, h);
            }
        });

        GLFW.glfwSetKeyCallback(handle, (window, key, scancode, action, mods) -> {
            if (keyCallback != null) {
                keyCallback.onKey(key, scancode, action, mods);
            }
        });

        GLFW.glfwSetMouseButtonCallback(handle, (window, button, action, mods) -> {
            if (mouseButtonCallback != null) {
                mouseButtonCallback.onMouseButton(button, action, mods);
            }
        });

        GLFW.glfwSetCursorPosCallback(handle, (window, xpos, ypos) -> {
            if (cursorPosCallback != null) {
                cursorPosCallback.onCursorPos(xpos, ypos);
            }
        });

        GLFW.glfwSetScrollCallback(handle, (window, xoffset, yoffset) -> {
            if (scrollCallback != null) {
                scrollCallback.onScroll(xoffset, yoffset);
            }
        });

        GLFW.glfwSetCharCallback(handle, (windowHandle, codepoint) -> {
            if (charCallback != null) {
                charCallback.onChar(codepoint);
            }
        });
    }

    @Override
    public long getHandle() {
        return handle;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getFramebufferWidth() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            GLFW.glfwGetFramebufferSize(handle, pWidth, pHeight);
            return pWidth.get(0);
        }
    }

    @Override
    public int getFramebufferHeight() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            GLFW.glfwGetFramebufferSize(handle, pWidth, pHeight);
            return pHeight.get(0);
        }
    }

    @Override
    public boolean shouldClose() {
        return GLFW.glfwWindowShouldClose(handle);
    }

    @Override
    public void pollEvents() {
        GLFW.glfwPollEvents();
    }

    @Override
    public void waitEvents() {
        GLFW.glfwWaitEvents();
    }

    @Override
    public void setTitle(String title) {
        GLFW.glfwSetWindowTitle(handle, title);
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public boolean isKeyPressed(int keyCode) {
        return GLFW.glfwGetKey(handle, keyCode) == GLFW_PRESS;
    }

    @Override
    public boolean isMouseButtonPressed(int button) {
        return GLFW.glfwGetMouseButton(handle, button) == GLFW_PRESS;
    }

    @Override
    public double getMouseX() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var x = stack.mallocDouble(1);
            var y = stack.mallocDouble(1);
            GLFW.glfwGetCursorPos(handle, x, y);
            return x.get(0);
        }
    }

    @Override
    public double getMouseY() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var x = stack.mallocDouble(1);
            var y = stack.mallocDouble(1);
            GLFW.glfwGetCursorPos(handle, x, y);
            return y.get(0);
        }
    }

    @Override
    public void setResizeCallback(ResizeCallback callback) {
        this.resizeCallback = callback;
    }

    @Override
    public void setKeyCallback(KeyCallback callback) {
        this.keyCallback = callback;
    }

    @Override
    public void setCharCallback(CharCallback callback) {
        this.charCallback = callback;
    }

    @Override
    public void setMouseButtonCallback(MouseButtonCallback callback) {
        this.mouseButtonCallback = callback;
    }

    @Override
    public void setCursorPosCallback(CursorPosCallback callback) {
        this.cursorPosCallback = callback;
    }

    @Override
    public void setScrollCallback(ScrollCallback callback) {
        this.scrollCallback = callback;
    }

    @Override
    public String[] getRequiredVulkanExtensions() {
        PointerBuffer pointerBuffer = GLFWVulkan.glfwGetRequiredInstanceExtensions();
        if (pointerBuffer == null) {
            return new String[0];
        }
        String[] extensions = new String[pointerBuffer.capacity()];
        for (int i = 0; i < pointerBuffer.capacity(); i++) {
            extensions[i] = MemoryUtil.memUTF8(pointerBuffer.get(i));
        }
        return extensions;
    }

    @Override
    public long createVulkanSurface(long vulkanInstance) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var pSurface = stack.mallocLong(1);
            GLFWVulkan.nglfwCreateWindowSurface(vulkanInstance, handle, MemoryUtil.NULL, MemoryUtil.memAddress(pSurface));
            int error = GLFW.glfwGetError(null);
            if (error != 0) {
                throw new RuntimeException("Failed to create Vulkan surface: GLFW error " + error);
            }
            return pSurface.get(0);
        }
    }

    @Override
    public void close() {
        GLFW.glfwDestroyWindow(handle);
        GLFW.glfwTerminate();
    }
}