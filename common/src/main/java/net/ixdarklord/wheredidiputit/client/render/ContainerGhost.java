package net.ixdarklord.wheredidiputit.client.render;

import net.ixdarklord.coolcatcanvas.api.client.viewport.SceneViewport;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxFrame;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxMaterial;
import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A found container's ghost: its blocks drawn by the game's own renderers into a picture of their own (a CoolCatLib
 * {@link SceneViewport}), seen from where the player stands, and that picture laid over the container in the world at
 * one opacity. Drawing the picture opaque first is what keeps it looking like the block: the picture's depth buffer
 * hides a model's inner and far faces as on the real block, which faces drawn see-through one by one each showed (the
 * author, 2026-10-21). It also draws chests and other block entities' models as the game does.
 * <p>
 * The picture is orthographic, looking from the camera at the container's middle, and is laid on a quad turned to the
 * camera at the container's nearest point, sized so it covers what the container covers on the screen. So where the
 * container is in plain sight it is behind the quad, and {@code vfx/ghost} draws nothing there.
 */
public final class ContainerGhost {
    /** The pictures' size in pixels. */
    private static final int SIZE = 512;
    /** How many containers get a ghost at once; the farther ones of a larger find have only their outline. */
    private static final int MAX = 16;
    /** The picture is a little wider than the container from any side, so nothing of it is cut off. */
    private static final double MARGIN = 1.04;
    /** Closer than this to its quad, the ghost isn't drawn: the player is all but inside the container. */
    private static final double MIN_NEAR = 0.05;
    private static final Identifier SHADER = WhereDidIPutIt.id("vfx/ghost");

    // Made as they are first needed and kept for later finds, one picture each.
    private static final List<SceneViewport> VIEWPORTS = new ArrayList<>();
    private static final Map<Identifier, VfxMaterial> MATERIALS = new HashMap<>();

    private ContainerGhost() {
    }

    /**
     * A container's ghost: the picture it is drawn in, the box it covers in the world, and how much of the scene the
     * picture spans.
     */
    public record Ghost(int viewport, AABB box, double span) {
    }

    /**
     * Sets up the {@code index}th picture with the blocks at {@code positions} as they are now; null when there are
     * already {@link #MAX} or none of the blocks is loaded.
     */
    public static @Nullable Ghost build(Level level, int index, List<BlockPos> positions, AABB shape) {
        if (index >= MAX || positions.isEmpty()) return null;
        while (VIEWPORTS.size() <= index) {
            VIEWPORTS.add(SceneViewport.create(WhereDidIPutIt.id("ghost_" + VIEWPORTS.size()), SIZE, SIZE));
        }
        SceneViewport viewport = VIEWPORTS.get(index).clear();
        // The scene round the first block, so its numbers stay small (the scene is drawn in floats) far from 0, 0.
        BlockPos origin = positions.getFirst();
        boolean any = false;
        for (BlockPos pos : positions) {
            if (!level.isLoaded(pos)) continue;
            viewport.block(pos.subtract(origin), level.getBlockState(pos));
            any = true;
        }
        if (!any) return null;
        // Any side of a box fits in its diagonal.
        double span = Math.sqrt(shape.getXsize() * shape.getXsize() + shape.getYsize() * shape.getYsize() + shape.getZsize() * shape.getZsize()) * MARGIN;
        Vec3 centre = shape.getCenter();
        viewport.frame(span, centre.x - origin.getX(), centre.y - origin.getY(), centre.z - origin.getZ());
        return new Ghost(index, shape, span);
    }

    /** Turns the ghost's picture to the camera and lays it over the container, in {@code color} (its tint and opacity). */
    public static void draw(VfxFrame frame, Vec3 camera, Ghost ghost, int color) {
        SceneViewport viewport = VIEWPORTS.get(ghost.viewport());
        viewport.use();
        Vec3 centre = ghost.box().getCenter();
        Vec3 toCamera = camera.subtract(centre);
        double distance = toCamera.length();
        if (distance < 1.0E-3) return;
        Vec3 u = toCamera.scale(1.0 / distance);
        // The viewport's camera stands at (-cos p sin y, sin p, cos p cos y) from the scene's middle (its view()).
        double pitch = Math.asin(Math.clamp(u.y, -1.0, 1.0));
        double yaw = Math.atan2(-u.x, u.z);
        // Drawn at the start of the next frame: the picture is a frame behind the camera, too little to see.
        viewport.camera((float) Math.toDegrees(yaw), (float) Math.toDegrees(pitch));
        if (!viewport.isReady()) return;

        // How far the box reaches towards the camera from its middle, and how far that nearest point is.
        AABB box = ghost.box();
        double reach = Math.abs(u.x) * box.getXsize() / 2.0 + Math.abs(u.y) * box.getYsize() / 2.0 + Math.abs(u.z) * box.getZsize() / 2.0;
        double near = distance - reach;
        if (near < MIN_NEAR) return;
        // The quad sits at the nearest point but covers on the screen what the picture's span covers at the middle.
        double half = ghost.span() / 2.0 * near / distance;
        Vec3 at = centre.add(u.scale(reach));
        // The picture's right and up, as the viewport turns the scene.
        Vec3 right = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw)).scale(half);
        Vec3 up = new Vec3(Math.sin(pitch) * Math.sin(yaw), Math.cos(pitch), -Math.sin(pitch) * Math.cos(yaw)).scale(half);
        VfxMaterial material = MATERIALS.computeIfAbsent(viewport.texture(), texture -> VfxMaterial.alpha(texture).withSoftness(0.0F).withShader(SHADER));
        // The picture is upside down (drawn-to textures are): v runs up it.
        Vec3 bottomLeft = at.subtract(right).subtract(up);
        Vec3 topLeft = at.subtract(right).add(up);
        Vec3 topRight = at.add(right).add(up);
        Vec3 bottomRight = at.add(right).subtract(up);
        frame.draw(material).quad(bottomLeft, topLeft, topRight, bottomRight, 0.0F, 0.0F, 1.0F, 1.0F, color);
    }
}
