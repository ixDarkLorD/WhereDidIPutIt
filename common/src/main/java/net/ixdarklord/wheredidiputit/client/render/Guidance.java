package net.ixdarklord.wheredidiputit.client.render;

import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxDraw;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxFrame;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxMaterial;
import net.ixdarklord.coolcatcanvas.api.client.vfx.WorldVfx;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.ixdarklord.wheredidiputit.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Leads the way to the one container a find pointed out (the find key, the room finder, the browser's Show): a glowing
 * line from a little above the player's feet straight to the container, up or down and through whatever is between,
 * with chevrons drifting along it towards the container ({@code highlight.pathLine}), and the HUD compass
 * ({@code client/hud/Compass}, {@code highlight.compass}), both lasting after the highlight until the container is
 * opened or broken, the player changes dimension, {@link #LIFETIME_MILLIS} pass, or the player is away (neither moving
 * nor turning) for {@link #AFK_MILLIS}. The line is laid out again every frame from where the player is, so it follows
 * them without lag.
 */
public final class Guidance {
    private static final long LIFETIME_MILLIS = 5L * 60L * 1000L;
    /** A player who neither moves nor looks round for this long is away: the guidance stops. */
    private static final long AFK_MILLIS = 60L * 1000L;
    /** How far above the player's feet the line starts, in blocks. */
    private static final double RAISE = 0.3;
    /** The line is cut into pieces this long, in blocks, so it fades evenly. */
    private static final double STEP = 0.1;
    /** The line starts this far ahead of the player and fades in over the next {@link #FADE_IN}, in blocks. */
    private static final double START = 0.9;
    private static final double FADE_IN = 1.5;
    /** It fades out over this much before the container. */
    private static final double FADE_OUT = 0.6;
    /**
     * Further from the player than {@link #FADE_FROM} the line fades, until it's gone at {@link #RANGE}, in blocks, and
     * nothing past that is drawn: it shows the way to go, not the whole way there.
     */
    private static final double FADE_FROM = 6.0;
    private static final double RANGE = 14.0;
    private static final VfxMaterial LINE = VfxMaterial.alpha(VfxMaterial.GLOW).withSoftness(0.0F).withShader(WhereDidIPutIt.id("vfx/path_line"));

    private static @Nullable Target target;
    // Where the player was and how they looked when last seen moving, and when that was.
    private static @Nullable Vec3 lastPosition;
    private static float lastYaw;
    private static float lastPitch;
    private static long lastActive;

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
        lastPosition = null;
        lastActive = Util.getMillis();
    }

    public static void clear() {
        target = null;
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
                || Util.getMillis() - current.since > LIFETIME_MILLIS || isAway(minecraft)) {
            clear();
        }
    }

    // Whether the player has neither moved nor turned for AFK_MILLIS.
    private static boolean isAway(Minecraft minecraft) {
        Vec3 position = minecraft.player.position();
        float yaw = minecraft.player.getYRot();
        float pitch = minecraft.player.getXRot();
        long now = Util.getMillis();
        if (lastPosition == null || position.distanceToSqr(lastPosition) > 0.01
                || Math.abs(Mth.wrapDegrees(yaw - lastYaw)) > 1.0F || Math.abs(pitch - lastPitch) > 1.0F) {
            lastPosition = position;
            lastYaw = yaw;
            lastPitch = pitch;
            lastActive = now;
            return false;
        }
        return now - lastActive > AFK_MILLIS;
    }

    // A ribbon facing the camera, from just ahead of the player straight to the nearest point of the container's box.
    private static void render(VfxFrame frame) {
        Minecraft minecraft = Minecraft.getInstance();
        Target current = current(frame.level());
        if (current == null || minecraft.player == null || !WDIPIClientConfig.PATH_LINE.get()) return;
        Vec3 from = minecraft.player.getPosition(frame.partialTick()).add(0.0, RAISE, 0.0);
        AABB box = current.box;
        Vec3 to = new Vec3(Mth.clamp(from.x, box.minX, box.maxX), Mth.clamp(from.y, box.minY, box.maxY), Mth.clamp(from.z, box.minZ, box.maxZ));
        double length = from.distanceTo(to);
        if (length - START <= FADE_OUT) return;
        Vec3 direction = to.subtract(from).scale(1.0 / length);
        double end = Math.min(length, START + RANGE);

        int rgb = WDIPIClientConfig.HIGHLIGHT_COLOR.get() & 0xFFFFFF;
        Vec3 camera = frame.cameraPosition();
        VfxDraw draw = frame.draw(LINE);
        Vec3 lastPoint = null;
        Vec3 lastSide = null;
        double lastAlong = 0.0;
        float lastAlpha = 0.0F;
        int pieces = (int) Math.ceil((end - START) / STEP);
        for (int i = 0; i <= pieces; i++) {
            double along = Math.min(START + i * STEP, end);
            Vec3 point = from.add(direction.scale(along));
            Vec3 side = point.subtract(camera).cross(direction);
            if (side.lengthSqr() < 1.0E-8) continue;
            // As wide as the outline's glow, so it reads the same at any distance.
            float half = (float) Math.max(0.13, point.distanceTo(camera) * 0.018);
            side = side.normalize().scale(half);
            float alpha = (float) (Mth.clamp((along - START) / FADE_IN, 0.0, 1.0)
                    * Mth.clamp((length - along) / FADE_OUT, 0.0, 1.0)
                    * (1.0 - Mth.smoothstep(Mth.clamp((along - FADE_FROM) / (RANGE - FADE_FROM), 0.0, 1.0))));
            if (lastPoint != null) {
                int color = ARGB.color(Math.round((lastAlpha + alpha) * 0.5F * 255.0F), rgb);
                // u counts back from the container, so the shader's chevrons stay put in the world as the player moves.
                draw.quad(lastPoint.add(lastSide), lastPoint.subtract(lastSide), point.subtract(side), point.add(side),
                        (float) (lastAlong - length), 0.0F, (float) (along - length), 1.0F, color);
            }
            lastPoint = point;
            lastSide = side;
            lastAlong = along;
            lastAlpha = alpha;
        }
    }
}
