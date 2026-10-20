package net.ixdarklord.wheredidiputit.client.hud;

import net.ixdarklord.coolcatcore.api.event.v2.client.ClientGuiEvents;
import net.ixdarklord.wheredidiputit.client.render.ContainerHighlight;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Locale;

/**
 * The item searched for, drawn over the middle of each container being pointed out, with how many of it the
 * container held in its bottom right corner, as in a slot. It is drawn on the HUD, so walls never hide it.
 */
public final class ItemMarker {
    private ItemMarker() {
    }

    public static void init() {
        ClientGuiEvents.RENDER_HUD.register(ItemMarker::render);
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.options.hideGui || !WDIPIClientConfig.ITEM_MARKER.get()) return;
        for (ContainerHighlight.Target target : ContainerHighlight.current(minecraft.level)) {
            ScreenProjection on = ScreenProjection.of(minecraft, graphics, target.box().getCenter());
            if (!on.inView()) continue;
            int x = Math.round(on.x());
            int y = Math.round(on.y());
            // Drawn as a slot draws it: the amount in the bottom right corner, by the game's own item decorations.
            graphics.item(target.item(), x - 8, y - 8);
            graphics.itemDecorations(minecraft.font, target.item(), x - 8, y - 8, count(target.count()));
        }
    }

    /** An amount as it fits on an item: 64, 1.2k, 3.4M. */
    static String count(long count) {
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
