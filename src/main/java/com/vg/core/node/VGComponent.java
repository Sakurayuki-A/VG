package com.vg.core.node;

import com.vg.core.VG;

/**
 * 交互式 UI 组件的基类。
 * <p>
 * 继承 {@link VGContainer}，焦点由 {@link com.vg.core.RenderContext} 统一管理。
 */
public class VGComponent extends VGContainer {

    protected boolean focusable = false;

    // ── Focusable ──

    public boolean isFocusable() {
        return focusable;
    }

    public VGComponent setFocusable(boolean focusable) {
        this.focusable = focusable;
        return this;
    }

    // ── Focus (委托到 RenderContext) ──

    public boolean isFocused() {
        return VG.getInstance().getRenderContext().getFocused() == this;
    }

    /**
     * 请求焦点。只有 {@link #isFocusable()} 为 true 且 {@link #isEnabled()} 且 {@link #isVisible()} 时才会成功。
     */
    public void requestFocus() {
        if (!focusable || !enabled || !visible) return;
        VG.getInstance().getRenderContext().setFocus(this);
    }

    /**
     * 失去焦点。
     */
    public void loseFocus() {
        var ctx = VG.getInstance().getRenderContext();
        if (ctx.getFocused() == this) {
            ctx.setFocus(null);
        }
    }
}