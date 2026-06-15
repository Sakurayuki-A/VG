package com.vg.core;

/**
 * VG Padding System - 统一的内边距常量
 * <p>
 * Phase 5: 所有组件使用此类定义的 padding 常量，禁止随意设置内边距
 * <p>
 * 设计原则：
 * - 与 VGSpacing 配合使用
 * - 组件类型决定 padding 值
 * - 保持视觉一致性
 * <p>
 * 使用示例：
 * <pre>
 * VGCard card = new VGCard(x, y, w, h)
 *     .setPadding(VGPadding.CARD);    // ✅ 正确
 * 
 * VGInput input = new VGInput(x, y, w, h);
 * input.setPadding(VGPadding.INPUT);   // ✅ 正确
 * 
 * card.setPadding(13);                 // ❌ 禁止魔术数字
 * </pre>
 */
public final class VGPadding {

    private VGPadding() {
        throw new UnsupportedOperationException("VGPadding is a utility class and cannot be instantiated");
    }

    // ══════════════════════════════════════════════
    // 基础内边距等级 (Base Padding Scale)
    // ══════════════════════════════════════════════

    /** 零内边距 (0px) - 无内边距 */
    public static final float NONE = 0f;

    /** 微内边距 (4px) - 极紧凑组件 */
    public static final float TINY = 4f;

    /** 紧凑内边距 (8px) - 紧凑组件、小按钮、徽章 */
    public static final float COMPACT = 8f;

    /** 标准内边距 (12px) - 标准组件（Input、小卡片） */
    public static final float STANDARD = 12f;

    /** 舒适内边距 (16px) - 舒适组件（Card、Button、大输入框） */
    public static final float COMFORTABLE = 16f;

    /** 宽松内边距 (24px) - 宽松面板、模态框 */
    public static final float SPACIOUS = 24f;

    /** 超宽松内边距 (32px) - 页面级容器 */
    public static final float RELAXED = 32f;

    // ══════════════════════════════════════════════
    // 组件特定内边距 (Component-Specific Padding)
    // ══════════════════════════════════════════════

    // ── 按钮 (Button) ──

    /** 按钮内边距 (16px) - 标准按钮 */
    public static final float BUTTON = COMFORTABLE;

    /** 小按钮内边距 (12px) - 紧凑场景 */
    public static final float BUTTON_SMALL = STANDARD;

    /** 大按钮内边距 (24px) - 强调按钮 */
    public static final float BUTTON_LARGE = SPACIOUS;

    /** 按钮水平内边距 (20px) - 左右留更多空间 */
    public static final float BUTTON_HORIZONTAL = 20f;

    /** 按钮垂直内边距 (12px) - 上下较紧凑 */
    public static final float BUTTON_VERTICAL = STANDARD;

    /** 图标按钮内边距 (8px) - 正方形图标按钮 */
    public static final float BUTTON_ICON = COMPACT;

    // ── 输入框 (Input) ──

    /** 输入框内边距 (12px) - 标准输入框 */
    public static final float INPUT = STANDARD;

    /** 输入框水平内边距 (16px) - 左右更宽松 */
    public static final float INPUT_HORIZONTAL = COMFORTABLE;

    /** 输入框垂直内边距 (10px) - 上下稍紧 */
    public static final float INPUT_VERTICAL = 10f;

    /** 大输入框内边距 (16px) - 多行文本框 */
    public static final float INPUT_LARGE = COMFORTABLE;

    /** 小输入框内边距 (8px) - 搜索框、过滤器 */
    public static final float INPUT_SMALL = COMPACT;

    // ── 卡片 (Card) ──

    /** 卡片内边距 (16px) - 标准卡片 */
    public static final float CARD = COMFORTABLE;

    /** 小卡片内边距 (12px) - 紧凑卡片 */
    public static final float CARD_SMALL = STANDARD;

    /** 大卡片内边距 (24px) - 宽松卡片 */
    public static final float CARD_LARGE = SPACIOUS;

    /** 卡片头部内边距 (16px) - 卡片标题区域 */
    public static final float CARD_HEADER = COMFORTABLE;

    /** 卡片内容内边距 (16px) - 卡片内容区域 */
    public static final float CARD_CONTENT = COMFORTABLE;

    /** 卡片底部内边距 (12px) - 卡片底部操作区 */
    public static final float CARD_FOOTER = STANDARD;

    // ── 面板 (Panel) ──

    /** 面板内边距 (24px) - 标准面板 */
    public static final float PANEL = SPACIOUS;

    /** 侧边栏内边距 (16px) - 侧边栏面板 */
    public static final float PANEL_SIDEBAR = COMFORTABLE;

    /** 内容面板内边距 (32px) - 主内容区域 */
    public static final float PANEL_CONTENT = RELAXED;

    // ── 容器 (Container) ──

    /** 容器内边距 (24px) - 标准容器 */
    public static final float CONTAINER = SPACIOUS;

    /** 页面容器内边距 (32px) - 页面级容器 */
    public static final float CONTAINER_PAGE = RELAXED;

    /** 区域容器内边距 (16px) - 区域分隔容器 */
    public static final float CONTAINER_SECTION = COMFORTABLE;

    // ── 弹窗与浮层 (Modal & Popover) ──

    /** 模态框内边距 (24px) - 模态对话框 */
    public static final float MODAL = SPACIOUS;

    /** 模态框头部内边距 (20px) - 模态框标题 */
    public static final float MODAL_HEADER = 20f;

    /** 模态框内容内边距 (24px) - 模态框内容 */
    public static final float MODAL_CONTENT = SPACIOUS;

    /** 模态框底部内边距 (16px) - 模态框按钮区 */
    public static final float MODAL_FOOTER = COMFORTABLE;

    /** 提示框内边距 (12px) - Tooltip */
    public static final float TOOLTIP = STANDARD;

    /** 下拉菜单内边距 (8px) - Dropdown */
    public static final float DROPDOWN = COMPACT;

    /** 弹出框内边距 (16px) - Popover */
    public static final float POPOVER = COMFORTABLE;

    // ── 列表与表格 (List & Table) ──

    /** 列表项内边距 (12px) - 标准列表项 */
    public static final float LIST_ITEM = STANDARD;

    /** 紧凑列表项内边距 (8px) - 密集列表 */
    public static final float LIST_ITEM_COMPACT = COMPACT;

    /** 舒适列表项内边距 (16px) - 宽松列表 */
    public static final float LIST_ITEM_COMFORTABLE = COMFORTABLE;

    /** 表格单元格内边距 (12px) - 表格 cell */
    public static final float TABLE_CELL = STANDARD;

    /** 表格头部内边距 (16px) - 表格 header */
    public static final float TABLE_HEADER = COMFORTABLE;

    // ── 导航 (Navigation) ──

    /** 导航栏内边距 (16px) - 顶部导航 */
    public static final float NAVBAR = COMFORTABLE;

    /** 导航栏垂直内边距 (12px) - 顶部导航上下 */
    public static final float NAVBAR_VERTICAL = STANDARD;

    /** 导航栏水平内边距 (24px) - 顶部导航左右 */
    public static final float NAVBAR_HORIZONTAL = SPACIOUS;

    /** 标签页内边距 (12px) - Tab */
    public static final float TAB = STANDARD;

    /** 标签页水平内边距 (16px) - Tab 左右 */
    public static final float TAB_HORIZONTAL = COMFORTABLE;

    /** 标签页垂直内边距 (8px) - Tab 上下 */
    public static final float TAB_VERTICAL = COMPACT;

    // ── 徽章与标签 (Badge & Tag) ──

    /** 徽章内边距 (8px) - Badge */
    public static final float BADGE = COMPACT;

    /** 徽章水平内边距 (10px) - Badge 左右 */
    public static final float BADGE_HORIZONTAL = 10f;

    /** 徽章垂直内边距 (4px) - Badge 上下 */
    public static final float BADGE_VERTICAL = TINY;

    /** 标签内边距 (8px) - Tag */
    public static final float TAG = COMPACT;

    /** 标签水平内边距 (12px) - Tag 左右 */
    public static final float TAG_HORIZONTAL = STANDARD;

    /** 标签垂直内边距 (6px) - Tag 上下 */
    public static final float TAG_VERTICAL = 6f;

    // ── 表单 (Form) ──

    /** 表单字段内边距 (12px) - 表单字段容器 */
    public static final float FORM_FIELD = STANDARD;

    /** 表单区域内边距 (24px) - 表单区域分组 */
    public static final float FORM_SECTION = SPACIOUS;

    /** 表单标签内边距 (8px) - 字段标签 */
    public static final float FORM_LABEL = COMPACT;

    // ══════════════════════════════════════════════
    // 非对称内边距 (Asymmetric Padding)
    // ══════════════════════════════════════════════

    /**
     * 非对称内边距记录类
     * 用于需要不同上下左右内边距的场景
     */
    public record Asymmetric(float top, float right, float bottom, float left) {
        
        public Asymmetric {
            top = Math.max(0f, top);
            right = Math.max(0f, right);
            bottom = Math.max(0f, bottom);
            left = Math.max(0f, left);
        }

        public static Asymmetric all(float padding) {
            return new Asymmetric(padding, padding, padding, padding);
        }

        public static Asymmetric horizontal(float horizontal, float vertical) {
            return new Asymmetric(vertical, horizontal, vertical, horizontal);
        }

        public static Asymmetric vertical(float vertical, float horizontal) {
            return new Asymmetric(vertical, horizontal, vertical, horizontal);
        }

        public float horizontal() {
            return left + right;
        }

        public float vertical() {
            return top + bottom;
        }
    }

    // ══════════════════════════════════════════════
    // 工具方法 (Utility Methods)
    // ══════════════════════════════════════════════

    /**
     * 根据数值获取最接近的标准内边距
     * 用于迁移旧代码时的辅助方法
     *
     * @param value 原始内边距值
     * @return 最接近的标准内边距常量
     */
    public static float nearest(float value) {
        if (value <= 0f) return NONE;
        if (value <= 6f) return TINY;
        if (value <= 10f) return COMPACT;
        if (value <= 14f) return STANDARD;
        if (value <= 20f) return COMFORTABLE;
        if (value <= 28f) return SPACIOUS;
        return RELAXED;
    }

    /**
     * 验证一个内边距值是否符合设计系统
     * 用于开发时的内边距检查
     *
     * @param value 要验证的内边距值
     * @return 是否为标准内边距
     */
    public static boolean isStandard(float value) {
        return value == NONE || value == TINY || value == COMPACT || 
               value == STANDARD || value == COMFORTABLE || 
               value == SPACIOUS || value == RELAXED ||
               value == 10f || value == 20f || value == 6f; // 特殊值
    }

    /**
     * 获取内边距的描述信息
     * 用于调试和日志输出
     *
     * @param value 内边距值
     * @return 描述字符串
     */
    public static String describe(float value) {
        if (value == NONE) return "NONE (0px)";
        if (value == TINY) return "TINY (4px)";
        if (value == COMPACT) return "COMPACT (8px)";
        if (value == STANDARD) return "STANDARD (12px)";
        if (value == COMFORTABLE) return "COMFORTABLE (16px)";
        if (value == SPACIOUS) return "SPACIOUS (24px)";
        if (value == RELAXED) return "RELAXED (32px)";
        
        // 组件特定
        if (value == BUTTON) return "BUTTON (16px)";
        if (value == INPUT) return "INPUT (12px)";
        if (value == CARD) return "CARD (16px)";
        
        return String.format("CUSTOM (%.1fpx) - nearest: %s", value, describe(nearest(value)));
    }

    /**
     * 根据组件类型获取推荐内边距
     *
     * @param componentType 组件类型名称
     * @return 推荐的内边距值
     */
    public static float forComponent(String componentType) {
        return switch (componentType.toLowerCase()) {
            case "button" -> BUTTON;
            case "input", "textfield" -> INPUT;
            case "card" -> CARD;
            case "panel" -> PANEL;
            case "modal", "dialog" -> MODAL;
            case "list", "listitem" -> LIST_ITEM;
            case "badge" -> BADGE;
            case "tag" -> TAG;
            case "container" -> CONTAINER;
            case "tooltip" -> TOOLTIP;
            case "dropdown" -> DROPDOWN;
            default -> STANDARD;
        };
    }

    /**
     * 计算内容区域尺寸（考虑内边距）
     *
     * @param totalWidth  总宽度
     * @param totalHeight 总高度
     * @param padding     内边距
     * @return [contentWidth, contentHeight]
     */
    public static float[] contentSize(float totalWidth, float totalHeight, float padding) {
        float contentWidth = Math.max(0f, totalWidth - padding * 2);
        float contentHeight = Math.max(0f, totalHeight - padding * 2);
        return new float[]{contentWidth, contentHeight};
    }

    /**
     * 计算总尺寸（从内容尺寸 + 内边距）
     *
     * @param contentWidth  内容宽度
     * @param contentHeight 内容高度
     * @param padding       内边距
     * @return [totalWidth, totalHeight]
     */
    public static float[] totalSize(float contentWidth, float contentHeight, float padding) {
        float totalWidth = contentWidth + padding * 2;
        float totalHeight = contentHeight + padding * 2;
        return new float[]{totalWidth, totalHeight};
    }

    /**
     * 响应式内边距 - 根据容器大小调整
     *
     * @param containerSize 容器尺寸
     * @param compact       紧凑模式内边距
     * @param standard      标准模式内边距
     * @param comfortable   舒适模式内边距
     * @return 响应式内边距
     */
    public static float responsive(float containerSize, float compact, float standard, float comfortable) {
        if (containerSize < 400f) return compact;
        if (containerSize < 800f) return standard;
        return comfortable;
    }

    /**
     * 根据设备类型获取推荐内边距
     *
     * @param deviceType 设备类型 ("mobile", "tablet", "desktop")
     * @return 推荐内边距倍数
     */
    public static float forDevice(String deviceType) {
        return switch (deviceType.toLowerCase()) {
            case "mobile" -> 0.75f;   // 移动端更紧凑
            case "tablet" -> 1.0f;    // 平板标准
            case "desktop" -> 1.25f;  // 桌面更宽松
            default -> 1.0f;
        };
    }
}
