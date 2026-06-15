package com.vg.core.component;

import com.vg.core.AnimatedState;
import com.vg.core.Interpolator;
import com.vg.core.VG;
import com.vg.core.VGMeasure;
import com.vg.core.VGSpacing;
import com.vg.core.color.VGColor;
import com.vg.core.font.VGFont;
import com.vg.core.font.VGFontMetrics;
import com.vg.core.icon.Icons;
import com.vg.core.input.MouseContext;
import com.vg.core.node.VGNode;
import com.vg.core.text.VGTextLayout;

import java.util.ArrayList;
import java.util.List;

public class VGButton extends VGNode {

    private final VGCard card;
    private String text = "";
    private int textColor = VGColor.WHITE;
    private int fontSize = 16;
    private VGTextLayout.TextAlign textAlign = VGTextLayout.TextAlign.CENTER;
    private VGFont font;
    private Icons icon;
    private int iconColor = VGColor.WHITE;
    private float iconSize = 16f;
    private static final float ICON_GAP = VGSpacing.ICON_TEXT_GAP;

    private final AnimatedState hoverState = new AnimatedState(65f, 20f);
    private final AnimatedState pressState = new AnimatedState(180f, 40f);

    private final List<Runnable> clickListeners = new ArrayList<>();
    private final List<Runnable> hoverListeners = new ArrayList<>();
    private final List<Runnable> leaveListeners = new ArrayList<>();
    private final List<Runnable> pressListeners = new ArrayList<>();
    private final List<Runnable> releaseListeners = new ArrayList<>();
    private boolean wasPressed = false;
    private boolean wasHovered = false;
    private boolean justClicked = false;

    public VGButton(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.card = new VGCard(x, y, width, height)
                .setRadius(height / 2.0f)
                .setShadow(VGSpacing.SM, VGSpacing.BASE, VGColor.rgba(0, 0, 0, 0.25f));
    }

    @Override
    public void update(float dt, MouseContext mouse) {
        if (!enabled) {
            if (wasHovered) { fireLeave(); wasHovered = false; }
            if (wasPressed) { fireRelease(); wasPressed = false; }
            hoverState.setTarget(false);
            pressState.setTarget(false);
        } else {
            syncCardPosition();
            boolean hovered = card.contains(mouse.x(), mouse.y());
            boolean pressed = hovered && mouse.leftDown();
            hoverState.setTarget(hovered);
            pressState.setTarget(pressed);

            if (hovered && !wasHovered) fireHover();
            if (!hovered && wasHovered) fireLeave();
            if (pressed && !wasPressed) firePress();
            if (!pressed && wasPressed) fireRelease();

            if (pressed && !wasPressed && hoverState.get() > 0.3f) {
                justClicked = true;
                fireClick();
            }

            wasPressed = pressed;
            wasHovered = hovered;
        }
        hoverState.update(dt);
        pressState.update(dt);
    }

    @Override
    public void render() {
        syncCardPosition();

        if (!enabled) {
            card.setBackgroundColor(VGColor.rgb(160, 160, 160));
            card.render();
            renderText(0.5f);
            return;
        }

        float hp = hoverState.get();
        float pp = pressState.get();

        float scale = Interpolator.lerp(1.0f, 1.06f, hp) * Interpolator.lerp(1.0f, 0.94f, pp);
        float offsetX = card.x() + (card.width() * (1 - scale)) / 2;
        float offsetY = card.y() + (card.height() * (1 - scale)) / 2;
        float scaledW = card.width() * scale;
        float scaledH = card.height() * scale;

        float shadowAlpha = Interpolator.lerp(0.25f, 0.55f, hp);
        float shadowSpread = Interpolator.lerp(VGSpacing.SM, VGSpacing.CARD_GAP, hp);
        float shadowBlur = Interpolator.lerp(VGSpacing.BASE, VGSpacing.LG, hp);

        VG.shadow(offsetX, offsetY, scaledW, scaledH,
                shadowSpread, shadowBlur, VGColor.rgba(0, 0, 0, shadowAlpha));

        new VGCard(offsetX, offsetY, scaledW, scaledH)
                .setRadius(card.height() / 2.0f * scale)
                .setBackgroundColor(card.backgroundColor())
                .render();

        float overlayAlpha = Interpolator.lerp(0f, 0.35f, hp) * (1 - pp);
        if (overlayAlpha > 0.005f) {
            new VGCard(offsetX, offsetY, scaledW, scaledH)
                    .setRadius(card.height() / 2.0f * scale)
                    .setBackgroundColor(VGColor.rgba(1.0f, 1.0f, 1.0f, overlayAlpha))
                    .render();
        }

        float pressDarken = pp * 0.30f;
        if (pressDarken > 0.005f) {
            new VGCard(offsetX, offsetY, scaledW, scaledH)
                    .setRadius(card.height() / 2.0f * scale)
                    .setBackgroundColor(VGColor.rgba(0.0f, 0.0f, 0.0f, pressDarken))
                    .render();
        }

        renderText(scale);
    }

    private void syncCardPosition() {
        card.setPosition(x, y);
        card.setSize(width, height);
    }

    private void renderText(float scale) {
        if ((text == null || text.isEmpty()) && icon == null) return;

        float textW = 0;
        float iconW = 0;
        if (text != null && !text.isEmpty()) {
            textW = font != null ? VG.measureText(text, font) : VG.measureText(text);
        }
        if (icon != null) {
            iconW = iconSize;
        }

        float contentW = iconW + textW;
        if (iconW > 0 && textW > 0) contentW += ICON_GAP;

        float startX = card.x() + (card.width() - contentW) / 2f;
        float textY = VGFontMetrics.centerY(card.y(), card.height(), font);

        float cursorX = startX;

        if (icon != null) {
            float iconY = VG.getIconFont().centerY(card.y(), card.height(), iconSize);
            VG.icon(icon, cursorX, iconY, iconSize, iconColor);
            cursorX += iconW + ICON_GAP;
        }

        if (text != null && !text.isEmpty()) {
            VG.text(text, cursorX, textY, textColor);
        }
    }

    public boolean isHovered() {
        return hoverState.get() > 0.5f;
    }

    public boolean isPressed() {
        return pressState.get() > 0.5f;
    }

    public boolean wasJustClicked() {
        return justClicked;
    }

    public void consumeClick() {
        justClicked = false;
    }

    public float hoverProgress() {
        return hoverState.get();
    }

    public float pressProgress() {
        return pressState.get();
    }

    private void fireClick() {
        for (Runnable listener : clickListeners) listener.run();
    }

    private void fireHover() {
        for (Runnable listener : hoverListeners) listener.run();
    }

    private void fireLeave() {
        for (Runnable listener : leaveListeners) listener.run();
    }

    private void firePress() {
        for (Runnable listener : pressListeners) listener.run();
    }

    private void fireRelease() {
        for (Runnable listener : releaseListeners) listener.run();
    }

    public VGButton setText(String text) {
        this.text = text;
        return this;
    }

    public String getText() {
        return text;
    }

    public VGButton setTextColor(int color) {
        this.textColor = color;
        return this;
    }

    public VGButton setIcon(Icons icon) {
        this.icon = icon;
        return this;
    }

    public Icons getIcon() {
        return icon;
    }

    public VGButton setIconColor(int color) {
        this.iconColor = color;
        return this;
    }

    public VGButton setIconSize(float size) {
        this.iconSize = size;
        return this;
    }

    public VGButton setFontSize(int size) {
        this.fontSize = size;
        return this;
    }

    public VGButton setTextAlign(VGTextLayout.TextAlign align) {
        this.textAlign = align;
        return this;
    }

    public VGButton setFont(VGFont font) {
        this.font = font;
        return this;
    }

    public VGButton setBackgroundColor(int color) {
        card.setBackgroundColor(color);
        return this;
    }

    public int backgroundColor() {
        return card.backgroundColor();
    }

    public VGButton setRadius(float radius) {
        card.setRadius(radius);
        return this;
    }

    public VGButton setShadow(float spread, float blur, int color) {
        card.setShadow(spread, blur, color);
        return this;
    }

    public VGButton onClick(Runnable action) {
        clickListeners.add(action);
        return this;
    }

    public VGButton onHover(Runnable action) {
        hoverListeners.add(action);
        return this;
    }

    public VGButton onLeave(Runnable action) {
        leaveListeners.add(action);
        return this;
    }

    public VGButton onPress(Runnable action) {
        pressListeners.add(action);
        return this;
    }

    public VGButton onRelease(Runnable action) {
        releaseListeners.add(action);
        return this;
    }

    @Override
    public VGButton setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public VGButton setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public VGButton setSize(float width, float height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public VGCard getCard() {
        return card;
    }
}