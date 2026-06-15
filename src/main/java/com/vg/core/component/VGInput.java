package com.vg.core.component;

import com.vg.core.AnimatedState;
import com.vg.core.Interpolator;
import com.vg.core.VG;
import com.vg.core.VGMeasure;
import com.vg.core.VGPadding;
import com.vg.core.VGSpacing;
import com.vg.core.color.VGColor;
import com.vg.core.font.VGFont;
import com.vg.core.font.VGFontMetrics;
import com.vg.core.icon.Icons;
import com.vg.core.input.MouseContext;
import com.vg.core.node.VGNode;

public class VGInput extends VGNode {

    private final VGCard card;
    private String text = "";
    private String placeholder = "";
    private int placeholderColor = VGColor.rgb(160, 160, 170);

    private int textColor = VGColor.rgb(32, 33, 36);
    private int fontSize = 16;
    private float paddingLeft = VGPadding.INPUT;
    private float paddingRight = VGPadding.INPUT;

    private boolean passwordMode = false;
    private char passwordChar = '\u2022';

    private int maxLength = -1;

    private int cursorPosition = 0;
    private float cursorBlinkTime = 0f;
    private final float CURSOR_BLINK_INTERVAL = 0.53f;
    private boolean cursorVisible = true;

    private int selectionStart = -1;
    private int selectionEnd = -1;

    private boolean errorState = false;
    private String errorMessage = "";

    private final AnimatedState focusState = new AnimatedState(80f, 18f);
    private final AnimatedState hoverState = new AnimatedState(65f, 20f);

    private final java.util.List<Runnable> focusListeners = new java.util.ArrayList<>();
    private final java.util.List<Runnable> blurListeners = new java.util.ArrayList<>();
    private final java.util.List<java.util.function.Consumer<String>> changeListeners = new java.util.ArrayList<>();
    private final java.util.List<java.util.function.Consumer<String>> submitListeners = new java.util.ArrayList<>();
    private final java.util.function.Consumer<String> onTextChanged = null;

    private VGFont font;
    private Icons leftIcon;
    private Icons rightIcon;
    private float iconSize = 18f;
    private int iconColor = VGColor.rgb(140, 140, 150);
    private static final float ICON_GAP = VGSpacing.SM;

    private String label = "";
    private int labelColor;
    private float labelFontSize = 13;

    public VGInput(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.card = new VGCard(x, y, width, height)
                .setRadius(10f)
                .setBackgroundColor(VGColor.WHITE);
        this.labelColor = placeholderColor;
    }

    @Override
    public void update(float dt, MouseContext mouse) {
        var keyboard = VG.getInstance().getKeyboardContext();
        var ctx = VG.getInstance().getRenderContext();
        boolean focused = ctx.getFocused() == this;

        if (!enabled) {
            if (focused) { ctx.setFocus(null); fireBlur(); }
            focusState.setTarget(false);
            hoverState.setTarget(false);
            focusState.update(dt);
            hoverState.update(dt);
            return;
        }

        syncCardPosition();
        boolean hovered = card.contains(mouse.x(), mouse.y());
        hoverState.setTarget(hovered);

        if (mouse.leftPressed() && hovered) {
            if (!focused) { ctx.setFocus(this); fireFocus(); }
            cursorPosition = getCursorPositionFromMouse(mouse.x());
            clearSelection();
        } else if (mouse.leftPressed() && !hovered && focused) {
            ctx.setFocus(null);
            fireBlur();
        }

        focused = ctx.getFocused() == this;
        focusState.setTarget(focused);

        if (focused) {
            handleKeyboardInput(keyboard);
            cursorBlinkTime += dt;
            if (cursorBlinkTime >= CURSOR_BLINK_INTERVAL) {
                cursorBlinkTime = 0f;
                cursorVisible = !cursorVisible;
            }
        } else {
            cursorBlinkTime = 0f;
            cursorVisible = false;
        }

        focusState.update(dt);
        hoverState.update(dt);
    }

    private void handleKeyboardInput(com.vg.core.input.KeyboardContext kb) {
        if (kb.isBackspace()) {
            handleBackspace();
        } else if (kb.isDelete()) {
            handleDelete();
        } else if (kb.isLeft()) {
            handleCursorLeft(kb);
        } else if (kb.isRight()) {
            handleCursorRight(kb);
        } else if (kb.isHome()) {
            cursorPosition = 0;
            clearSelection();
        } else if (kb.isEnd()) {
            cursorPosition = text.length();
            clearSelection();
        } else if (kb.isEnter()) {
            fireSubmit(text);
        } else if (kb.isEscape()) {
            VG.getInstance().getRenderContext().setFocus(null);
            fireBlur();
        } else if (kb.hasChars()) {
            for (int codepoint : kb.getCharBuffer()) {
                if (isValidCharacter(codepoint)) {
                    insertCharacter((char) codepoint);
                }
            }
            kb.clearChars();
        }
    }

    private boolean isValidCharacter(int codepoint) {
        char c = (char) codepoint;
        if (maxLength > 0 && text.length() >= maxLength) return false;
        return c >= 32 && c != 127;
    }

    private void insertCharacter(char c) {
        if (hasSelection()) {
            deleteSelection();
        }
        text = new StringBuilder(text).insert(cursorPosition, c).toString();
        cursorPosition++;
        fireChange(text);
    }

    private void handleBackspace() {
        if (hasSelection()) {
            deleteSelection();
            return;
        }
        if (cursorPosition > 0) {
            text = new StringBuilder(text).deleteCharAt(cursorPosition - 1).toString();
            cursorPosition--;
            fireChange(text);
        }
    }

    private void handleDelete() {
        if (hasSelection()) {
            deleteSelection();
            return;
        }
        if (cursorPosition < text.length()) {
            text = new StringBuilder(text).deleteCharAt(cursorPosition).toString();
            fireChange(text);
        }
    }

    private void handleCursorLeft(com.vg.core.input.KeyboardContext kb) {
        if (kb.isShiftDown()) {
            if (selectionStart == -1) {
                selectionStart = cursorPosition;
                selectionEnd = cursorPosition;
            }
            if (cursorPosition > 0) {
                cursorPosition--;
                selectionEnd = cursorPosition;
            }
        } else {
            if (cursorPosition > 0) cursorPosition--;
            clearSelection();
        }
    }

    private void handleCursorRight(com.vg.core.input.KeyboardContext kb) {
        if (kb.isShiftDown()) {
            if (selectionStart == -1) {
                selectionStart = cursorPosition;
                selectionEnd = cursorPosition;
            }
            if (cursorPosition < text.length()) {
                cursorPosition++;
                selectionEnd = cursorPosition;
            }
        } else {
            if (cursorPosition < text.length()) cursorPosition++;
            clearSelection();
        }
    }

    private void deleteSelection() {
        if (!hasSelection()) return;
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        text = new StringBuilder(text).delete(start, end).toString();
        cursorPosition = start;
        clearSelection();
        fireChange(text);
    }

    private boolean hasSelection() {
        return selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd;
    }

    private void clearSelection() {
        selectionStart = -1;
        selectionEnd = -1;
    }

    private int getCursorPositionFromMouse(float mouseX) {
        String displayText = passwordMode ? maskText(text) : text;
        float startX = getTextStartX();
        float previousWidth = 0f;

        for (int i = 0; i < displayText.length(); i++) {
            float nextWidth = measureTextWidth(displayText.substring(0, i + 1));
            float midpoint = startX + previousWidth + (nextWidth - previousWidth) / 2f;
            if (mouseX <= midpoint) {
                return i;
            }
            previousWidth = nextWidth;
        }
        return displayText.length();
    }

    private float measureTextWidth(String str) {
        return VGMeasure.measureText(str, font).width();
    }

    private String maskText(String original) {
        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < original.length(); i++) {
            masked.append(passwordChar);
        }
        return masked.toString();
    }

    @Override
    public void render() {
        syncCardPosition();
        renderBackground();
        renderIcons();
        renderTextAndCursor();
        renderLabel();
    }

    private void syncCardPosition() {
        card.setPosition(x, y);
        card.setSize(width, height);
    }

    private void renderBackground() {
        float fp = focusState.get();
        float hp = hoverState.get();

        int bgColor = card.backgroundColor();
        if (errorState && !isFocused()) {
            bgColor = blendColors(bgColor, VGColor.rgba(255, 0, 0, 0.05f));
        }

        float shadowAlpha = Interpolator.lerp(0.08f, 0.20f, fp);
        VG.shadow(card.x(), card.y(), card.width(), card.height(),
                VGSpacing.ICON_TEXT_GAP, VGSpacing.BASE, VGColor.rgba(0, 0, 0, shadowAlpha));

        new VGCard(card.x(), card.y(), card.width(), card.height())
                .setRadius(card.getRadius())
                .setBackgroundColor(bgColor)
                .render();

        if (isFocused() || errorState || hp > 0.01f) {
            int borderColor = errorState ? VGColor.rgb(220, 53, 69) :
                             isFocused() ? VG.theme().primary() :
                             VGColor.rgb(200, 200, 210);
            float borderAlpha = errorState ? 1.0f :
                              isFocused() ? Interpolator.lerp(0.5f, 1.0f, fp) :
                              Interpolator.lerp(0.3f, 0.8f, hp);

            renderBorder(card.x(), card.y(), card.width(), card.height(),
                       card.getRadius(), borderColor, borderAlpha, bgColor);
        }

        if (hp > 0.01f && !isFocused() && !errorState) {
            float overlayAlpha = Interpolator.lerp(0f, 0.03f, hp);
            new VGCard(card.x(), card.y(), card.width(), card.height())
                    .setRadius(card.getRadius())
                    .setBackgroundColor(VGColor.rgba(0, 0, 0, overlayAlpha))
                    .render();
        }
    }

    private void renderBorder(float x, float y, float w, float h, float radius, int color, float alpha, int bgColor) {
        int borderColor = VGColor.withAlpha(color, alpha);
        float borderWidth = 2f;

        VG.roundRect(x, y, w, h, radius, borderColor);
        VG.roundRect(x + borderWidth, y + borderWidth,
                    w - borderWidth * 2, h - borderWidth * 2,
                    Math.max(0, radius - borderWidth), bgColor);
    }

    private void renderIcons() {
        float iconY = VG.getIconFont().centerY(card.y(), card.height(), iconSize);
        float rightIconWidth = VGMeasure.measureIcon(rightIcon, iconSize).width();

        if (leftIcon != null) {
            float iconX = card.x() + paddingLeft;
            VG.icon(leftIcon, iconX, iconY, iconSize, iconColor);
        }

        if (rightIcon != null) {
            float rightIconX = card.right() - paddingRight - rightIconWidth;
            VG.icon(rightIcon, rightIconX, iconY, iconSize, iconColor);
        }
    }

    private void renderTextAndCursor() {
        String displayText = text.isEmpty() ? "" : (passwordMode ? maskText(text) : text);
        float textX = getTextStartX();

        float textMaxWidth = card.right() - paddingRight;
        if (rightIcon != null) {
            textMaxWidth -= (VGMeasure.measureIcon(rightIcon, iconSize).width() + ICON_GAP);
        }
        textMaxWidth -= textX;

        float textY = VGFontMetrics.centerY(card.y(), card.height(), font);
        float textBoxHeight = VGFontMetrics.textBoxHeight(font);

        int visibleTextStartIndex = 0;
        String visibleText = displayText;

        if (!displayText.isEmpty()) {
            float fullTextWidth = measureTextWidth(displayText);

            if (fullTextWidth > textMaxWidth) {
                float cursorXPos = measureTextWidth(displayText.substring(0, cursorPosition));

                if (cursorXPos > textMaxWidth) {
                    visibleTextStartIndex = findVisibleStartIndex(displayText, cursorPosition, textMaxWidth);
                    visibleText = displayText.substring(visibleTextStartIndex);
                }
            }
        }

        if (text.isEmpty() && !isFocused() && !placeholder.isEmpty()) {
            VG.text(placeholder, textX, textY, placeholderColor);
        } else if (!visibleText.isEmpty()) {
            VG.text(visibleText, textX, textY, textColor);
        }

        if (isFocused() && cursorVisible) {
            int cursorVisibleIndex = cursorPosition - visibleTextStartIndex;
            String textBeforeCursor = visibleText.substring(0, Math.min(cursorVisibleIndex, visibleText.length()));
            float cursorX = textX + measureTextWidth(textBeforeCursor);

            float cursorHeight = Math.min(textBoxHeight, card.height() * 0.7f);
            float cursorTop = textY + (textBoxHeight - cursorHeight) / 2f;

            VG.line(cursorX, cursorTop, cursorX, cursorTop + cursorHeight,
                    2f, VG.theme().primary());
        }

        if (hasSelection()) {
            renderSelection(textX, textY, visibleTextStartIndex, textBoxHeight);
        }
    }

    private int findVisibleStartIndex(String fullText, int cursorPos, float maxWidth) {
        for (int i = 0; i < cursorPos; i++) {
            String sub = fullText.substring(i, cursorPos);
            if (measureTextWidth(sub) <= maxWidth) {
                return i;
            }
        }
        return Math.max(0, cursorPos - 1);
    }

    private void renderSelection(float textX, float textY, int visibleStartIndex, float textBoxHeight) {
        int selStart = Math.min(selectionStart, selectionEnd);
        int selEnd = Math.max(selectionStart, selectionEnd);
        String displayText = passwordMode ? maskText(text) : text;

        int visSelStart = Math.max(0, selStart - visibleStartIndex);
        int visSelEnd = Math.min(displayText.length() - visibleStartIndex, selEnd - visibleStartIndex);

        if (visSelStart >= visSelEnd) return;

        String visibleText = displayText.substring(visibleStartIndex);

        float startX = textX + measureTextWidth(visibleText.substring(0, visSelStart));
        float endX = textX + measureTextWidth(visibleText.substring(0, visSelEnd));
        float selectionHeight = Math.min(textBoxHeight, card.height() * 0.7f);
        float selectionTop = textY + (textBoxHeight - selectionHeight) / 2f;

        VG.rect(startX, selectionTop, endX - startX, selectionHeight,
                VGColor.rgba(149, 102, 255, 0.25f));
    }

    private void renderLabel() {
        if (label.isEmpty()) return;
        float labelX = card.x();
        float labelY = card.y() - VGFontMetrics.textBoxHeight(font) - VGSpacing.ICON_TEXT_GAP;
        VG.text(label, labelX, labelY, labelColor);
    }

    private float getTextStartX() {
        float x = card.x() + paddingLeft;
        if (leftIcon != null) {
            x += VGMeasure.measureIcon(leftIcon, iconSize).width() + ICON_GAP;
        }
        return x;
    }

    private int blendColors(int bg, int fg) {
        float a = VGColor.a(fg);
        if (a >= 1f) return fg;
        float invA = 1f - a;
        return VGColor.rgba(
                VGColor.r(bg) * invA + VGColor.r(fg) * a,
                VGColor.g(bg) * invA + VGColor.g(fg) * a,
                VGColor.b(bg) * invA + VGColor.b(fg) * a,
                VGColor.a(bg)
        );
    }

    // --- Public API ---

    public boolean isFocused() { return VG.getInstance().getRenderContext().getFocused() == this; }

    public String getText() { return text; }

    public VGInput setText(String text) {
        this.text = text != null ? text : "";
        this.cursorPosition = this.text.length();
        clearSelection();
        return this;
    }

    public void clear() { setText(""); }

    public VGInput setPlaceholder(String placeholder) {
        this.placeholder = placeholder != null ? placeholder : "";
        return this;
    }

    public VGInput setPlaceholderColor(int color) {
        this.placeholderColor = color;
        return this;
    }

    public VGInput setTextColor(int color) {
        this.textColor = color;
        return this;
    }

    public VGInput setFontSize(int size) {
        this.fontSize = size;
        return this;
    }

    public VGInput setPasswordMode(boolean enabled) {
        this.passwordMode = enabled;
        return this;
    }

    public VGInput setPasswordChar(char c) {
        this.passwordChar = c;
        return this;
    }

    public VGInput setMaxLength(int maxLen) {
        this.maxLength = maxLen;
        return this;
    }

    @Override
    public VGInput setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            var ctx = VG.getInstance().getRenderContext();
            if (ctx.getFocused() == this) ctx.setFocus(null);
        }
        return this;
    }

    @Override
    public boolean isEnabled() { return enabled; }

    public VGInput setError(boolean error) {
        this.errorState = error;
        return this;
    }

    public VGInput setError(boolean error, String message) {
        this.errorState = error;
        this.errorMessage = message != null ? message : "";
        return this;
    }

    public boolean hasError() { return errorState; }
    public String getErrorMessage() { return errorMessage; }

    public VGInput setLabel(String label) {
        this.label = label != null ? label : "";
        return this;
    }

    public VGInput setLabelColor(int color) {
        this.labelColor = color;
        return this;
    }

    public VGInput setLabelFontSize(float size) {
        this.labelFontSize = size;
        return this;
    }

    public VGInput setBackgroundColor(int color) {
        card.setBackgroundColor(color);
        return this;
    }

    public int backgroundColor() { return card.backgroundColor(); }

    public VGInput setRadius(float radius) {
        card.setRadius(radius);
        return this;
    }

    public VGInput setPadding(float padding) {
        this.paddingLeft = padding;
        this.paddingRight = padding;
        return this;
    }

    public VGInput setPadding(float horizontal, float vertical) {
        this.paddingLeft = horizontal;
        this.paddingRight = horizontal;
        return this;
    }

    public VGInput setFont(VGFont font) {
        this.font = font;
        return this;
    }

    public VGInput setLeftIcon(Icons icon) {
        this.leftIcon = icon;
        return this;
    }

    public VGInput setRightIcon(Icons icon) {
        this.rightIcon = icon;
        return this;
    }

    public VGInput setIconColor(int color) {
        this.iconColor = color;
        return this;
    }

    public VGInput setIconSize(float size) {
        this.iconSize = size;
        return this;
    }

    public VGInput setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public VGInput setSize(float width, float height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public VGInput onFocus(Runnable action) {
        focusListeners.add(action);
        return this;
    }

    public VGInput onBlur(Runnable action) {
        blurListeners.add(action);
        return this;
    }

    public VGInput onChange(java.util.function.Consumer<String> action) {
        changeListeners.add(action);
        return this;
    }

    public VGInput onSubmit(java.util.function.Consumer<String> action) {
        submitListeners.add(action);
        return this;
    }

    private void fireFocus() { for (Runnable l : focusListeners) l.run(); }
    private void fireBlur() { for (Runnable l : blurListeners) l.run(); }
    private void fireChange(String newText) { for (var l : changeListeners) l.accept(newText); }
    private void fireSubmit(String currentText) { for (var l : submitListeners) l.accept(currentText); }
}