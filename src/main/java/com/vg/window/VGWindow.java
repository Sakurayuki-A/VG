package com.vg.window;

public interface VGWindow extends AutoCloseable {

    long getHandle();

    int getWidth();

    int getHeight();

    int getFramebufferWidth();

    int getFramebufferHeight();

    boolean shouldClose();

    void pollEvents();

    void waitEvents();

    void setTitle(String title);

    String getTitle();

    boolean isKeyPressed(int keyCode);

    boolean isMouseButtonPressed(int button);

    double getMouseX();

    double getMouseY();

    void setResizeCallback(ResizeCallback callback);

    void setKeyCallback(KeyCallback callback);

    void setCharCallback(CharCallback callback);

    void setMouseButtonCallback(MouseButtonCallback callback);

    void setCursorPosCallback(CursorPosCallback callback);

    void setScrollCallback(ScrollCallback callback);

    String[] getRequiredVulkanExtensions();

    long createVulkanSurface(long vulkanInstance);

    @Override
    void close();

    @FunctionalInterface
    interface ResizeCallback {
        void onResize(int width, int height);
    }

    @FunctionalInterface
    interface KeyCallback {
        void onKey(int keyCode, int scancode, int action, int mods);
    }

    @FunctionalInterface
    interface CharCallback {
        void onChar(int codepoint);
    }

    @FunctionalInterface
    interface MouseButtonCallback {
        void onMouseButton(int button, int action, int mods);
    }

    @FunctionalInterface
    interface CursorPosCallback {
        void onCursorPos(double xPos, double yPos);
    }

    @FunctionalInterface
    interface ScrollCallback {
        void onScroll(double xOffset, double yOffset);
    }
}