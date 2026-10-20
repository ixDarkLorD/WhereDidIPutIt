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
 * for the highlight duration. Its own shader ({@code vfx/outline}) does that: CoolCatLib's world effects hide behind
 * solid things.
 */
public final class ContainerHighlight {
    private static final VfxMaterial OUTLINE = VfxMaterial.alpha(VfxMaterial.GLOW).withSoftness(0.0F).withShader(WhereDidIPutIt.id("vfx/outline"));
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
        int rgb = WDIPIClientConfig.HIGHLIGHT_COLOR.get() & 0xFFFFFF;
        // Lines keep about the same width on the screen however far away the container is.
        double distance = current.box.getCenter().distanceTo(frame.cameraPosition());
        float width = (float) Math.max(0.08, distance * 0.012);
        VfxDraw draw = frame.draw(OUTLINE);
        // A wide faint halo first, then the line itself.
        drawBox(draw, current.box, width * 4.0F, ARGB.color(Math.round(alpha * 70.0F), rgb));
        drawBox(draw, current.box, width, ARGB.color(Math.round(alpha * 255.0F), rgb));
    }

    private static void drawBox(VfxDraw draw, AABB box, float width, int color) {
        Vec3[] corners = new Vec3[8];
        for (int i = 0; i < 8; i++) {
            corners[i] = new Vec3((i & 1) == 0 ? box.minX : box.maxX, (i & 2) == 0 ? box.minY : box.maxY, (i & 4) == 0 ? box.minZ : box.maxZ);
        }
        // Each edge joins two corners that differ in one axis bit. A beam is a flat ribbon turned to the camera that
        // ends square at its points, so two meeting at a corner leave a notch there: each runs half its width past
        // both corners, and the ribbons overlap into a closed corner.
        for (int i = 0; i < 8; i++) {
            for (int bit = 1; bit <= 4; bit <<= 1) {
                if ((i & bit) != 0) continue;
                Vec3 from = corners[i];
                Vec3 to = corners[i | bit];
                Vec3 overhang = to.subtract(from).normalize().scale(width / 2.0F);
                draw.beam(from.subtract(overhang), to.add(overhang), width, color);
            }
        }
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
