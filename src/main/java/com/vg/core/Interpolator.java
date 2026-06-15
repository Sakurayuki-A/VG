package com.vg.core;

public final class Interpolator {

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static float clamp01(float t) {
        return Math.max(0f, Math.min(1f, t));
    }

    public static float smoothStep(float t) {
        t = clamp01(t);
        return t * t * (3 - 2 * t);
    }

    public static float easeOutCubic(float t) {
        t = clamp01(t);
        return 1 - (float) Math.pow(1 - t, 3);
    }

    public static float easeOutQuad(float t) {
        t = clamp01(t);
        return 1 - (1 - t) * (1 - t);
    }

    public static float easeOutElastic(float t) {
        if (t == 0 || t == 1) return t;
        return (float) Math.pow(2, -10 * t) * (float) Math.sin((t - 0.075f) * (2 * Math.PI) / 0.3f) + 1;
    }

    public static float spring(float current, float target, float velocity,
                               float stiffness, float damping, float dt) {
        float force = -(current - target) * stiffness;
        force -= damping * velocity;
        float newVelocity = velocity + force * dt;
        return current + newVelocity * dt;
    }

    public static float easeOutBounce(float t) {
        t = clamp01(t);
        if (t < 1f / 2.75f) {
            return 7.5625f * t * t;
        } else if (t < 2f / 2.75f) {
            t -= 1.5f / 2.75f;
            return 7.5625f * t * t + 0.75f;
        } else if (t < 2.5f / 2.75f) {
            t -= 2.25f / 2.75f;
            return 7.5625f * t * t + 0.9375f;
        } else {
            t -= 2.625f / 2.75f;
            return 7.5625f * t * t + 0.984375f;
        }
    }
}
