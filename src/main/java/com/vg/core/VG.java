package com.vg.core;

import com.vg.core.input.MouseContext;
import com.vg.core.input.KeyboardContext;
import com.vg.core.color.VGColor;
import com.vg.core.command.DrawCommand;
import com.vg.core.command.GradientRectCommand;
import com.vg.core.command.GradientRoundRectCommand;
import com.vg.core.command.IconCommand;
import com.vg.core.command.LineCommand;
import com.vg.core.command.PopClipCommand;
import com.vg.core.command.PushClipCommand;
import com.vg.core.command.RectCommand;
import com.vg.core.command.RoundRectCommand;
import com.vg.core.command.ShadowCommand;
import com.vg.core.command.RawVertexCommand;
import com.vg.core.command.TextCommand;
import com.vg.core.font.VGFont;
import com.vg.core.text.VGTextLayout;
import com.vg.core.icon.Icons;
import com.vg.core.icon.VGIconFont;
import com.vg.core.theme.VGTheme;
import com.vg.vulkan.VKFontTexture;
import com.vg.vulkan.VKRenderer;
import com.vg.window.GlfwWindow;
import com.vg.window.VGWindow;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vg.core.coord.Matrix3;

public class VG implements AutoCloseable {

    private static VG activeInstance;

    private final VGConfig config;
    private VGWindow window;
    private VKRenderer renderer;
    private boolean initialized = false;

    private final List<DrawCommand> drawCommands = new ArrayList<>();

    private VGFont defaultFont;
    private VKFontTexture defaultFontTexture;
    
    // 通用资源管理器，自动管理所有需要清理的资源
    private final ResourceManager resourceManager = new ResourceManager();
    
    // 字体缓存：支持多字体实例
    private final Map<String, VGFont> fontCache = new HashMap<>();
    private final Map<String, VKFontTexture> fontTextureCache = new HashMap<>();
    
    // 当前激活的字体（用于 text() 方法）
    private VGFont currentFont;
    private VKFontTexture currentFontTexture;

    private VGIconFont iconFont;
    private VKFontTexture iconTexture;
    private VGTheme theme = VGTheme.brand();

    // 当前图层索引，用于控制绘制顺序
    private int currentLayer = 0;
    
    // ── 2D 变换系统 ──
    // 栈顶 = 当前活跃矩阵，`pushTransform()` / `popTransform()` 管理嵌套
    private final Deque<float[]> transformStack = new ArrayDeque<>();
    { transformStack.push(Matrix3.identity()); }
    
    // ── 裁剪系统 ──
    // pushClip/popClip 通过命令流由 VKRenderer 管理
    
    // 鼠标输入上下文
    private final MouseContext mouseContext = new MouseContext();
    
    // 键盘输入上下文
    private final KeyboardContext keyboardContext = new KeyboardContext();
    
    // 渲染上下文，提供绘图工具方法
    private final RenderContext renderContext;
    
    // 渲染回调，用于每帧重新执行绘图逻辑
    private java.util.function.Consumer<RenderContext> renderCallback = null;

    public VG(VGConfig config) {
        this.config = config;
        this.renderContext = new RenderContext(this);
    }

    public void init() {
        if (initialized) {
            throw new IllegalStateException("VG is already initialized");
        }

        window = new GlfwWindow(
                config.getWindowTitle(),
                config.getWindowWidth(),
                config.getWindowHeight()
        );

        renderer = new VKRenderer(config, window, this);
        renderer.init();

        window.setMouseButtonCallback((button, action, mods) -> {
            if (button == 0) {
                mouseContext.setLeftPressed(action == 1);
            } else if (button == 1) {
                mouseContext.setRightPressed(action == 1);
            }
        });

        window.setCursorPosCallback((xPos, yPos) -> {
            // 左上角坐标系：GLFW 鼠标坐标已经是左上角，直接使用
            mouseContext.setPosition((float) xPos, (float) yPos);
        });

        window.setKeyCallback((keyCode, scancode, action, mods) -> {
            keyboardContext.onKey(keyCode, action);
        });

        window.setCharCallback((codepoint) -> {
            keyboardContext.onChar(codepoint);
        });

        window.setScrollCallback((xOffset, yOffset) -> {
            mouseContext.addScroll((float) yOffset);
        });

        activeInstance = this;
        initialized = true;
        System.out.println("[VG] Framework initialized");
    }

    public void run() {
        ensureInitialized();
        renderer.run();
    }

    public void stop() {
        ensureInitialized();
        renderer.stop();
    }

    public VGWindow getWindow() {
        ensureInitialized();
        return window;
    }

    public VKRenderer getRenderer() {
        ensureInitialized();
        return renderer;
    }

    /**
     * 获取上一帧的性能统计数据。
     * 等价于 {@code getRenderer().getStats()}.
     */
    public VKRenderer.PerformanceStats getStats() {
        return getRenderer().getStats();
    }

    public MouseContext getMouseContext() {
        return mouseContext;
    }

    public static VG getInstance() {
        return activeInstance;
    }
    
    public KeyboardContext getKeyboardContext() {
        return keyboardContext;
    }

    public VGFont getDefaultFont() {
        return defaultFont;
    }

    public VKFontTexture getDefaultFontTexture() {
        return defaultFontTexture;
    }
    
    /**
     * 获取当前激活的字体（优先于 defaultFont）
     */
    public VGFont getCurrentFont() {
        return currentFont != null ? currentFont : defaultFont;
    }
    
    /**
     * 获取当前字体纹理（优先于 defaultFontTexture）
     */
    public VKFontTexture getCurrentFontTexture() {
        return currentFontTexture != null ? currentFontTexture : defaultFontTexture;
    }

    public VKFontTexture getIconTexture() {
        return iconTexture;
    }

    /**
     * 获取指定字体对应的纹理
     */
    public VKFontTexture getFontTexture(VGFont font) {
        for (Map.Entry<String, VKFontTexture> entry : fontTextureCache.entrySet()) {
            VGFont cachedFont = fontCache.get(entry.getKey());
            if (cachedFont == font) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 遍历所有缓存的字体纹理
     */
    public void forEachFontTexture(java.util.function.Consumer<VKFontTexture> action) {
        for (VKFontTexture tex : fontTextureCache.values()) {
            action.accept(tex);
        }
    }

    /**
     * 设置当前激活的字体
     * @param font 要激活的字体
     */
    public void setFont(VGFont font) {
        if (font == null) {
            throw new IllegalArgumentException("Font cannot be null");
        }
        
        // 查找对应的纹理
        String cacheKey = null;
        for (Map.Entry<String, VGFont> entry : fontCache.entrySet()) {
            if (entry.getValue() == font) {
                cacheKey = entry.getKey();
                break;
            }
        }
        
        if (cacheKey == null) {
            throw new IllegalArgumentException("Font was not created by this VG instance");
        }
        
        this.currentFont = font;
        this.currentFontTexture = fontTextureCache.get(cacheKey);

        // 不再需要重建管线，渲染时通过 updateTexture 动态切换纹理
    }
    
    /**
     * 创建或获取缓存的字体实例
     * @param fontPath 字体文件路径
     * @param fontSize 字体大小
     * @return 字体对象
     */
    public VGFont createFont(String fontPath, float fontSize) {
        ensureInitialized();
        
        // 生成缓存键
        String cacheKey = fontPath + "@" + fontSize;
        
        // 如果已存在，直接返回
        if (fontCache.containsKey(cacheKey)) {
            System.out.println("[VG] Using cached font: " + cacheKey);
            return fontCache.get(cacheKey);
        }
        
        // 创建新字体
        VGFont font = new VGFont(fontPath, fontSize);
        VKFontTexture texture = new VKFontTexture(
                renderer.getDevice().get(),
                renderer.getDevice(),
                font.getRgbaPixels(),
                font.getAtlasWidth(),
                font.getAtlasHeight());
        
        // 注册到资源管理器
        resourceManager.register(font);
        resourceManager.register(texture);
        
        // 存入缓存
        fontCache.put(cacheKey, font);
        fontTextureCache.put(cacheKey, texture);
        
        // 自动设置为当前字体
        this.currentFont = font;
        this.currentFontTexture = texture;

        renderer.createTextPipeline(
                window.getFramebufferWidth(),
                window.getFramebufferHeight());

        System.out.println("[VG] Font created and cached: " + cacheKey);
        return font;
    }

    public VGFont loadFont(String fontPath, float fontSize) {
        ensureInitialized();
        return createFont(fontPath, fontSize);
    }

    public VGConfig getConfig() {
        return config;
    }

    /**
     * 设置当前绘制图层
     * @param layer 图层索引，数值越小越先绘制（在底层）
     *              建议：0=背景，1=面板，2=控件，3=文字，4=悬浮提示
     */
    public void setLayer(int layer) {
        this.currentLayer = layer;
    }
    
    /**
     * 获取当前绘制图层
     */
    public int getCurrentLayer() {
        return currentLayer;
    }

    /**
     * 获取当前 framebuffer 宽度
     * @return framebuffer 宽度（像素）
     */
    public static int width() {
        assertInitialized();
        return activeInstance.window.getFramebufferWidth();
    }

    /**
     * 获取当前 framebuffer 高度
     * @return framebuffer 高度（像素）
     */
    public static int height() {
        assertInitialized();
        return activeInstance.window.getFramebufferHeight();
    }

    /**
     * 计算元素水平居中时的 X 坐标
     * @param elementWidth 元素宽度
     * @return 居中的 X 坐标
     */
    public static float centerX(float elementWidth) {
        return (width() - elementWidth) / 2.0f;
    }

    /**
     * 计算元素垂直居中时的 Y 坐标
     * @param elementHeight 元素高度
     * @return 居中的 Y 坐标
     */
    public static float centerY(float elementHeight) {
        return (height() - elementHeight) / 2.0f;
    }

    /**
     * 计算面板中心位置
     * @param w 面板宽度
     * @param h 面板高度
     * @return 包含中心坐标的 float[] {x, y}
     */
    public static float[] panelCenter(float w, float h) {
        return new float[] {centerX(w), centerY(h)};
    }

    /**
     * 根据锚点计算元素位置
     * @param anchor 对齐锚点
     * @param elementWidth 元素宽度
     * @param elementHeight 元素高度
     * @return 包含坐标的 float[] {x, y}
     */
    public static float[] alignAnchor(VGAnchor anchor, float elementWidth, float elementHeight) {
        int w = width();
        int h = height();
        
        float x, y;
        switch (anchor) {
            case TOP_LEFT:
                x = 0;
                y = 0;
                break;
            case TOP_RIGHT:
                x = w - elementWidth;
                y = 0;
                break;
            case CENTER:
                x = (w - elementWidth) / 2.0f;
                y = (h - elementHeight) / 2.0f;
                break;
            case BOTTOM_LEFT:
                x = 0;
                y = h - elementHeight;
                break;
            case BOTTOM_RIGHT:
                x = w - elementWidth;
                y = h - elementHeight;
                break;
            default:
                x = 0;
                y = 0;
        }
        
        return new float[] {x, y};
    }

    /**
     * 在屏幕中心绘制矩形
     * @param w 矩形宽度
     * @param h 矩形高度
     * @param color 颜色
     */
    public static void drawCenteredRect(float w, float h, int color) {
        rect(centerX(w), centerY(h), w, h, color);
    }

    /**
     * 在屏幕中心绘制矩形（简化版）
     * @param w 矩形宽度
     * @param h 矩形高度
     */
    public static void centerRect(float w, float h) {
        rect(centerX(w), centerY(h), w, h, VGColor.WHITE);
    }

    /**
     * 在指定锚点位置绘制面板
     * @param anchor 对齐锚点
     * @param w 面板宽度
     * @param h 面板高度
     * @param color 颜色
     */
    public static void drawPanel(VGAnchor anchor, float w, float h, int color) {
        float[] pos = alignAnchor(anchor, w, h);
        rect(pos[0], pos[1], w, h, color);
    }

    /**
     * 获取按图层排序的绘制命令列表
     * @return 按 layer 字段升序排序的命令列表
     */
    public List<DrawCommand> getDrawCommands() {
        // 创建副本并排序，避免修改原始列表
        List<DrawCommand> sortedCommands = new ArrayList<>(drawCommands);
        sortedCommands.sort((a, b) -> Integer.compare(a.getLayer(), b.getLayer()));
        return sortedCommands;
    }

    public void clearDrawCommands() {
        drawCommands.clear();
    }
    
    /**
     * 获取渲染上下文
     * @return 渲染上下文实例
     */
    public RenderContext getRenderContext() {
        return renderContext;
    }
    
    /**
     * 设置渲染回调函数（新版，支持 RenderContext）
     * @param callback 每帧调用的渲染逻辑，接收 RenderContext 参数
     */
    public void setRenderCallback(java.util.function.Consumer<RenderContext> callback) {
        this.renderCallback = callback;
    }
    
    /**
     * 设置渲染回调函数（旧版，兼容代码）
     * @param callback 每帧调用的渲染逻辑
     * @deprecated 建议使用 setRenderCallback(Consumer<RenderContext>)
     */
    @Deprecated
    public void setRenderCallback(Runnable callback) {
        this.renderCallback = ctx -> callback.run();
    }
    
    /**
     * 执行渲染回调（由渲染器每帧调用）
     */
    public void executeRenderCallback() {
        if (renderCallback != null) {
            clearDrawCommands();  // 清除上一帧的命令
            renderCallback.accept(renderContext);  // 执行新的绘图逻辑
            mouseContext.endFrame();
            keyboardContext.endFrame();
        }
    }

    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("VG has not been initialized. Call init() first.");
        }
    }

    /**
     * Draws a filled rectangle on the canvas.
     *
     * @param x      The x-coordinate of the rectangle's top-left corner (pixels)
     * @param y      The y-coordinate of the rectangle's top-left corner (pixels)
     * @param width  The width of the rectangle (pixels)
     * @param height The height of the rectangle (pixels)
     * @param color  The ARGB color value (e.g., 0xFFFF0000 for red)
     */
    public static void rect(float x, float y, float width, float height, int color) {
        assertInitialized();
        RectCommand cmd = new RectCommand(x, y, width, height, color);
        cmd.setLayer(activeInstance.currentLayer);
        applyTransform(cmd);
        activeInstance.drawCommands.add(cmd);
    }

    /**
     * 批量提交预计算顶点数据的矩形（高性能路径）
     * <p>
     * 顶点格式：每 6 个 float 为一个顶点 (x, y, r, g, b, a)
     * 坐标必须已经是 NDC 坐标，颜色值范围为 0.0 ~ 1.0
     * 每 6 个顶点组成 1 个矩形（2 个三角形）。
     *
     * @param vertices 预计算的顶点数据数组
     */
    public static void batchRects(float[] vertices) {
        assertInitialized();
        RawVertexCommand cmd = new RawVertexCommand(vertices);
        cmd.setLayer(activeInstance.currentLayer);
        applyTransform(cmd);
        activeInstance.drawCommands.add(cmd);
    }

    /**
     * Draws a filled rounded rectangle on the canvas.
     *
     * @param x      The x-coordinate of the rectangle's top-left corner (pixels)
     * @param y      The y-coordinate of the rectangle's top-left corner (pixels)
     * @param width  The width of the rectangle (pixels)
     * @param height The height of the rectangle (pixels)
     * @param radius The corner radius (pixels)
     * @param color  The ARGB color value (e.g., 0xFFFF0000 for red)
     */
    public static void roundRect(float x, float y, float width, float height, float radius, int color) {
        assertInitialized();
        float r = Math.min(radius, Math.min(width * 0.5f, height * 0.5f));
        RoundRectCommand cmd = new RoundRectCommand(x, y, width, height, r, color);
        cmd.setLayer(activeInstance.currentLayer);
        applyTransform(cmd);
        activeInstance.drawCommands.add(cmd);
    }

    /**
     * Draws a line between two points on the canvas.
     *
     * @param x1        The x-coordinate of the start point (pixels)
     * @param y1        The y-coordinate of the start point (pixels)
     * @param x2        The x-coordinate of the end point (pixels)
     * @param y2        The y-coordinate of the end point (pixels)
     * @param lineWidth The width of the line (pixels)
     * @param color     The ARGB color value (e.g., 0xFFFF0000 for red)
     */
    public static void line(float x1, float y1, float x2, float y2, float lineWidth, int color) {
        assertInitialized();
        LineCommand cmd = new LineCommand(x1, y1, x2, y2, lineWidth, color);
        cmd.setLayer(activeInstance.currentLayer);
        applyTransform(cmd);
        activeInstance.drawCommands.add(cmd);
    }

    public static void gradientRect(float x, float y, float w, float h,
                                    int topLeft, int topRight, int bottomLeft, int bottomRight) {
        assertInitialized();
        GradientRectCommand cmd = new GradientRectCommand(x, y, w, h,
                topLeft, topRight, bottomLeft, bottomRight);
        cmd.setLayer(activeInstance.currentLayer);
        applyTransform(cmd);
        activeInstance.drawCommands.add(cmd);
    }

    public static void gradientRoundRect(float x, float y, float w, float h, float radius,
                                         int topLeft, int topRight, int bottomLeft, int bottomRight) {
        assertInitialized();
        float r = Math.min(radius, Math.min(w * 0.5f, h * 0.5f));
        GradientRoundRectCommand cmd = new GradientRoundRectCommand(x, y, w, h, r,
                topLeft, topRight, bottomLeft, bottomRight);
        cmd.setLayer(activeInstance.currentLayer);
        applyTransform(cmd);
        activeInstance.drawCommands.add(cmd);
    }

    public static void shadow(float x, float y, float w, float h,
                              float spread, float blur, int color) {
        assertInitialized();
        ShadowCommand cmd = new ShadowCommand(x, y, w, h, spread, blur, color);
        cmd.setLayer(activeInstance.currentLayer);
        applyTransform(cmd);
        activeInstance.drawCommands.add(cmd);
    }

    /**
     * 推入裁剪区域，与父裁剪区域求交集后生效。
     * 必须与 {@link #clipEnd()} 成对使用。
     *
     * @param x      裁剪矩形左上角 x（像素）
     * @param y      裁剪矩形左上角 y（像素）
     * @param width  裁剪矩形宽度（像素）
     * @param height 裁剪矩形高度（像素）
     */
    public static void clip(float x, float y, float width, float height) {
        assertInitialized();
        PushClipCommand cmd = new PushClipCommand(x, y, width, height);
        cmd.setLayer(activeInstance.currentLayer);
        applyTransform(cmd);
        activeInstance.drawCommands.add(cmd);
    }

    /**
     * 弹出裁剪区域，恢复父裁剪区域或全屏。
     * 必须与 {@link #clip(float, float, float, float)} 成对使用。
     */
    public static void clipEnd() {
        assertInitialized();
        PopClipCommand cmd = new PopClipCommand();
        cmd.setLayer(activeInstance.currentLayer);
        applyTransform(cmd);
        activeInstance.drawCommands.add(cmd);
    }

    // ══════════════════════════════════════════════
    // 2D 变换系统
    // ══════════════════════════════════════════════

    /**
     * 将当前变换矩阵附加到命令上。
     * 如果当前矩阵为单位矩阵则不设置（节省内存）。
     */
    private static void applyTransform(DrawCommand cmd) {
        float[] m = activeInstance.transformStack.peek();
        if (!Matrix3.isIdentity(m)) {
            cmd.matrix = m;
        }
    }

    /** 推入当前变换矩阵的副本，保存状态以便嵌套变换 */
    public static void pushTransform() {
        assertInitialized();
        activeInstance.transformStack.push(Matrix3.copy(activeInstance.transformStack.peek()));
    }

    /** 弹出上一次 pushTransform 的变换矩阵 */
    public static void popTransform() {
        assertInitialized();
        if (activeInstance.transformStack.size() <= 1) {
            throw new IllegalStateException("popTransform() without matching pushTransform()");
        }
        activeInstance.transformStack.pop();
    }

    /** 在当前变换上叠加平移：current = current * translate(tx, ty) */
    public static void translate(float tx, float ty) {
        assertInitialized();
        Matrix3.translate(activeInstance.transformStack.peek(), tx, ty);
    }

    /** 在当前变换上叠加缩放：current = current * scale(sx, sy) */
    public static void scale(float sx, float sy) {
        assertInitialized();
        Matrix3.scale(activeInstance.transformStack.peek(), sx, sy);
    }

    /** 在当前变换上叠加旋转（顺时针角度）：current = current * rotate(deg) */
    public static void rotate(float degrees) {
        assertInitialized();
        Matrix3.rotate(activeInstance.transformStack.peek(), degrees);
    }

    /**
     * Draws text on the canvas.
     *
     * @param text  The text string to render
     * @param x     The x-coordinate of the text top-left corner (pixels)
     * @param y     The y-coordinate of the text top-left corner (pixels)
     * @param color The ARGB color value (e.g., 0xFFFFFFFF for white)
     */
    public static void text(String text, float x, float y, int color) {
        assertInitialized();
        if (activeInstance.currentFont == null && activeInstance.defaultFont == null) {
            throw new IllegalStateException("No font loaded. Call VG.loadFont() or VG.createFont() and VG.setFont() first.");
        }
        TextCommand cmd = new TextCommand(text, x, y, color, activeInstance.getCurrentFont());
        cmd.setLayer(activeInstance.currentLayer);
        applyTransform(cmd);
        activeInstance.drawCommands.add(cmd);
    }

    public static void multilineText(String text, float x, float y, float maxWidth, int color) {
        assertInitialized();
        if (activeInstance.currentFont == null && activeInstance.defaultFont == null) {
            throw new IllegalStateException("No font loaded. Call VG.loadFont() or VG.createFont() and VG.setFont() first.");
        }

        float defaultLineHeight = com.vg.core.font.VGFontMetrics.lineHeight(activeInstance.getCurrentFont());
        multilineText(text, x, y, maxWidth, color, defaultLineHeight);
    }

    public static void multilineText(String text, float x, float y, float maxWidth, int color, float lineHeight) {
        assertInitialized();
        if (activeInstance.currentFont == null && activeInstance.defaultFont == null) {
            throw new IllegalStateException("No font loaded. Call VG.loadFont() or VG.createFont() and VG.setFont() first.");
        }

        VGTextLayout.TextLayoutResult result = VGTextLayout.wrap(text, maxWidth, activeInstance.getCurrentFont(), lineHeight);
        float cursorY = y;

        for (VGTextLayout.TextLine line : result.lines()) {
            VG.text(line.text(), x + line.xOffset(), cursorY, color);
            cursorY += lineHeight;
        }
    }

    public static void multilineText(String text, float x, float y, float maxWidth, int color, float lineHeight, VGTextLayout.TextAlign align) {
        assertInitialized();
        if (activeInstance.currentFont == null && activeInstance.defaultFont == null) {
            throw new IllegalStateException("No font loaded. Call VG.loadFont() or VG.createFont() and VG.setFont() first.");
        }

        VGTextLayout.TextLayoutResult result = VGTextLayout.wrap(text, maxWidth, activeInstance.getCurrentFont(), lineHeight, align);
        float cursorY = y;

        for (VGTextLayout.TextLine line : result.lines()) {
            VG.text(line.text(), x + line.xOffset(), cursorY, color);
            cursorY += lineHeight;
        }
    }

    public static void icon(Icons icon, float x, float y, float size, int color) {
        assertInitialized();
        ensureIconFont();
        IconCommand cmd = new IconCommand(icon, x, y, size, color);
        cmd.setLayer(activeInstance.currentLayer);
        applyTransform(cmd);
        activeInstance.drawCommands.add(cmd);
    }

    private static void ensureIconFont() {
        if (activeInstance.iconFont != null) return;
        createIconFont(24.0f);
    }

    /**
     * 设置图标字体大小（重新生成图集）
     * @param size 字体大小（像素）
     */
    public static void setIconFontSize(float size) {
        assertInitialized();
        // 释放旧资源
        if (activeInstance.iconFont != null) {
            activeInstance.resourceManager.unregister(activeInstance.iconFont);
            activeInstance.resourceManager.unregister(activeInstance.iconTexture);
            activeInstance.iconFont.close();
            activeInstance.iconTexture.close();
            activeInstance.iconFont = null;
            activeInstance.iconTexture = null;
        }
        createIconFont(size);
        System.out.println("[VG] Icon font resized to " + size + "px");
    }

    private static void createIconFont(float size) {
        activeInstance.iconFont = new VGIconFont("fonts/MaterialSymbolsOutlined-Regular.ttf", size);
        activeInstance.iconTexture = new VKFontTexture(
                activeInstance.renderer.getDevice().get(),
                activeInstance.renderer.getDevice(),
                activeInstance.iconFont.getRgbaPixels(),
                activeInstance.iconFont.getAtlasWidth(),
                activeInstance.iconFont.getAtlasHeight());
        activeInstance.resourceManager.register(activeInstance.iconFont);
        activeInstance.resourceManager.register(activeInstance.iconTexture);
        activeInstance.renderer.createIconPipeline(
                activeInstance.window.getFramebufferWidth(),
                activeInstance.window.getFramebufferHeight());
        if (size == 24.0f) {
            System.out.println("[VG] Icon font initialized");
        }
    }

    public static VGIconFont getIconFont() {
        assertInitialized();
        ensureIconFont();
        return activeInstance.iconFont;
    }

    public static float measureIcon(Icons icon) {
        assertInitialized();
        ensureIconFont();
        return activeInstance.iconFont.measureIcon(icon);
    }

    public static VGTheme theme() {
        assertInitialized();
        return activeInstance.theme;
    }

    public static void setTheme(VGTheme theme) {
        assertInitialized();
        activeInstance.theme = theme;
    }

    public static float measureText(String text) {
        assertInitialized();
        return activeInstance.getCurrentFont().measureTextWidth(text);
    }

    public static float measureText(String text, VGFont font) {
        assertInitialized();
        return font.measureTextWidth(text);
    }

    public static float lineHeight() {
        assertInitialized();
        return activeInstance.getCurrentFont().getLineHeight();
    }

    public static float lineHeight(VGFont font) {
        return font.getLineHeight();
    }

    public static float baselineOffset() {
        assertInitialized();
        return activeInstance.getCurrentFont().getBaselineOffset();
    }

    public static float baselineOffset(VGFont font) {
        return font.getBaselineOffset();
    }

    public static float descent() {
        assertInitialized();
        return activeInstance.getCurrentFont().getDescent();
    }

    public static float descent(VGFont font) {
        return font.getDescent();
    }

    private static void assertInitialized() {
        if (activeInstance == null) {
            throw new IllegalStateException("VG has not been initialized. Call init() first.");
        }
    }

    @Override
    public void close() {
        // 使用资源管理器统一清理所有注册的资源
        // 这包括 defaultFont 和 defaultFontTexture
        resourceManager.close();
        
        activeInstance = null;
        if (renderer != null) {
            renderer.close();
        }
        if (window != null) {
            window.close();
        }
        initialized = false;
        System.out.println("[VG] Framework shut down");
    }
}