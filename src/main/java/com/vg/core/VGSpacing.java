package com.vg.core;

/**
 * VG Spacing System - 统一的间距常量
 * <p>
 * Phase 4: 禁止魔术数字，所有间距使用此类定义的常量
 * <p>
 * 设计原则：
 * - 8px 基础网格系统 (8, 16, 24, 32...)
 * - 4px 微调网格 (4, 12, 20...)
 * - 2px 精细调整 (仅用于特殊情况)
 * <p>
 * 使用示例：
 * <pre>
 * y += VGSpacing.MD;          // ✅ 正确
 * y += VGSpacing.ICON_GAP;    // ✅ 语义化
 * y += 13;                    // ❌ 禁止魔术数字
 * </pre>
 */
public final class VGSpacing {

    private VGSpacing() {
        throw new UnsupportedOperationException("VGSpacing is a utility class and cannot be instantiated");
    }

    // ══════════════════════════════════════════════
    // 基础间距等级 (Base Spacing Scale)
    // ══════════════════════════════════════════════

    /** 零间距 (0px) */
    public static final float NONE = 0f;

    /** 微间距 (2px) - 精细调整，谨慎使用 */
    public static final float XXS = 2f;

    /** 极小间距 (4px) - 紧凑元素之间 */
    public static final float XS = 4f;

    /** 小间距 (8px) - 相关元素之间 */
    public static final float SM = 8f;

    /** 中等间距 (12px) - 标准组件内间距 */
    public static final float BASE = 12f;

    /** 标准间距 (16px) - 默认间距，最常用 */
    public static final float MD = 16f;

    /** 大间距 (24px) - 组件之间 */
    public static final float LG = 24f;

    /** 极大间距 (32px) - 区块之间 */
    public static final float XL = 32f;

    /** 超大间距 (48px) - 主要区域之间 */
    public static final float XXL = 48f;

    /** 巨大间距 (64px) - 页面级分隔 */
    public static final float XXXL = 64f;

    // ══════════════════════════════════════════════
    // 语义化间距 (Semantic Spacing)
    // ══════════════════════════════════════════════

    // ── 图标与文字 ──

    /** 图标与文字之间的间距 (6px) */
    public static final float ICON_TEXT_GAP = 6f;

    /** 图标与图标之间的间距 (8px) */
    public static final float ICON_GAP = SM;

    /** 小图标与文字的紧凑间距 (4px) */
    public static final float ICON_TEXT_COMPACT = XS;

    // ── 文本间距 ──

    /** 单词之间的间距 (4px) */
    public static final float WORD_GAP = XS;

    /** 行内元素间距 (8px) */
    public static final float INLINE_GAP = SM;

    /** 段落之间的间距 (16px) */
    public static final float PARAGRAPH_GAP = MD;

    /** 章节之间的间距 (32px) */
    public static final float SECTION_GAP = XL;

    // ── 组件间距 ──

    /** 组件内部元素间距 (8px) - 如按钮内图标和文字 */
    public static final float COMPONENT_INNER = SM;

    /** 相关组件之间的间距 (12px) - 如表单字段之间 */
    public static final float COMPONENT_GROUP = BASE;

    /** 独立组件之间的间距 (16px) */
    public static final float COMPONENT_GAP = MD;

    /** 组件与边界的间距 (16px) */
    public static final float COMPONENT_MARGIN = MD;

    // ── 布局间距 ──

    /** 卡片之间的间距 (16px) */
    public static final float CARD_GAP = MD;

    /** 卡片内部元素间距 (12px) */
    public static final float CARD_INNER = BASE;

    /** 面板之间的间距 (24px) */
    public static final float PANEL_GAP = LG;

    /** 面板内部区块间距 (16px) */
    public static final float PANEL_INNER = MD;

    /** 容器边距 (32px) */
    public static final float CONTAINER_MARGIN = XL;

    // ── 列表与网格 ──

    /** 列表项之间的紧凑间距 (4px) */
    public static final float LIST_COMPACT = XS;

    /** 列表项之间的标准间距 (8px) */
    public static final float LIST_STANDARD = SM;

    /** 列表项之间的舒适间距 (12px) */
    public static final float LIST_COMFORTABLE = BASE;

    /** 网格列之间的间距 (16px) */
    public static final float GRID_COLUMN = MD;

    /** 网格行之间的间距 (16px) */
    public static final float GRID_ROW = MD;

    // ── 按钮与输入框 ──

    /** 按钮组内按钮之间的间距 (8px) */
    public static final float BUTTON_GROUP = SM;

    /** 独立按钮之间的间距 (12px) */
    public static final float BUTTON_GAP = BASE;

    /** 表单字段之间的间距 (16px) */
    public static final float FORM_FIELD_GAP = MD;

    /** 表单区域之间的间距 (24px) */
    public static final float FORM_SECTION_GAP = LG;

    // ── 标签与内容 ──

    /** 标签与内容之间的间距 (8px) */
    public static final float LABEL_CONTENT = SM;

    /** 标签与标签之间的间距 (12px) */
    public static final float LABEL_GAP = BASE;

    // ── 头部与页脚 ──

    /** 页面头部高度相关间距 (24px) */
    public static final float HEADER_GAP = LG;

    /** 页脚间距 (24px) */
    public static final float FOOTER_GAP = LG;

    /** 标题栏间距 (16px) */
    public static final float TITLE_GAP = MD;

    // ── 弹窗与浮层 ──

    /** 弹窗边距 (24px) */
    public static final float MODAL_MARGIN = LG;

    /** 弹窗内容间距 (16px) */
    public static final float MODAL_CONTENT = MD;

    /** 提示框间距 (12px) */
    public static final float TOOLTIP_MARGIN = BASE;

    /** 下拉菜单项间距 (8px) */
    public static final float DROPDOWN_ITEM = SM;

    // ══════════════════════════════════════════════
    // 工具方法 (Utility Methods)
    // ══════════════════════════════════════════════

    /**
     * 根据数值获取最接近的标准间距
     * 用于迁移旧代码时的辅助方法
     *
     * @param value 原始间距值
     * @return 最接近的标准间距常量
     */
    public static float nearest(float value) {
        if (value <= 0f) return NONE;
        if (value <= 3f) return XXS;
        if (value <= 6f) return XS;
        if (value <= 10f) return SM;
        if (value <= 14f) return BASE;
        if (value <= 20f) return MD;
        if (value <= 28f) return LG;
        if (value <= 40f) return XL;
        if (value <= 56f) return XXL;
        return XXXL;
    }

    /**
     * 获取指定倍数的间距
     * 用于需要动态计算间距的场景
     *
     * @param multiplier 倍数
     * @return 标准间距 × 倍数
     */
    public static float multiply(float multiplier) {
        return MD * multiplier;
    }

    /**
     * 验证一个间距值是否符合设计系统
     * 用于开发时的间距检查
     *
     * @param value 要验证的间距值
     * @return 是否为标准间距
     */
    public static boolean isStandard(float value) {
        return value == NONE || value == XXS || value == XS || value == SM ||
               value == BASE || value == MD || value == LG || value == XL ||
               value == XXL || value == XXXL || value == ICON_TEXT_GAP;
    }

    /**
     * 获取间距的描述信息
     * 用于调试和日志输出
     *
     * @param value 间距值
     * @return 描述字符串
     */
    public static String describe(float value) {
        if (value == NONE) return "NONE (0px)";
        if (value == XXS) return "XXS (2px)";
        if (value == XS) return "XS (4px)";
        if (value == SM) return "SM (8px)";
        if (value == BASE) return "BASE (12px)";
        if (value == MD) return "MD (16px)";
        if (value == LG) return "LG (24px)";
        if (value == XL) return "XL (32px)";
        if (value == XXL) return "XXL (48px)";
        if (value == XXXL) return "XXXL (64px)";
        if (value == ICON_TEXT_GAP) return "ICON_TEXT_GAP (6px)";
        return String.format("CUSTOM (%.1fpx) - nearest: %s", value, describe(nearest(value)));
    }

    // ══════════════════════════════════════════════
    // 响应式间距 (Responsive Spacing)
    // ══════════════════════════════════════════════

    /**
     * 根据屏幕宽度获取响应式间距
     * 小屏幕使用较小间距，大屏幕使用较大间距
     *
     * @param screenWidth 屏幕宽度
     * @param compact     紧凑模式的间距
     * @param standard    标准模式的间距
     * @param comfortable 舒适模式的间距
     * @return 响应式间距
     */
    public static float responsive(int screenWidth, float compact, float standard, float comfortable) {
        if (screenWidth < 768) return compact;
        if (screenWidth < 1200) return standard;
        return comfortable;
    }

    /**
     * 根据容器宽度计算自适应间距
     * 容器越宽，间距越大
     *
     * @param containerWidth 容器宽度
     * @return 自适应间距
     */
    public static float adaptive(float containerWidth) {
        if (containerWidth < 300f) return SM;
        if (containerWidth < 600f) return BASE;
        if (containerWidth < 900f) return MD;
        if (containerWidth < 1200f) return LG;
        return XL;
    }
}
