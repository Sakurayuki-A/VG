package com.vg.core.input;

import java.util.ArrayList;
import java.util.List;

public class KeyboardContext {

    private boolean[] keyPressed = new boolean[512];
    private boolean[] keyJustPressed = new boolean[512];
    private boolean[] keyJustReleased = new boolean[512];

    private final List<Integer> charBuffer = new ArrayList<>();

    public void onKey(int keyCode, int action) {
        if (keyCode >= 0 && keyCode < keyPressed.length) {
            if (action == 1) { // Press
                if (!keyPressed[keyCode]) {
                    keyJustPressed[keyCode] = true;
                }
                keyPressed[keyCode] = true;
            } else if (action == 0) { // Release
                if (keyPressed[keyCode]) {
                    keyJustReleased[keyCode] = true;
                }
                keyPressed[keyCode] = false;
            }
        }
    }

    public void onChar(int codepoint) {
        charBuffer.add(codepoint);
    }

    public boolean isKeyPressed(int keyCode) {
        return keyCode >= 0 && keyCode < keyPressed.length && keyPressed[keyCode];
    }

    public boolean isKeyJustPressed(int keyCode) {
        return keyCode >= 0 && keyCode < keyJustPressed.length && keyJustPressed[keyCode];
    }

    public boolean isKeyJustReleased(int keyCode) {
        return keyCode >= 0 && keyCode < keyJustReleased.length && keyJustReleased[keyCode];
    }

    public List<Integer> getCharBuffer() {
        return charBuffer;
    }

    public boolean hasChars() {
        return !charBuffer.isEmpty();
    }

    public void clearChars() {
        charBuffer.clear();
    }

    public void endFrame() {
        for (int i = 0; i < keyJustPressed.length; i++) {
            keyJustPressed[i] = false;
            keyJustReleased[i] = false;
        }
        charBuffer.clear();
    }

    public boolean isControlDown() {
        return isKeyPressed(341) || isKeyPressed(345); // Left/Right Ctrl
    }

    public boolean isShiftDown() {
        return isKeyPressed(340) || isKeyPressed(344); // Left/Right Shift
    }

    public boolean isAltDown() {
        return isKeyPressed(342) || isKeyPressed(346); // Left/Right Alt
    }

    public boolean isBackspace() {
        return isKeyJustPressed(259);
    }

    public boolean isDelete() {
        return isKeyJustPressed(261);
    }

    public boolean isEnter() {
        return isKeyJustPressed(257) || isKeyJustPressed(335); // Enter / Keypad Enter
    }

    public boolean isTab() {
        return isKeyJustPressed(258);
    }

    public boolean isEscape() {
        return isKeyJustPressed(256);
    }

    public boolean isLeft() {
        return isKeyJustPressed(263);
    }

    public boolean isRight() {
        return isKeyJustPressed(262);
    }

    public boolean isHome() {
        return isKeyJustPressed(268);
    }

    public boolean isEnd() {
        return isKeyJustPressed(269);
    }

    public boolean isSelectAll() {
        return isControlDown() && isKeyJustPressed(65); // Ctrl+A
    }

    public boolean isCopy() {
        return isControlDown() && isKeyJustPressed(67); // Ctrl+C
    }

    public boolean isPaste() {
        return isControlDown() && isKeyJustPressed(86); // Ctrl+V
    }

    public boolean isCut() {
        return isControlDown() && isKeyJustPressed(88); // Ctrl+X
    }
}
