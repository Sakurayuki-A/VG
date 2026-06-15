package com.vg.core;

import com.vg.core.color.VGColor;
import com.vg.core.input.KeyboardContext;
import com.vg.core.input.MouseContext;
import com.vg.core.node.VGNode;

/**
 * 渲染上下文，提供绘图相关的状态和工具方法。
 * <p>
 * 同时管理全局焦点状态。
 */
public class RenderContext {

    private final VG vg;

    // ── 全局焦点 ──

    private VGNode focused = null;

    public RenderContext(VG vg) {
        this.vg = vg;
    }

    /**
     * 设置焦点到指定节点。传入 {@code null} 可清除焦点。
     */
    public void setFocus(VGNode node) {
        if (focused == node) return;
        if (focused != null) {
            focused.loseFocus();
        }
        focused = node;
        if (focused != null) {
            focused.requestFocus();
        }
    }

    /**
     * 获取当前拥有焦点的节点。
     */
    public VGNode getFocused() {
        return focused;
    }
    
    /**
     * 获取当前 framebuffer 宽度
     * @return framebuffer 宽度（像素）
     */
    public int width() {
        return vg.getWindow().getFramebufferWidth();
    }
    
    /**
     * 获取当前 framebuffer 高度
     * @return framebuffer 高度（像素）
     */
    public int height() {
        return vg.getWindow().getFramebufferHeight();
    }
    
    /**
     * 计算元素水平居中时的 X 坐标
     * @param elementWidth 元素宽度
     * @return 居中的 X 坐标
     */
    public float centerX(float elementWidth) {
        return (width() - elementWidth) / 2.0f;
    }
    
    /**
     * 计算元素垂直居中时的 Y 坐标
     * @param elementHeight 元素高度
     * @return 居中的 Y 坐标
     */
    public float centerY(float elementHeight) {
        return (height() - elementHeight) / 2.0f;
    }
    
    /**
     * 计算元素居中位置
     * @param w 元素宽度
     * @param h 元素高度
     * @return 居中位置对象
     */
    public VGPosition center(float w, float h) {
        return new VGPosition(centerX(w), centerY(h));
    }
    
    /**
     * 创建锚点定位器
     * @param anchor 对齐锚点
     * @return 锚点定位器对象
     */
    public AnchorPosition anchor(Anchor anchor) {
        return new AnchorPosition(this, anchor);
    }
    
    public MouseContext mouse() {
        return vg.getMouseContext();
    }
    
    public KeyboardContext keyboard() {
        return vg.getKeyboardContext();
    }
    
    /**
     * 根据锚点计算元素位置（内部方法）
     * @param anchor 对齐锚点
     * @param w 元素宽度
     * @param h 元素高度
     * @return 位置对象
     */
    VGPosition align(Anchor anchor, float w, float h) {
        int width = width();
        int height = height();
        
        float x, y;
        switch (anchor) {
            case TOP_LEFT:
                x = 0;
                y = 0;
                break;
            case TOP_CENTER:
                x = (width - w) / 2.0f;
                y = 0;
                break;
            case TOP_RIGHT:
                x = width - w;
                y = 0;
                break;
            case CENTER_LEFT:
                x = 0;
                y = (height - h) / 2.0f;
                break;
            case CENTER:
                x = (width - w) / 2.0f;
                y = (height - h) / 2.0f;
                break;
            case CENTER_RIGHT:
                x = width - w;
                y = (height - h) / 2.0f;
                break;
            case BOTTOM_LEFT:
                x = 0;
                y = height - h;
                break;
            case BOTTOM_CENTER:
                x = (width - w) / 2.0f;
                y = height - h;
                break;
            case BOTTOM_RIGHT:
                x = width - w;
                y = height - h;
                break;
            default:
                x = 0;
                y = 0;
        }
        
        return new VGPosition(x, y);
    }
}
