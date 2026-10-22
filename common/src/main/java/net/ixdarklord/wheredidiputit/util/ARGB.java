package net.ixdarklord.wheredidiputit.util;

/**
 * The few colour helpers of 26.1's {@code net.minecraft.util.ARGB} the mod uses, which 1.20.1 lacks (its
 * {@code FastColor.ARGB32} has only some), under the same names so the code reads as on 26.1.
 */
public final class ARGB {
    private ARGB() {
    }

    public static int color(int alpha, int rgb) {
        return (alpha & 0xFF) << 24 | rgb & 0xFFFFFF;
    }

    public static int opaque(int rgb) {
        return 0xFF000000 | rgb;
    }

    public static int alpha(int argb) {
        return argb >>> 24;
    }

    /** The colour with its alpha multiplied by {@code factor}. */
    public static int multiplyAlpha(int argb, float factor) {
        return color(Math.round(alpha(argb) * Math.max(0.0F, Math.min(1.0F, factor))), argb);
    }

    /** Each channel, alpha included, {@code t} of the way from {@code from} to {@code to}. */
    public static int srgbLerp(float t, int from, int to) {
        return lerp(t, from >>> 24, to >>> 24) << 24 | lerp(t, from >> 16 & 0xFF, to >> 16 & 0xFF) << 16
                | lerp(t, from >> 8 & 0xFF, to >> 8 & 0xFF) << 8 | lerp(t, from & 0xFF, to & 0xFF);
    }

    private static int lerp(float t, int from, int to) {
        return Math.round(from + t * (to - from)) & 0xFF;
    }
}
