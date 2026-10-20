package net.ixdarklord.wheredidiputit.client.render;

import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxDraw;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxFrame;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxMaterial;
import net.ixdarklord.coolcatcanvas.api.client.vfx.WorldVfx;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The glowing outline round the container a search found, seen through walls (dimmer where blocks are in the way)
 * for the highlight duration. Its own shaders ({@code vfx/outline_line}, {@code vfx/outline_dot}, sharing
 * {@code include/outline.glsl}) do that: CoolCatLib's world effects hide behind solid things.
 * <p>
 * Each edge is one quad turned to the camera, as wide as the glow, with the solid line down its middle; each corner is
 * a disc of the same line and glow, so corners are round. The line is opaque, so where pieces overlap it doesn't get
 * brighter. The glow is not: an edge's glow fades out over its last glow half width ({@code u} from 1 to 0) while the
 * corner disc's round glow fades in, so the glow runs evenly round each corner instead of piling up there.
 */
public final class ContainerHighlight {
    private static final VfxMaterial LINE = VfxMaterial.alpha(VfxMaterial.GLOW).withSoftness(0.0F).withShader(WhereDidIPutIt.id("vfx/outline_line"));
    private static final VfxMaterial DOT = VfxMaterial.alpha(VfxMaterial.GLOW).withSoftness(0.0F).withShader(WhereDidIPutIt.id("vfx/outline_dot"));
    private static final long FADE_IN_MILLIS = 150L;
    private static final long FADE_OUT_MILLIS = 700L;
    // Pushed out from the block a little, so the lines don't sit in its faces and flicker against them.
    private static final double INFLATE = 0.02;

    private static @Nullable Target target;

    private ContainerHighlight() {
    }

    public static void init() {
        WorldVfx.register(WhereDidIPutIt.id("container_highlight"), ContainerHighlight::render);
        ClientPlayerEvents.LEAVE.register(player -> target = null);
    }

    /** Outlines the container at {@code pos} (both halves of a double chest) in the player's level. */
    public static void show(Level level, BlockPos pos) {
        long now = Util.getMillis();
        long duration = WDIPIClientConfig.HIGHLIGHT_DURATION.get() * 1000L;
        target = new Target(level.dimension(), pos.immutable(), bounds(level, pos), now, now + duration);
    }

    /** The container being pointed out right now, if any, in the given level. */
    public static @Nullable Target current(Level level) {
        Target current = target;
        if (current == null || current.dimension != level.dimension() || Util.getMillis() >= current.end) return null;
        return current;
    }

    private static void render(VfxFrame frame) {
        Target current = current(frame.level());
        if (current == null) return;
        float alpha = current.alpha(Util.getMillis());
        // A slow breath, so it catches the eye.
        alpha *= 0.85F + 0.15F * Mth.sin(frame.time() * 5.0F);
        int color = ARGB.color(Math.round(alpha * 255.0F), WDIPIClientConfig.HIGHLIGHT_COLOR.get() & 0xFFFFFF);
        Vec3 camera = frame.cameraPosition();
        // The glow's half width; the solid line is a quarter of it (OUTLINE_CORE in outline.glsl). Both keep about the
        // same size on the screen however far away the container is.
        double glow = Math.max(0.12, current.box.getCenter().distanceTo(camera) * 0.018);

        Vec3[] corners = new Vec3[8];
        for (int i = 0; i < 8; i++) {
            corners[i] = new Vec3((i & 1) == 0 ? current.box.minX : current.box.maxX, (i & 2) == 0 ? current.box.minY : current.box.maxY,
                    (i & 4) == 0 ? current.box.minZ : current.box.maxZ);
        }
        VfxDraw lines = frame.draw(LINE);
        // Each edge joins two corners that differ in one axis bit.
        for (int i = 0; i < 8; i++) {
            for (int bit = 1; bit <= 4; bit <<= 1) {
                if ((i & bit) == 0) edge(lines, camera, corners[i], corners[i | bit], glow, color);
            }
        }
        VfxDraw dots = frame.draw(DOT);
        for (Vec3 corner : corners) {
            dots.billboard(corner.x, corner.y, corner.z, (float) glow, 0.0F, color);
        }
    }

    // An edge in three pieces: the glow fading in from one corner, the middle, and fading out into the other corner.
    private static void edge(VfxDraw draw, Vec3 camera, Vec3 from, Vec3 to, double glow, int color) {
        Vec3 along = to.subtract(from);
        double length = along.length();
        Vec3 dir = along.scale(1.0 / length);
        // Turned to face the camera: sideways is across both the edge and the line of sight.
        Vec3 side = dir.cross(from.add(to).scale(0.5).subtract(camera));
        if (side.lengthSqr() < 1.0E-8) return; // Seen end on: the corner disc stands in for it.
        side = side.normalize().scale(glow);
        double fade = Math.min(glow, length / 2.0);
        Vec3 fadeIn = from.add(dir.scale(fade));
        Vec3 fadeOut = to.subtract(dir.scale(fade));
        piece(draw, side, from, fadeIn, 0.0F, 1.0F, color);
        piece(draw, side, fadeIn, fadeOut, 1.0F, 1.0F, color);
        piece(draw, side, fadeOut, to, 1.0F, 0.0F, color);
    }

    // A quad from a to b, as wide as the glow; u (the glow's ramp) goes from u0 at a to u1 at b, v runs across.
    private static void piece(VfxDraw draw, Vec3 side, Vec3 a, Vec3 b, float u0, float u1, int color) {
        draw.quad(a.add(side), a.subtract(side), b.subtract(side), b.add(side), u0, 0.0F, u1, 1.0F, color);
    }

    private static AABB bounds(Level level, BlockPos pos) {
        AABB box = shape(level, pos);
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            box = box.minmax(shape(level, pos.relative(ChestBlock.getConnectedDirection(state))));
        }
        return box.inflate(INFLATE);
    }

    // The block's own outline where it is loaded, else a full block.
    private static AABB shape(Level level, BlockPos pos) {
        if (level.isLoaded(pos)) {
            VoxelShape shape = level.getBlockState(pos).getShape(level, pos);
            if (!shape.isEmpty()) return shape.bounds().move(pos);
        }
        return new AABB(pos);
    }

    /**
     * @param pos the container's remembered position
     * @param box what is outlined, in world coordinates
     */
    public record Target(ResourceKey<Level> dimension, BlockPos pos, AABB box, long start, long end) {
        /** How strongly it shows at {@code now}: fading in, then out at the end. */
        public float alpha(long now) {
            float in = Mth.clamp((now - this.start) / (float) FADE_IN_MILLIS, 0.0F, 1.0F);
            float out = Mth.clamp((this.end - now) / (float) FADE_OUT_MILLIS, 0.0F, 1.0F);
            return Math.min(in, out);
        }
    }
}
