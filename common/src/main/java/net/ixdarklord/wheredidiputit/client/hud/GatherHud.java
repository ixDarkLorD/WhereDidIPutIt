package net.ixdarklord.wheredidiputit.client.hud;

import net.ixdarklord.coolcatcore.api.event.v2.client.ClientGuiEvents;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.ixdarklord.wheredidiputit.find.FindMessages;
import net.ixdarklord.wheredidiputit.find.GatherList;
import net.ixdarklord.wheredidiputit.find.TooltipLines;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The gathering list on the HUD, top left: each line's item and how many the player carries of how many wanted
 * (ticked and dimmed once it is enough), and the next container on the route with how far it is.
 */
public final class GatherHud {
    private static final int LEFT = 4;
    private static final int TOP = 4;
    private static final int PADDING = 4;
    private static final int ROW = 18;
    private static final int BACKGROUND = 0xA0101318;
    private static final int TEXT = 0xFFE6E6E6;
    private static final int DONE = 0xFF7FD87F;
    private static final int MUTED = 0xFF9AA0A8;

    private GatherHud() {
    }

    public static void init() {
        ClientGuiEvents.RENDER_HUD.register(GatherHud::render);
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui || GatherList.needs().isEmpty()) return;
        Font font = minecraft.font;
        Component title = Component.translatable("gui.wheredidiputit.gather.title");
        GatherList.Stop next = GatherList.next();
        Component nextLine = next == null ? Component.translatable("gui.wheredidiputit.gather.no_stop")
                : Component.translatable("gui.wheredidiputit.gather.next", FindMessages.name(next.record()),
                        Mth.floor(Vec3.atCenterOf(next.record().pos()).distanceTo(minecraft.player.position())));

        int width = Math.max(font.width(title), font.width(nextLine));
        for (GatherList.Need need : GatherList.needs()) width = Math.max(width, 20 + font.width(line(need)));
        int height = 12 + GatherList.needs().size() * ROW + 12;
        int accent = 0xFF000000 | WDIPIClientConfig.HIGHLIGHT_COLOR.get();
        graphics.fill(LEFT, TOP, LEFT + width + PADDING * 2, TOP + height + PADDING * 2, BACKGROUND);
        graphics.fill(LEFT, TOP, LEFT + 2, TOP + height + PADDING * 2, accent);

        int x = LEFT + PADDING + 2;
        int y = TOP + PADDING;
        graphics.text(font, title, x, y, accent);
        y += 12;
        for (GatherList.Need need : GatherList.needs()) {
            boolean done = GatherList.have(need) >= need.wanted();
            graphics.item(need.shown(), x, y);
            graphics.text(font, line(need), x + 20, y + 4, done ? DONE : TEXT);
            y += ROW;
        }
        graphics.text(font, nextLine, x, y + 1, MUTED);
    }

    // "Iron Ingot  3 / 8", ticked once it is enough.
    private static Component line(GatherList.Need need) {
        int have = Math.min(GatherList.have(need), need.wanted());
        Component counts = Component.literal(TooltipLines.number(have) + " / " + TooltipLines.number(need.wanted()));
        return Component.translatable(have >= need.wanted() ? "gui.wheredidiputit.gather.line_done" : "gui.wheredidiputit.gather.line",
                need.shown().getHoverName(), counts);
    }
}
