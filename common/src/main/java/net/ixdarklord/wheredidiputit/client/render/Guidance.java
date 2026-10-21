package net.ixdarklord.wheredidiputit.client.render;

import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxDraw;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxFrame;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxMaterial;
import net.ixdarklord.coolcatcanvas.api.client.vfx.WorldVfx;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.ixdarklord.wheredidiputit.find.PathFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Leads the way to the one container a find pointed out (the find key, the room finder, the browser's Show): a line of
 * glowing dots along a walking route on the ground ({@code highlight.pathLine}), and the HUD compass
 * ({@code client/hud/Compass}, {@code highlight.compass}), both lasting after the highlight until the container is
 * opened or broken, the player changes dimension, or {@link #LIFETIME_MILLIS} pass. The route is found again while the
 * player moves ({@link PathFinder}); where none is found there is no line, and the compass still points.
 */
public final class Guidance {
    private static final long LIFETIME_MILLIS = 5L * 60L * 1000L;
    /** Ticks between looking for the route again, when the player has moved. */
    private static final int REPATH_TICKS = 20;
    /** Dots along the route, this far apart, in blocks. */
    private static final double DOT_SPACING = 0.6;
    private static final int MAX_DOTS = 300;
    private static final VfxMaterial DOT = VfxMaterial.alpha(VfxMaterial.GLOW).withSoftness(0.0F);

    private static @Nullable Target target;
    private static @Nullable List<Vec3> route;
    private static @Nullable BlockPos routeFrom;
    private static int ticks;

    private Guidance() {
    }

    /** Where the player is being led. */
    public record Target(ResourceKey<Level> dimension, BlockPos pos, AABB box, ItemStack item, long since) {
    }

    public static void init() {
        WorldVfx.register(WhereDidIPutIt.id("guidance"), Guidance::render);
        ClientTickEvents.END.register(Guidance::tick);
        ClientPlayerEvents.LEAVE.register(player -> clear());
    }

    /** Leads the way to a container from now on, in place of any other. */
    public static void follow(Level level, BlockPos pos, AABB box, ItemStack item) {
        if (!WDIPIClientConfig.PATH_LINE.get() && !WDIPIClientConfig.COMPASS.get()) return;
        target = new Target(level.dimension(), pos.immutable(), box, item.copyWithCount(1), Util.getMillis());
        route = null;
        routeFrom = null;
        ticks = REPATH_TICKS;
    }

    public static void clear() {
        target = null;
        route = null;
        routeFrom = null;
    }

    /** A container was opened, or the block at {@code pos} broken: the way there is no longer needed. */
    public static void onReached(BlockPos pos) {
        Target current = target;
        if (current != null && (current.pos.equals(pos) || current.box.contains(Vec3.atCenterOf(pos)))) clear();
    }

    /** The block at {@code pos} changed: the way to a container broken there is no longer needed. */
    public static void onReachedIfBroken(BlockPos pos, BlockState state) {
        Target current = target;
        if (current != null && current.pos.equals(pos) && state.isAir()) clear();
    }

    /** Where the player is being led in this level, or null. */
    public static @Nullable Target current(Level level) {
        Target current = target;
        return current != null && current.dimension == level.dimension() ? current : null;
    }

    private static void tick(Minecraft minecraft) {
        Target current = target;
        if (current == null) return;
        if (minecraft.level == null || minecraft.player == null || minecraft.level.dimension() != current.dimension
                || Util.getMillis() - current.since > LIFETIME_MILLIS) {
            clear();
            return;
        }
        if (!WDIPIClientConfig.PATH_LINE.get() || ++ticks < REPATH_TICKS) return;
        ticks = 0;
        BlockPos feet = minecraft.player.blockPosition();
        if (feet.equals(routeFrom)) return;
        routeFrom = feet;
        List<BlockPos> found = PathFinder.find(minecraft.level, feet, current.box);
        if (found == null) {
            route = null;
            return;
        }
        List<Vec3> points = new ArrayList<>(found.size());
        for (BlockPos pos : found) points.add(Vec3.atBottomCenterOf(pos).add(0.0, 0.06, 0.0));
        route = points;
    }

    // Dots along the route, from the player on, with a brighter pulse running towards the container.
    private static void render(VfxFrame frame) {
        List<Vec3> points = route;
        if (points == null || points.size() < 2 || current(frame.level()) == null || !WDIPIClientConfig.PATH_LINE.get()) return;
        int rgb = WDIPIClientConfig.HIGHLIGHT_COLOR.get() & 0xFFFFFF;
        VfxDraw dots = frame.draw(DOT);
        double along = 0.0;
        double next = 1.0;
        int drawn = 0;
        for (int i = 1; i < points.size() && drawn < MAX_DOTS; i++) {
            Vec3 from = points.get(i - 1);
            Vec3 to = points.get(i);
            double length = from.distanceTo(to);
            while (next <= along + length && drawn < MAX_DOTS) {
                Vec3 at = from.lerp(to, (next - along) / length);
                float pulse = 0.5F + 0.5F * Mth.sin(frame.time() * 5.0F - (float) next * 0.9F);
                dots.flat(at.x, at.y, at.z, 0.09F + 0.05F * pulse, 0.0F, ARGB.color(Math.round((0.35F + 0.5F * pulse) * 255.0F), rgb));
                next += DOT_SPACING;
                drawn++;
            }
            along += length;
        }
    }
}
