package net.ixdarklord.wheredidiputit.client.render;

import net.ixdarklord.coolcatcanvas.api.client.vfx.GhostBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Found containers' ghosts: their blocks drawn by the game's own renderers, with the player's own camera, into a picture
 * of their own, then laid over the world at one opacity wherever the world is in front of them. Drawn opaque first, a
 * model shows only its outside, as the real block does: faces drawn see-through one by one showed its inside and far
 * faces (the author, 2026-10-21). Drawn with the world's own projection, a ghost is in perspective from every side,
 * which a picture of the block from one direction was not (a CoolCatLib scene viewport, tried and dropped the same day).
 * Chests and other block entities' models come out as the game draws them. They are unshaded, every face full bright,
 * so they stand out through walls.
 * <p>
 * On 26.3 the drawing is CoolCatLib's {@link GhostBlocks} (this mod's own renderer, moved there when CoolCatLib was
 * ported to 26.3's renderer): {@link GhostBlocks.Show#THROUGH_WALLS} draws each block a hair larger than itself, so
 * where the container is in plain sight its ghost is just in front of it and the real block shows.
 */
public final class ContainerGhost {
    private ContainerGhost() {
    }

    /** A container's ghost: its blocks as they were when it was found. */
    public record Ghost(List<Placed> blocks) {
    }

    public record Placed(BlockPos pos, BlockState state) {
    }

    /** Forgets the block models made so far, so a find after the game's models were reloaded draws the new ones. */
    public static void clearModels() {
        GhostBlocks.clearModels();
    }

    /** The ghost of the blocks at {@code positions}, as they are now; null when none of them is loaded. */
    public static @Nullable Ghost build(Level level, List<BlockPos> positions) {
        List<Placed> blocks = new ArrayList<>();
        for (BlockPos pos : positions) {
            if (level.isLoaded(pos)) blocks.add(new Placed(pos.immutable(), level.getBlockState(pos)));
        }
        return blocks.isEmpty() ? null : new Ghost(List.copyOf(blocks));
    }

    /**
     * Draws the ghosts over the world in {@code color} (their tint and opacity). Called from the world effects pass,
     * right after the level, while its projection and depth are still there.
     */
    public static void draw(List<Ghost> ghosts, int color) {
        if (ghosts.isEmpty() || (color >>> 24) == 0) return;
        List<GhostBlocks.Placed> blocks = new ArrayList<>();
        for (Ghost ghost : ghosts) {
            for (Placed block : ghost.blocks()) blocks.add(new GhostBlocks.Placed(block.pos(), block.state()));
        }
        GhostBlocks.draw(blocks, color, GhostBlocks.Show.THROUGH_WALLS, false);
    }
}
