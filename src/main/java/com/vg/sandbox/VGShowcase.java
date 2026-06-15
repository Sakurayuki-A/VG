package com.vg.sandbox;

import com.vg.core.*;
import com.vg.core.color.VGColor;
import com.vg.core.component.VGButton;
import com.vg.core.component.VGCard;
import com.vg.core.component.VGInput;
import com.vg.core.font.VGFont;
import com.vg.core.font.VGFontMetrics;
import com.vg.core.icon.Icons;
import com.vg.core.input.MouseContext;
import com.vg.core.text.VGTextLayout;

/**
 * VG Framework Showcase - 完整功能演示
 * <p>
 * 展示所有核心功能：
 * - Phase 1-3: 坐标系统、字体度量、测量系统
 * - Phase 4-5: Spacing & Padding 系统
 * - Phase 6: Card Auto Measure
 * - Phase 7: Debug Overlay
 * - 渲染: 形状、阴影、渐变、圆角
 * - 组件: Button、Input、Card
 * - 图标: Material Symbols
 * - 文本: 单行、多行、对齐
 * - 动画: Hover & Press 效果
 */
public class VGShowcase {

    private static VG vgInstance;
    private static VGFont fontTitle;
    private static VGFont fontBody;
    private static VGFont fontSmall;

    public static void main(String[] args) {
        VGConfig config = new VGConfig()
                .setWindowTitle("VG Framework Showcase - Complete Feature Demo")
                .setWindowWidth(1600)
                .setWindowHeight(1000);

        try (VG vg = new VG(config)) {
            vgInstance = vg;
            vg.init();

            // 加载字体
            fontTitle = vg.createFont("fonts/arial.ttf", 28.0f);
            fontBody = vg.createFont("fonts/arial.ttf", 15.0f);
            fontSmall = vg.createFont("fonts/arial.ttf", 12.0f);
            vg.setFont(fontBody);

            // 启用调试覆盖层（默认关闭）
            VGDebug.enable(false);

            // ═══════════════════════════════════════
            // 创建 UI 组件
            // ═══════════════════════════════════════

            // 按钮组
            VGButton btnPrimary = new VGButton(50, 200, 160, 44)
                    .setText("Primary")
                    .setRadius(8)
                    .setBackgroundColor(VGColor.rgba(0.2f, 0.5f, 1.0f, 1.0f));

            VGButton btnSuccess = new VGButton(230, 200, 160, 44)
                    .setText("Success")
                    .setIcon(Icons.CHECK)
                    .setIconSize(20)
                    .setRadius(8)
                    .setBackgroundColor(VGColor.rgba(0.2f, 0.8f, 0.3f, 1.0f));

            VGButton btnWarning = new VGButton(410, 200, 160, 44)
                    .setText("Warning")
                    .setIcon(Icons.WARNING)
                    .setIconSize(20)
                    .setRadius(8)
                    .setBackgroundColor(VGColor.rgba(1.0f, 0.6f, 0.1f, 1.0f));

            // 输入框组
            VGInput inputEmail = new VGInput(50, 280, 280, 44)
                    .setLabel("Email")
                    .setPlaceholder("user@example.com");

            VGInput inputPassword = new VGInput(360, 280, 280, 44)
                    .setLabel("Password")
                    .setPlaceholder("Enter your password");

            // 卡片组
            VGCard card1 = VGCard.withStandardPadding(50, 360, 300, 180)
                    .setRadius(12)
                    .setBackgroundColor(VGColor.WHITE)
                    .setShadow(VGSpacing.XS, VGSpacing.MD, VGColor.rgba(0, 0, 0, 0.1f));

            VGCard card2 = VGCard.withStandardPadding(380, 360, 300, 180)
                    .setRadius(12)
                    .setBackgroundColor(VGColor.rgba(0.95f, 0.98f, 1.0f, 1.0f))
                    .setShadow(VGSpacing.XS, VGSpacing.MD, VGColor.rgba(0, 0, 0, 0.08f));

            // Auto-sized Card
            VGMeasure autoTitleMeasure = VGMeasure.measureText("Auto Card", fontBody);
            VGMeasure autoContentMeasure = VGMeasure.measureMultiline(
                    "This card automatically sizes based on content.",
                    260, fontBody, 22);

            VGCard card3 = new VGCard(710, 360, 290, 0)
                    .setRadius(12)
                    .setBackgroundColor(VGColor.rgba(1.0f, 0.98f, 0.95f, 1.0f))
                    .setPadding(VGPadding.CARD)
                    .setShadow(VGSpacing.XS, VGSpacing.MD, VGColor.rgba(0, 0, 0, 0.08f))
                    .autoHeightVertical(VGSpacing.BASE, 
                            autoTitleMeasure.height(),
                            autoContentMeasure.height());

            // 控制变量
            boolean[] showDebug = {false};
            boolean[] showGrid = {false};
            int[] selectedDemo = {0}; // 0=Overview, 1=Shapes, 2=Text, 3=Components

            vg.setRenderCallback(ctx -> {
                MouseContext mouse = ctx.mouse();
                float dt = 0.016f;

                // 背景渐变
                VG.gradientRect(0, 0, VG.width(), VG.height(),
                        VGColor.rgb(240, 245, 250),
                        VGColor.rgb(230, 240, 250),
                        VGColor.rgb(250, 250, 255),
                        VGColor.rgb(245, 248, 252));

                // ═══════════════════════════════════════
                // 顶部导航栏
                // ═══════════════════════════════════════
                renderTopBar();

                // ═══════════════════════════════════════
                // 主内容区域
                // ═══════════════════════════════════════
                renderMainContent(mouse, dt, btnPrimary, btnSuccess, btnWarning,
                        inputEmail, inputPassword, card1, card2, card3, showDebug[0]);

                // ═══════════════════════════════════════
                // 右侧信息面板
                // ═══════════════════════════════════════
                renderInfoPanel();

                // ═══════════════════════════════════════
                // 底部功能展示
                // ═══════════════════════════════════════
                renderBottomSection();

                // ═══════════════════════════════════════
                // 调试网格
                // ═══════════════════════════════════════
                if (showGrid[0]) {
                    VGDebug.grid(50);
                }

                // ═══════════════════════════════════════
                // 键盘控制
                // ═══════════════════════════════════════
                if (ctx.keyboard().isKeyPressed('D')) {
                    showDebug[0] = !showDebug[0];
                    VGDebug.enable(showDebug[0]);
                }
                if (ctx.keyboard().isKeyPressed('G')) {
                    showGrid[0] = !showGrid[0];
                }
                if (ctx.keyboard().isKeyPressed('1')) {
                    selectedDemo[0] = 0;
                }
                if (ctx.keyboard().isKeyPressed('2')) {
                    selectedDemo[0] = 1;
                }
                if (ctx.keyboard().isKeyPressed('3')) {
                    selectedDemo[0] = 2;
                }
            });

            vg.run();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void renderTopBar() {
        // 顶部栏背景
        VG.rect(0, 0, VG.width(), 80, VGColor.WHITE);
        VG.line(0, 80, VG.width(), 80, 2, VGColor.rgba(0.9f, 0.9f, 0.9f, 1.0f));

        // Logo & Title
        VG.icon(Icons.HOME, 50, 20, 40, VGColor.rgba(0.2f, 0.5f, 1.0f, 1.0f));
        
        vgInstance.setFont(fontTitle);
        VG.text("VG Framework", 105, 22, VGColor.BLACK);
        vgInstance.setFont(fontBody);
        VG.text("Complete Feature Showcase", 105, 52, VGColor.rgba(0.5f, 0.5f, 0.5f, 1.0f));

        // 右侧导航
        float navX = VG.width() - 400;
        VG.icon(Icons.SETTINGS, navX, 28, 24, VGColor.rgba(0.4f, 0.4f, 0.4f, 1.0f));
        VG.text("Settings", navX + 32, 30, VGColor.rgba(0.4f, 0.4f, 0.4f, 1.0f));

        VG.icon(Icons.SEARCH, navX + 140, 28, 24, VGColor.rgba(0.4f, 0.4f, 0.4f, 1.0f));
        VG.text("Search", navX + 172, 30, VGColor.rgba(0.4f, 0.4f, 0.4f, 1.0f));

        VG.icon(Icons.MENU, navX + 270, 28, 24, VGColor.rgba(0.4f, 0.4f, 0.4f, 1.0f));
        VG.text("Menu", navX + 302, 30, VGColor.rgba(0.4f, 0.4f, 0.4f, 1.0f));
    }

    private static void renderMainContent(MouseContext mouse, float dt,
                                           VGButton btnPrimary, VGButton btnSuccess, VGButton btnWarning,
                                           VGInput inputEmail, VGInput inputPassword,
                                           VGCard card1, VGCard card2, VGCard card3,
                                           boolean showDebug) {
        // Section title
        VG.text("Interactive Components", 50, 110, VGColor.BLACK);
        VG.text("Buttons, inputs, and cards with smooth animations", 50, 135,
                VGColor.rgba(0.5f, 0.5f, 0.5f, 1.0f));

        // 按钮组标题
        vgInstance.setFont(fontSmall);
        VG.text("BUTTONS", 50, 175, VGColor.rgba(0.6f, 0.6f, 0.6f, 1.0f));
        vgInstance.setFont(fontBody);

        // 渲染按钮
        btnPrimary.update(dt, mouse);
        btnPrimary.render();

        btnSuccess.update(dt, mouse);
        btnSuccess.render();

        btnWarning.update(dt, mouse);
        btnWarning.render();

        if (showDebug) {
            VGDebug.bounds(btnPrimary.x(), btnPrimary.y(), btnPrimary.width(), btnPrimary.height(), "Button");
            VGDebug.padding(btnPrimary.x(), btnPrimary.y(), btnPrimary.width(), btnPrimary.height(), 
                    VGPadding.BUTTON);
        }

        // 输入框组标题
        vgInstance.setFont(fontSmall);
        VG.text("INPUT FIELDS", 50, 255, VGColor.rgba(0.6f, 0.6f, 0.6f, 1.0f));
        vgInstance.setFont(fontBody);

        // 渲染输入框
        inputEmail.update(dt, mouse);
        inputEmail.render();

        inputPassword.update(dt, mouse);
        inputPassword.render();

        if (showDebug) {
            VGDebug.bounds(inputEmail.x(), inputEmail.y(), inputEmail.width(), inputEmail.height(), "Input");
            VGDebug.padding(inputEmail.x(), inputEmail.y(), inputEmail.width(), inputEmail.height(),
                    VGPadding.INPUT);
        }

        // 卡片组标题
        vgInstance.setFont(fontSmall);
        VG.text("CARDS", 50, 335, VGColor.rgba(0.6f, 0.6f, 0.6f, 1.0f));
        vgInstance.setFont(fontBody);

        // 渲染卡片
        card1.render();
        float c1x = card1.contentX();
        float c1y = card1.contentY();
        VG.icon(Icons.HOME, c1x, c1y, 32, VGColor.rgba(0.2f, 0.5f, 1.0f, 1.0f));
        VG.text("Feature Card", c1x + 40, c1y + 5, VGColor.BLACK);
        VG.text("Standard padding (16px)", c1x, c1y + 50,
                VGColor.rgba(0.5f, 0.5f, 0.5f, 1.0f));
        VG.text("With shadow and radius", c1x, c1y + 75,
                VGColor.rgba(0.5f, 0.5f, 0.5f, 1.0f));
        VG.text("✓ Phase 1-5 Complete", c1x, c1y + 110,
                VGColor.rgba(0.2f, 0.7f, 0.3f, 1.0f));

        card2.render();
        float c2x = card2.contentX();
        float c2y = card2.contentY();
        VG.icon(Icons.SETTINGS, c2x, c2y, 32, VGColor.rgba(0.6f, 0.4f, 0.9f, 1.0f));
        VG.text("Styled Card", c2x + 40, c2y + 5, VGColor.BLACK);
        VG.text("Custom background color", c2x, c2y + 50,
                VGColor.rgba(0.5f, 0.5f, 0.5f, 1.0f));
        VG.text("Gradient support ready", c2x, c2y + 75,
                VGColor.rgba(0.5f, 0.5f, 0.5f, 1.0f));
        VG.text("✓ Spacing System", c2x, c2y + 110,
                VGColor.rgba(0.2f, 0.7f, 0.3f, 1.0f));

        card3.render();
        float c3x = card3.contentX();
        float c3y = card3.contentY();
        VG.icon(Icons.CHECK, c3x, c3y, 32, VGColor.rgba(1.0f, 0.6f, 0.1f, 1.0f));
        VG.text("Auto Card", c3x + 40, c3y + 5, VGColor.BLACK);
        VG.multilineText("This card automatically sizes based on content.",
                c3x, c3y + 50, 260, VGColor.rgba(0.5f, 0.5f, 0.5f, 1.0f), 22);
        VG.text("✓ Phase 6 Auto-size", c3x, c3y + 110,
                VGColor.rgba(0.2f, 0.7f, 0.3f, 1.0f));

        if (showDebug) {
            VGDebug.card(card1.x(), card1.y(), card1.width(), card1.height(), VGPadding.CARD);
            VGDebug.card(card2.x(), card2.y(), card2.width(), card2.height(), VGPadding.CARD);
            VGDebug.card(card3.x(), card3.y(), card3.width(), card3.height(), VGPadding.CARD);
        }
    }

    private static void renderInfoPanel() {
        float panelX = VG.width() - 380;
        float panelY = 110;

        // 信息面板背景
        VG.roundRect(panelX, panelY, 350, 850, 12, VGColor.WHITE);
        VG.shadow(panelX, panelY, 350, 850, VGSpacing.XS, VGSpacing.MD,
                VGColor.rgba(0, 0, 0, 0.08f));

        float contentX = panelX + VGPadding.CARD;
        float contentY = panelY + VGPadding.CARD;

        // 标题
        VG.text("Framework Info", contentX, contentY, VGColor.BLACK);

        // 版本信息
        contentY += 35;
        vgInstance.setFont(fontSmall);
        VG.text("Version: 1.0.0", contentX, contentY, VGColor.rgba(0.5f, 0.5f, 0.5f, 1.0f));
        contentY += 20;
        VG.text("Build: Stable", contentX, contentY, VGColor.rgba(0.5f, 0.5f, 0.5f, 1.0f));
        vgInstance.setFont(fontBody);

        // 功能清单
        contentY += 35;
        VG.text("Features", contentX, contentY, VGColor.BLACK);
        
        String[] features = {
                "✓ Vulkan Rendering",
                "✓ SDF Shadows",
                "✓ Gradient Support",
                "✓ Icon System",
                "✓ Font Rendering",
                "✓ Text Layout",
                "✓ Component System",
                "✓ Animation System",
                "✓ Spacing & Padding",
                "✓ Debug Overlay",
                "✓ Auto Measure"
        };

        contentY += 30;
        vgInstance.setFont(fontSmall);
        for (String feature : features) {
            VG.text(feature, contentX, contentY, VGColor.rgba(0.2f, 0.7f, 0.3f, 1.0f));
            contentY += 22;
        }
        vgInstance.setFont(fontBody);

        // Phase 状态
        contentY += 20;
        VG.text("Phase Status", contentX, contentY, VGColor.BLACK);
        
        String[] phases = {
                "Phase 1: Coordinates ✓",
                "Phase 2: Font Metrics ✓",
                "Phase 3: Measure System ✓",
                "Phase 4: Spacing System ✓",
                "Phase 5: Padding System ✓",
                "Phase 6: Auto Measure ✓",
                "Phase 7: Debug Overlay ✓",
                "Phase 8: Validation ✓"
        };

        contentY += 30;
        vgInstance.setFont(fontSmall);
        for (String phase : phases) {
            VG.text(phase, contentX, contentY, VGColor.rgba(0.5f, 0.5f, 0.5f, 1.0f));
            contentY += 22;
        }
        vgInstance.setFont(fontBody);

        // 控制提示
        contentY += 20;
        VG.text("Controls", contentX, contentY, VGColor.BLACK);
        
        contentY += 30;
        vgInstance.setFont(fontSmall);
        VG.text("[D] Toggle Debug Overlay", contentX, contentY, VGColor.rgba(0.5f, 0.5f, 0.5f, 1.0f));
        contentY += 20;
        VG.text("[G] Toggle Grid", contentX, contentY, VGColor.rgba(0.5f, 0.5f, 0.5f, 1.0f));
        contentY += 20;
        VG.text("[ESC] Exit", contentX, contentY, VGColor.rgba(0.5f, 0.5f, 0.5f, 1.0f));
        vgInstance.setFont(fontBody);
    }

    private static void renderBottomSection() {
        float sectionY = 570;

        // 形状展示
        vgInstance.setFont(fontSmall);
        VG.text("SHAPES & EFFECTS", 50, sectionY, VGColor.rgba(0.6f, 0.6f, 0.6f, 1.0f));
        vgInstance.setFont(fontBody);

        float shapeY = sectionY + 30;

        // 基础形状
        VG.rect(50, shapeY, 80, 60, VGColor.rgba(0.2f, 0.5f, 1.0f, 1.0f));
        VG.text("Rect", 60, shapeY + 70, VGColor.rgba(0.5f, 0.5f, 0.5f, 1.0f));

        VG.roundRect(150, shapeY, 80, 60, 12, VGColor.rgba(0.2f, 0.8f, 0.3f, 1.0f));
        VG.text("Round", 155, shapeY + 70, VGColor.rgba(0.5f, 0.5f, 0.5f, 1.0f));

        // 渐变矩形
        VG.gradientRect(250, shapeY, 80, 60,
                VGColor.rgba(1.0f, 0.6f, 0.1f, 1.0f),
                VGColor.rgba(1.0f, 0.3f, 0.4f, 1.0f),
                VGColor.rgba(1.0f, 0.6f, 0.1f, 1.0f),
                VGColor.rgba(1.0f, 0.3f, 0.4f, 1.0f));
        VG.text("Gradient", 250, shapeY + 70, VGColor.rgba(0.5f, 0.5f, 0.5f, 1.0f));

        // 渐变圆角矩形
        VG.gradientRoundRect(350, shapeY, 80, 60, 12,
                VGColor.rgba(0.6f, 0.4f, 0.9f, 1.0f),
                VGColor.rgba(0.3f, 0.6f, 1.0f, 1.0f),
                VGColor.rgba(0.3f, 0.6f, 1.0f, 1.0f),
                VGColor.rgba(0.6f, 0.4f, 0.9f, 1.0f));
        VG.text("Grad+Round", 350, shapeY + 70, VGColor.rgba(0.5f, 0.5f, 0.5f, 1.0f));

        // 阴影示例
        VG.shadow(450, shapeY, 80, 60, VGSpacing.SM, VGSpacing.LG,
                VGColor.rgba(0, 0, 0, 0.3f));
        VG.rect(450, shapeY, 80, 60, VGColor.WHITE);
        VG.text("Shadow", 455, shapeY + 70, VGColor.rgba(0.5f, 0.5f, 0.5f, 1.0f));

        // 线条
        VG.line(550, shapeY, 630, shapeY + 60, 3, VGColor.rgba(0.2f, 0.5f, 1.0f, 1.0f));
        VG.text("Line", 570, shapeY + 70, VGColor.rgba(0.5f, 0.5f, 0.5f, 1.0f));

        // 图标展示
        float iconY = sectionY + 140;
        vgInstance.setFont(fontSmall);
        VG.text("ICONS", 50, iconY, VGColor.rgba(0.6f, 0.6f, 0.6f, 1.0f));
        vgInstance.setFont(fontBody);

        float iconDisplayY = iconY + 30;
        Icons[] iconList = {
                Icons.HOME, Icons.SETTINGS, Icons.SEARCH, Icons.CHECK, Icons.CLOSE,
                Icons.MENU, Icons.WARNING, Icons.ARROW_LEFT, Icons.ARROW_RIGHT
        };

        float iconX = 50;
        for (Icons icon : iconList) {
            VG.icon(icon, iconX, iconDisplayY, 32, VGColor.rgba(0.4f, 0.4f, 0.4f, 1.0f));
            iconX += 50;
        }

        // 文本对齐展示
        float textY = sectionY + 230;
        vgInstance.setFont(fontSmall);
        VG.text("TEXT ALIGNMENT", 50, textY, VGColor.rgba(0.6f, 0.6f, 0.6f, 1.0f));
        vgInstance.setFont(fontBody);

        float textBoxY = textY + 30;
        float textBoxWidth = 600;

        VG.rect(50, textBoxY, textBoxWidth, 120, VGColor.rgba(1.0f, 1.0f, 1.0f, 0.5f));

        // Left align
        VG.text("Left Aligned Text", 60, textBoxY + 15, VGColor.BLACK);

        // Center align
        String centerText = "Center Aligned Text";
        float centerWidth = VG.measureText(centerText);
        float centerX = 50 + (textBoxWidth - centerWidth) / 2;
        VG.text(centerText, centerX, textBoxY + 50, VGColor.BLACK);

        // Right align
        String rightText = "Right Aligned Text";
        float rightWidth = VG.measureText(rightText);
        float rightX = 50 + textBoxWidth - rightWidth - 10;
        VG.text(rightText, rightX, textBoxY + 85, VGColor.BLACK);

        // 底部状态栏
        VG.rect(0, VG.height() - 40, VG.width(), 40, VGColor.rgba(0.95f, 0.95f, 0.95f, 1.0f));
        VG.line(0, VG.height() - 40, VG.width(), VG.height() - 40, 1,
                VGColor.rgba(0.85f, 0.85f, 0.85f, 1.0f));
        
        vgInstance.setFont(fontSmall);
        VG.text("VG Framework v1.0.0 | All Systems Operational ✓", 50, VG.height() - 23,
                VGColor.rgba(0.5f, 0.5f, 0.5f, 1.0f));
        VG.text("Press [D] for Debug Overlay | [G] for Grid", VG.width() - 350, VG.height() - 23,
                VGColor.rgba(0.5f, 0.5f, 0.5f, 1.0f));
        vgInstance.setFont(fontBody);
    }
}
