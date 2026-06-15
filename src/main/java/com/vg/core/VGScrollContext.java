package com.vg.core;

import com.vg.core.input.MouseContext;

/**
 * 2D 滚动上下文 —— Clip + Transform 的组合实现。
 * <p>
 * 用法：
 * <pre>
 * VGScrollContext scroll = new VGScrollContext(x, y, w, h, contentHeight);
 *
 * // 每帧
 * scroll.handleScroll(mouse);
 * scroll.begin();
 * // ... 绘制内容（自动裁剪 + 偏移）
 * scroll.end();
 * </pre>
 */
public class VGScrollContext {

    private final float x, y;
    private final float viewportWidth;
    private float viewportHeight;
    private float contentHeight;
    private float scrollY;
    private float targetScrollY;

    private static final float SCROLL_SPEED = 12f;
    private static final float SMOOTH_DAMPING = 8f;

    /**
     * @param x              视口左上角 x
     * @param y              视口左上角 y
     * @param viewportWidth  视口宽度
     * @param viewportHeight 视口高度
     * @param contentHeight  内容总高度
     */
    public VGScrollContext(float x, float y, float viewportWidth, float viewportHeight, float contentHeight) {
        this.x = x;
        this.y = y;
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.contentHeight = contentHeight;
        this.scrollY = 0f;
        this.targetScrollY = 0f;
    }

    // ── 公共 API ──

    /** 应用裁剪 + 平移变换。必须与 {@link #end()} 成对使用。 */
    public void begin() {
        VG.clip(x, y, viewportWidth, viewportHeight);
        VG.pushTransform();
        VG.translate(0f, -scrollY);
    }

    /** 恢复裁剪 + 变换。必须与 {@link #begin()} 成对使用。 */
    public void end() {
        VG.popTransform();
        VG.clipEnd();
    }

    /**
     * 处理鼠标滚轮滚动。
     * 每帧在 {@link #begin()} 之前调用。
     */
    public void handleScroll(MouseContext mouse) {
        float delta = mouse.scrollY();
        if (delta != 0f) {
            targetScrollY -= delta * SCROLL_SPEED;
            clampTarget();
        }
    }

    /**
     * 更新平滑滚动插值。
     * 每帧在 {@link #begin()} 之前调用。
     *
     * @param dt 帧时间差（秒）
     */
    public void update(float dt) {
        float diff = targetScrollY - scrollY;
        if (Math.abs(diff) > 0.5f) {
            scrollY += diff * Math.min(1f, SMOOTH_DAMPING * dt);
        } else {
            scrollY = targetScrollY;
        }
    }

    // ── 属性 ──

    public float getScrollY() {
        return scrollY;
    }

    public void setScrollY(float scrollY) {
        this.targetScrollY = scrollY;
        clampTarget();
    }

    public float getContentHeight() {
        return contentHeight;
    }

    public void setContentHeight(float contentHeight) {
        this.contentHeight = contentHeight;
        clampTarget();
    }

    public float getViewportHeight() {
        return viewportHeight;
    }

    public void setViewportHeight(float viewportHeight) {
        this.viewportHeight = viewportHeight;
        clampTarget();
    }

    /** 最大可滚动距离 = max(0, content - viewport) */
    public float getMaxScroll() {
        return Math.max(0f, contentHeight - viewportHeight);
    }

    /** 滚动进度 0~1 */
    public float getNormalizedScroll() {
        float max = getMaxScroll();
        return max > 0f ? scrollY / max : 0f;
    }

    // ── 内部 ──

    private void clampTarget() {
        targetScrollY = Math.max(0f, Math.min(targetScrollY, getMaxScroll()));
    }
}