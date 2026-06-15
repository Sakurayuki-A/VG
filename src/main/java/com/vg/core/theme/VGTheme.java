package com.vg.core.theme;

import com.vg.core.color.VGColor;

public record VGTheme(
        int background,

        int surface,
        int surfaceElevated,

        int primary,
        int primaryHover,
        int primaryPressed,

        int textPrimary,
        int textSecondary,

        int border
) {

    public static VGTheme light() {
        return new VGTheme(
                VGColor.rgb(232, 236, 242),
                VGColor.rgb(255, 255, 255),
                VGColor.rgb(246, 249, 255),
                VGColor.rgb(66, 133, 244),
                VGColor.rgb(52, 120, 230),
                VGColor.rgb(38, 105, 210),
                VGColor.rgb(32, 33, 36),
                VGColor.rgb(95, 99, 104),
                VGColor.rgb(194, 199, 206)
        );
    }

    public static VGTheme brand() {
        return new VGTheme(
                VGColor.rgb(12, 10, 20),
                VGColor.rgb(24, 20, 40),
                VGColor.rgb(35, 29, 56),
                VGColor.rgb(149, 102, 255),
                VGColor.rgb(167, 123, 255),
                VGColor.rgb(124, 82, 219),
                VGColor.rgb(245, 245, 250),
                VGColor.rgb(170, 170, 190),
                VGColor.rgba(255, 255, 255, 24)
        );
    }
}
