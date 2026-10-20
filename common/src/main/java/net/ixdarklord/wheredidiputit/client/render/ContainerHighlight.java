package net.ixdarklord.wheredidiputit.client.render;

import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxDraw;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxFrame;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxMaterial;
import net.ixdarklord.coolcatcanvas.api.client.vfx.WorldVfx;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The containers a search found, pointed out for the highlight duration: a glowing outline round each, seen through
 * walls (dimmer where blocks are in the way), and a ghost of the container itself wherever something hides it. The
 * item and its amount are drawn over each by {@code ItemMarker}, the arrows by {@code TargetArrow}. Its own shaders do
 * the seeing through walls: CoolCatLib's world effects hide behind solid things.
 * <p>
 * The outline ({@code vfx/outline_line}, {@code vfx/outline_dot}, sharing {@code include/outline.glsl}): each edge is
 * one quad turned to the camera, as wide as the glow, with the solid line down its middle; each corner is a disc of the
 * solid line only, so corners are round. The line is opaque and each edge's line stops where the corner disc begins,
 * so it never gets brighter where pieces meet. Each edge's glow is a capsule round it, running past the corners with
 * a round cap, so the glow rounds each corner instead of being cut there (the corner discs' own glow showed as
 * circles, 2026-10-21).
 * <p>
 * The ghost ({@code vfx/ghost}): the block's own model faces, each in its sprite's texture ({@link ContainerGhost}),
 * the ones turned to the camera, drawn only where they are hidden.
 */
public final class ContainerHighlight {
    private static final VfxMaterial LINE = VfxMaterial.alpha(VfxMaterial.GLOW).withSoftness(0.0F).withShader(WhereDidIPutIt.id("vfx/outline_line"));
    private static final VfxMaterial DOT = VfxMaterial.alpha(VfxMaterial.GLOW).withSoftness(0.0F).withShader(WhereDidIPutIt.id("vfx/outline_dot"));
    private static final Identifier GHOST_SHADER = WhereDidIPutIt.id("vfx/ghost");
    // One ghost material per texture: a material is a texture, a blend and a shader.
    private static final Map<Identifier, VfxMaterial> GHOSTS = new HashMap<>();
    private static final long FADE_IN_MILLIS = 150L;
    private static final long FADE_OUT_MILLIS = 700L;
    // Pushed out from the block a little, so the lines don't sit in its faces and flicker against them.
    private static final double INFLATE = 0.02;
    // How see-through the ghost is, and how much of the highlight color it takes on.
    private static final float GHOST_ALPHA = 0.8F;
    private static final float GHOST_TINT = 0.15F;

    private static List<Target> targets = List.of();

    private ContainerHighlight() {
    }

    public static void init() {
        WorldVfx.register(WhereDidIPutIt.id("container_highlight"), ContainerHighlight::render);
        ClientPlayerEvents.LEAVE.register(player -> targets = List.of());
    }

    /** What to point out: a container's remembered position, the item searched for and how many of it are there. */
    public record Found(BlockPos pos, ItemStack item, long count) {
    }

    /** Points out the given containers in the player's level, replacing what was pointed out before. */
    public static void show(Level level, List<Found> found) {
        long now = Util.getMillis();
        long end = now + WDIPIClientConfig.HIGHLIGHT_DURATION.get() * 1000L;
        List<Target> shown = new ArrayList<>();
        for (Found container : found) {
            List<BlockPos> blocks = blocks(level, container.pos());
            AABB shape = shape(level, blocks);
            List<ContainerGhost.Face> ghost = WDIPIClientConfig.GHOST.get() ? ContainerGhost.build(level, blocks, shape) : List.of();
            shown.add(new Target(level.dimension(), container.pos(), shape.inflate(INFLATE), ghost, container.item().copyWithCount(1),
                    container.count(), now, end));
        }
        targets = List.copyOf(shown);
    }

    /** The containers being pointed out right now in the given level; empty when there are none. */
    public static List<Target> current(Level level) {
        List<Target> current = targets;
        if (current.isEmpty() || current.getFirst().dimension != level.dimension() || Util.getMillis() >= current.getFirst().end) return List.of();
        return current;
    }

    private static void render(VfxFrame frame) {
        List<Target> current = current(frame.level());
        if (current.isEmpty()) return;
        long now = Util.getMillis();
        // A slow breath, so it catches the eye.
        float breath = 0.85F + 0.15F * Mth.sin(frame.time() * 5.0F);
        int rgb = WDIPIClientConfig.HIGHLIGHT_COLOR.get() & 0xFFFFFF;
        int ghostRgb = ARGB.srgbLerp(GHOST_TINT, 0xFFFFFFFF, 0xFF000000 | rgb) & 0xFFFFFF;
        Vec3 camera = frame.cameraPosition();
        VfxDraw lines = frame.draw(LINE);
        VfxDraw dots = frame.draw(DOT);
        for (Target target : current) {
            float alpha = target.alpha(now);
            drawGhost(frame, camera, target.ghost, ARGB.color(Math.round(alpha * GHOST_ALPHA * 255.0F), ghostRgb));
            drawOutline(lines, dots, camera, target.box, ARGB.color(Math.round(alpha * breath * 255.0F), rgb));
        }
    }

    private static void drawGhost(VfxFrame frame, Vec3 camera, List<ContainerGhost.Face> faces, int color) {
        for (ContainerGhost.Face face : faces) {
            // Only the faces turned to the camera: the far ones would show through the near ones.
            if (face.normal().dot(camera.subtract(face.center())) <= 0.0) continue;
            VfxMaterial material = GHOSTS.computeIfAbsent(face.texture(), texture -> VfxMaterial.alpha(texture).withSoftness(0.0F).withShader(GHOST_SHADER));
            frame.draw(material).quad(face.a(), face.b(), face.c(), face.d(), face.u0(), face.v0(), face.u1(), face.v1(), color);
        }
    }

    private static void drawOutline(VfxDraw lines, VfxDraw dots, Vec3 camera, AABB box, int color) {
        // The glow's half width; the solid line is a quarter of it (OUTLINE_CORE in outline.glsl). Both keep about the
        // same size on the screen however far away the container is.
        double glow = Math.max(0.12, box.getCenter().distanceTo(camera) * 0.018);
        Vec3[] corners = new Vec3[8];
        for (int i = 0; i < 8; i++) {
            corners[i] = new Vec3((i & 1) == 0 ? box.minX : box.maxX, (i & 2) == 0 ? box.minY : box.maxY, (i & 4) == 0 ? box.minZ : box.maxZ);
        }
        // Each edge joins two corners that differ in one axis bit.
        for (int i = 0; i < 8; i++) {
            for (int bit = 1; bit <= 4; bit <<= 1) {
                if ((i & bit) == 0) edge(lines, camera, corners[i], corners[i | bit], glow, color);
            }
        }
        for (Vec3 corner : corners) {
            dots.billboard(corner.x, corner.y, corner.z, (float) glow, 0.0F, color);
        }
    }

    // An edge in five pieces, u measured in glow half widths from the nearest corner (see outline_line.fsh): the round
    // cap of the glow past each corner, the stretch inside each corner where the line meets the corner dot, and the
    // middle.
    private static void edge(VfxDraw draw, Vec3 camera, Vec3 from, Vec3 to, double glow, int color) {
        Vec3 along = to.subtract(from);
        double length = along.length();
        Vec3 dir = along.scale(1.0 / length);
        // Turned to face the camera: sideways is across both the edge and the line of sight.
        Vec3 side = dir.cross(from.add(to).scale(0.5).subtract(camera));
        if (side.lengthSqr() < 1.0E-8) return; // Seen end on: the corner disc stands in for it.
        side = side.normalize().scale(glow);
        Vec3 cap = dir.scale(glow);
        double inner = Math.min(glow, length / 2.0);
        float innerU = (float) (-inner / glow);
        Vec3 fromInner = from.add(dir.scale(inner));
        Vec3 toInner = to.subtract(dir.scale(inner));
        piece(draw, side, from.subtract(cap), from, 1.0F, 0.0F, color);
        piece(draw, side, from, fromInner, 0.0F, innerU, color);
        piece(draw, side, fromInner, toInner, innerU, innerU, color);
        piece(draw, side, toInner, to, innerU, 0.0F, color);
        piece(draw, side, to, to.add(cap), 0.0F, 1.0F, color);
    }

    // A quad from a to b, as wide as the glow; u goes from u0 at a to u1 at b, v runs across.
    private static void piece(VfxDraw draw, Vec3 side, Vec3 a, Vec3 b, float u0, float u1, int color) {
        draw.quad(a.add(side), a.subtract(side), b.subtract(side), b.add(side), u0, 0.0F, u1, 1.0F, color);
    }

    // The container's blocks: both halves of a double chest.
    private static List<BlockPos> blocks(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (level.isLoaded(pos) && state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            return List.of(pos, pos.relative(ChestBlock.getConnectedDirection(state)));
        }
        return List.of(pos);
    }

    // The blocks' own outline where they are loaded, else full blocks.
    private static AABB shape(Level level, List<BlockPos> blocks) {
        AABB box = null;
        for (BlockPos pos : blocks) {
            AABB part = new AABB(pos);
            if (level.isLoaded(pos)) {
                VoxelShape shape = level.getBlockState(pos).getShape(level, pos);
                if (!shape.isEmpty()) part = shape.bounds().move(pos);
            }
            box = box == null ? part : box.minmax(part);
        }
        return box;
    }

    /**
     * @param pos   the container's remembered position
     * @param box   what is outlined, in world coordinates
     * @param ghost the faces of its ghost
     * @param item  the item searched for, a stack of one
     * @param count how many of it the container held
     */
    public record Target(ResourceKey<Level> dimension, BlockPos pos, AABB box, List<ContainerGhost.Face> ghost, ItemStack item, long count,
                         long start, long end) {
        /** How strongly it shows at {@code now}: fading in, then out at the end. */
        public float alpha(long now) {
            float in = Mth.clamp((now - this.start) / (float) FADE_IN_MILLIS, 0.0F, 1.0F);
            float out = Mth.clamp((this.end - now) / (float) FADE_OUT_MILLIS, 0.0F, 1.0F);
            return Math.min(in, out);
        }
    }
}
