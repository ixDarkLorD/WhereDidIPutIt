package net.ixdarklord.wheredidiputit.client.hud;

import net.ixdarklord.coolcatcore.api.event.v2.client.ClientGuiEvents;
import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.ixdarklord.wheredidiputit.client.render.ContainerHighlight;
import net.ixdarklord.wheredidiputit.client.render.Guidance;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;

/**
 * A small compass at the top of the screen while {@link Guidance} leads somewhere and the highlight is over: the item
 * found, an arrow turned towards the container as the player turns, and how far it is. It goes once the container is
 * opened ({@code highlight.compass}). Where it sits is set by {@code highlight.compassX} / {@code compassY}; when
 * another mod's box drawn there last frame (Jade's, through {@code compat/jade}) would cover it, it moves below that.
 */
public final class Compass {
    private static final Identifier ARROW = WhereDidIPutIt.id("textures/gui/arrow.png");
    private static final int SIZE = 16;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    /** How long a box reported by another mod is kept out of the way of: a frame or two, as it is reported each frame. */
    private static final long AVOID_MILLIS = 150L;
    private static final int AVOID_GAP = 3;

    private static float avoidX;
    private static float avoidY;
    private static float avoidWidth;
    private static float avoidHeight;
    private static long avoidSeen;

    private Compass() {
    }

    public static void init() {
        ClientGuiEvents.RENDER_HUD.register(Compass::render);
    }

    /** A box another mod drew on the HUD this frame, in GUI pixels, for the compass to keep clear of. */
    public static void avoid(float x, float y, float width, float height) {
        avoidX = x;
        avoidY = y;
        avoidWidth = width;
        avoidHeight = height;
        avoidSeen = Util.getMillis();
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.options.hideGui || !WDIPIClientConfig.COMPASS.get()) return;
        Guidance.Target target = Guidance.current(minecraft.level);
        // While the container is highlighted its own arrow and marker show the way.
        if (target == null || !ContainerHighlight.current(minecraft.level).isEmpty()) return;

        Vec3 to = target.box().getCenter().subtract(player.position());
        // The bearing as the game counts yaw (0 south, 90 west), less where the player faces: 0 is straight ahead.
        float bearing = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
        float turn = Mth.wrapDegrees(bearing - player.getYRot());
        Component distance = Component.translatable("gui.wheredidiputit.compass.distance", Mth.floor(to.length()));
        int width = SIZE + 2 + SIZE + 4 + minecraft.font.width(distance);
        int x = Mth.clamp(graphics.guiWidth() * WDIPIClientConfig.COMPASS_X.get() / 100 - width / 2, 0, Math.max(0, graphics.guiWidth() - width));
        int top = Mth.clamp(WDIPIClientConfig.COMPASS_Y.get(), 0, Math.max(0, graphics.guiHeight() - SIZE));
        if (Util.getMillis() - avoidSeen < AVOID_MILLIS && x < avoidX + avoidWidth && x + width > avoidX
                && top < avoidY + avoidHeight && top + SIZE > avoidY) {
            top = Mth.ceil(avoidY + avoidHeight) + AVOID_GAP;
        }
        int rgb = WDIPIClientConfig.HIGHLIGHT_COLOR.get() & 0xFFFFFF;

        graphics.item(target.item(), x, top);
        // The arrow texture points right: a quarter turn back makes "ahead" point up.
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + SIZE + 2 + SIZE / 2.0F, top + SIZE / 2.0F);
        graphics.pose().rotate((float) Math.toRadians(turn) - Mth.HALF_PI);
        graphics.blit(RenderPipelines.GUI_TEXTURED, ARROW, -SIZE / 2, -SIZE / 2, 0.0F, 0.0F, SIZE, SIZE, SIZE * 2, SIZE, ARGB.opaque(rgb));
        graphics.pose().popMatrix();
        graphics.text(minecraft.font, distance, x + SIZE * 2 + 6, top + 4, TEXT_COLOR);
    }
}
