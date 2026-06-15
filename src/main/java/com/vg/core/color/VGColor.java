package com.vg.core.color;

public final class VGColor {

    public static final int RED = 0xFFFF0000;
    public static final int GREEN = 0xFF00FF00;
    public static final int BLUE = 0xFF0000FF;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int BLACK = 0xFF000000;
    public static final int YELLOW = 0xFFFFFF00;

    private VGColor() {
    }

    public static float r(int argb) {
        return ((argb >> 16) & 0xFF) / 255.0f;
    }

    public static float g(int argb) {
        return ((argb >> 8) & 0xFF) / 255.0f;
    }

    public static float b(int argb) {
        return (argb & 0xFF) / 255.0f;
    }

    public static float a(int argb) {
        return ((argb >> 24) & 0xFF) / 255.0f;
    }

    public static int argb(int a, int r, int g, int b) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int rgb(int r, int g, int b) {
        return 0xFF000000 | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int rgba(float r, float g, float b, float a) {
        int ai = Math.round(a * 255.0f);
        int ri = Math.round(r * 255.0f);
        int gi = Math.round(g * 255.0f);
        int bi = Math.round(b * 255.0f);
        return argb(ai, ri, gi, bi);
    }

    public static float[] toRgba(int argb) {
        return new float[] { r(argb), g(argb), b(argb), a(argb) };
    }

    public static int fromHsb(float h, float s, float br) {
        float c = br * s;
        float x = c * (1 - Math.abs((h / 60.0f) % 2 - 1));
        float m = br - c;

        float rp, gp, bp;
        if (h < 60) {
            rp = c;
            gp = x;
            bp = 0;
        } else if (h < 120) {
            rp = x;
            gp = c;
            bp = 0;
        } else if (h < 180) {
            rp = 0;
            gp = c;
            bp = x;
        } else if (h < 240) {
            rp = 0;
            gp = x;
            bp = c;
        } else if (h < 300) {
            rp = x;
            gp = 0;
            bp = c;
        } else {
            rp = c;
            gp = 0;
            bp = x;
        }

        return rgba(rp + m, gp + m, bp + m, 1.0f);
    }

    public static int lerp(int c0, int c1, float t) {
        float r = r(c0) + (r(c1) - r(c0)) * t;
        float g = g(c0) + (g(c1) - g(c0)) * t;
        float b = b(c0) + (b(c1) - b(c0)) * t;
        float a = a(c0) + (a(c1) - a(c0)) * t;
        return rgba(r, g, b, a);
    }
    
    public static int withAlpha(int color, float alpha) {
        return argb(Math.round(alpha * 255.0f), 
                   ((color >> 16) & 0xFF),
                   ((color >> 8) & 0xFF),
                   (color & 0xFF));
    }
}