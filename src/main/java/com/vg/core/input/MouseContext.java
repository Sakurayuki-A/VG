package com.vg.core.input;

public class MouseContext {

    private float x, y;
    private float scrollY;
    private boolean leftPressed;
    private boolean leftReleased;
    private boolean leftDown;
    private boolean rightPressed;
    private boolean rightReleased;
    private boolean rightDown;

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float scrollY() {
        return scrollY;
    }

    public boolean leftPressed() {
        return leftPressed;
    }

    public boolean leftReleased() {
        return leftReleased;
    }

    public boolean leftDown() {
        return leftDown;
    }

    public boolean rightPressed() {
        return rightPressed;
    }

    public boolean rightReleased() {
        return rightReleased;
    }

    public boolean rightDown() {
        return rightDown;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void addScroll(float y) {
        this.scrollY += y;
    }

    public void setLeftPressed(boolean pressed) {
        if (pressed && !leftDown) leftPressed = true;
        if (!pressed && leftDown) leftReleased = true;
        leftDown = pressed;
    }

    public void setRightPressed(boolean pressed) {
        if (pressed && !rightDown) rightPressed = true;
        if (!pressed && rightDown) rightReleased = true;
        rightDown = pressed;
    }

    public void endFrame() {
        leftPressed = false;
        leftReleased = false;
        rightPressed = false;
        rightReleased = false;
        scrollY = 0;
    }
}
