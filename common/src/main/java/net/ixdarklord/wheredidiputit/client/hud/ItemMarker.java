package net.ixdarklord.wheredidiputit.client.hud;

import net.ixdarklord.coolcatcore.api.event.v2.client.ClientGuiEvents;
import net.ixdarklord.wheredidiputit.client.render.ContainerHighlight;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The item searched for, drawn over the middle of each container being pointed out, with how many of it the
 * container held in its bottom right corner, as in a slot, and, for a remembered container, how long ago it was last
 * seen ({@code showAge}), as what it holds may have changed since. It is drawn on the HUD, so walls never hide it.
 */
public final class ItemMarker {
    private static final int AGE_COLOR = 0xFFC0C0C0;

    private ItemMarker() {
    }

    public static void init() {
        ClientGuiEvents.RENDER_HUD.register(ItemMarker::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.options.hideGui || !WDIPIClientConfig.ITEM_MARKER.get()) return;
        for (ContainerHighlight.Target target : ContainerHighlight.current(minecraft.level)) {
            ScreenProjection on = ScreenProjection.of(minecraft, graphics, target.box().getCenter());
            if (!on.inView()) continue;
            int x = Math.round(on.x());
            int y = Math.round(on.y());
            // Drawn as a slot draws it: the amount in the bottom right corner, by the game's own item decorations.
            graphics.renderItem(target.item(), x - 8, y - 8);
            graphics.renderItemDecorations(minecraft.font, target.item(), x - 8, y - 8, count(target.count()));
            if (WDIPIClientConfig.SHOW_AGE.get() && target.seenAt() != ContainerHighlight.LIVE) {
                Component age = age(System.currentTimeMillis() - target.seenAt());
                if (age != null) graphics.drawCenteredString(minecraft.font, age, x, y + 10, AGE_COLOR);
            }
        }
    }

    /** How long ago, in the largest whole unit: 5m ago, 3h ago, 2d ago; null under a minute, when it hardly matters. */
    public static @Nullable Component age(long millis) {
        long minutes = millis / 60_000L;
        if (minutes < 1) return null;
        Component amount;
        if (minutes < 60) amount = Component.translatable("gui.wheredidiputit.age.minutes", minutes);
        else if (minutes < 60 * 24) amount = Component.translatable("gui.wheredidiputit.age.hours", minutes / 60);
        else amount = Component.translatable("gui.wheredidiputit.age.days", minutes / (60 * 24));
        return Component.translatable("gui.wheredidiputit.age", amount);
    }

    /** An amount as it fits on an item: 64, 1.2k, 3.4M. */
    public static String count(long count) {
        if (count < 1000) return Long.toString(count);
        String[] units = {"k", "M", "G", "T"};
        double value = count;
        int unit = -1;
        while (value >= 1000.0 && unit < units.length - 1) {
            value /= 1000.0;
            unit++;
        }
        return (value < 10.0 ? String.format(Locale.ROOT, "%.1f", value) : Long.toString((long) value)) + units[unit];
    }
}
