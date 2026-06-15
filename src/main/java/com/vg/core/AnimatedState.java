package com.vg.core;

public class AnimatedState {

    private float current = 0f;
    private float target = 0f;
    private float velocity = 0f;
    private final float stiffness;
    private final float damping;

    public AnimatedState() {
        this(12f, 5f);
    }

    public AnimatedState(float stiffness, float damping) {
        this.stiffness = stiffness;
        this.damping = damping;
    }

    public void setTarget(boolean active) {
        target = active ? 1f : 0f;
    }

    public void update(float dt) {
        velocity += (target - current) * stiffness * dt;
        velocity *= Math.max(0, 1 - damping * dt);
        current += velocity * dt;
        if (Math.abs(current - target) < 0.001f && Math.abs(velocity) < 0.01f) {
            current = target;
            velocity = 0;
        }
        current = Interpolator.clamp01(current);
    }

    public float get() {
        return current;
    }

    public boolean isActive() {
        return current > 0.5f;
    }

    public boolean isAtTarget() {
        return Math.abs(current - target) < 0.005f;
    }

    public void reset() {
        current = 0f;
        target = 0f;
        velocity = 0f;
    }

    public void instant(boolean active) {
        current = active ? 1f : 0f;
        target = current;
        velocity = 0f;
    }
}
