package net.ixdarklord.wheredidiputit.find;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.ixdarklord.wheredidiputit.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.Util;
import org.jetbrains.annotations.Nullable;

/**
 * A line of text shown above an open container screen for a moment: the action bar is hidden behind the screen, so
 * a search that found nothing (and leaves the screen open) says so here. Drawn by {@code AbstractContainerScreenMixin}.
 */
public final class ScreenNotice {
    private static final long SHOW_MILLIS = 2500L;
    private static final long FADE_MILLIS = 400L;

    private static @Nullable Component text;
    private static long until;

    private ScreenNotice() {
    }

    public static void show(Component message) {
        text = message;
        until = Util.getMillis() + SHOW_MILLIS;
    }

    /** Draws the notice centred on {@code centerX}, just above {@code panelTop}, while it lasts. */
    public static void extract(GuiGraphics graphics, int centerX, int panelTop) {
        Component current = text;
        long left = until - Util.getMillis();
        if (current == null || left <= 0) return;
        int alpha = Math.round(Mth.clamp(left / (float) FADE_MILLIS, 0.0F, 1.0F) * 255.0F);
        // 1.20.1's font draws text with almost no alpha as opaque.
        if (alpha < 5) return;
        int y = Math.max(2, panelTop - 12);
        graphics.drawCenteredString(Minecraft.getInstance().font, current, centerX, y, ARGB.color(alpha, 0xFFFFFF));
    }
}
