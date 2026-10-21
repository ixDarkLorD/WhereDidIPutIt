package net.ixdarklord.wheredidiputit.find;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * A walking route to a container through the loaded blocks the client knows, for the path line: A* over the places a
 * player can stand (two blocks of room above something solid, or water), stepping up one block, dropping down three,
 * and cutting corners where both sides are open. It gives up after {@link #MAX_NODES} places, so a far or walled-off
 * container gets no line rather than a pause.
 */
public final class PathFinder {
    /** How many places are looked at before giving up. */
    private static final int MAX_NODES = 8000;
    /** How far a route may drop in one step. */
    private static final int MAX_DROP = 3;
    /** Close enough to reach into the container: within this of its box. */
    private static final double REACH = 1.6;

    private PathFinder() {
    }

    /** The places to walk through, from the player's feet to beside the container, or null when none was found. */
    public static @Nullable List<BlockPos> find(Level level, BlockPos from, AABB target) {
        BlockPos start = standable(level, from) ? from : standable(level, from.below()) ? from.below() : null;
        if (start == null) return null;
        AABB goal = target.inflate(REACH);
        Vec3 goalCenter = target.getCenter();
        PriorityQueue<Node> open = new PriorityQueue<>((a, b) -> Double.compare(a.cost + a.estimate, b.cost + b.estimate));
        Map<BlockPos, Node> seen = new HashMap<>();
        Node first = new Node(start, null, 0.0, estimate(start, goalCenter));
        open.add(first);
        seen.put(start, first);
        int visited = 0;
        while (!open.isEmpty() && visited++ < MAX_NODES) {
            Node node = open.poll();
            if (node.closed) continue;
            node.closed = true;
            if (goal.contains(Vec3.atBottomCenterOf(node.pos)) || goal.contains(Vec3.atBottomCenterOf(node.pos).add(0.0, 1.6, 0.0))) return route(node);
            for (Step step : steps(level, node.pos)) {
                double cost = node.cost + step.cost;
                Node known = seen.get(step.pos);
                if (known != null && (known.closed || known.cost <= cost)) continue;
                Node next = new Node(step.pos, node, cost, estimate(step.pos, goalCenter));
                seen.put(step.pos, next);
                open.add(next);
            }
        }
        return null;
    }

    private static List<BlockPos> route(Node end) {
        List<BlockPos> route = new ArrayList<>();
        for (Node node = end; node != null; node = node.parent) route.add(node.pos);
        Collections.reverse(route);
        return route;
    }

    private static double estimate(BlockPos pos, Vec3 goal) {
        return Vec3.atBottomCenterOf(pos).distanceTo(goal);
    }

    // Where one can go from a place: the four sides and the corners between them, each a step up, level, or a drop.
    private static List<Step> steps(Level level, BlockPos from) {
        List<Step> steps = new ArrayList<>(8);
        boolean headroom = open(level, from.above(2));
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = from.relative(direction);
            add(level, steps, side, 1.0, headroom);
        }
        for (Direction a : new Direction[]{Direction.NORTH, Direction.SOUTH}) {
            for (Direction b : new Direction[]{Direction.EAST, Direction.WEST}) {
                BlockPos corner = from.relative(a).relative(b);
                // Only round a corner with room on both sides of it, as a player can't squeeze between two blocks.
                if (clear(level, from.relative(a)) && clear(level, from.relative(b))) {
                    add(level, steps, corner, Math.sqrt(2.0), headroom);
                }
            }
        }
        return steps;
    }

    private static void add(Level level, List<Step> steps, BlockPos side, double cost, boolean headroom) {
        if (!level.isLoaded(side)) return;
        if (standable(level, side)) {
            steps.add(new Step(side, cost));
        } else if (headroom && standable(level, side.above())) {
            steps.add(new Step(side.above(), cost + 0.5));
        } else if (clear(level, side)) {
            for (int drop = 1; drop <= MAX_DROP; drop++) {
                BlockPos below = side.below(drop);
                if (standable(level, below)) {
                    steps.add(new Step(below, cost + drop * 0.25));
                    break;
                }
                if (!open(level, below)) break;
            }
        }
    }

    // Two blocks of room, on something to stand on (or in water).
    private static boolean standable(Level level, BlockPos pos) {
        if (!clear(level, pos)) return false;
        if (level.getFluidState(pos).is(FluidTags.WATER)) return true;
        BlockState below = level.getBlockState(pos.below());
        return !below.getCollisionShape(level, pos.below()).isEmpty();
    }

    // Room for a player: the block and the one above it can be walked through.
    private static boolean clear(Level level, BlockPos pos) {
        return open(level, pos) && open(level, pos.above());
    }

    private static boolean open(Level level, BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && !level.getFluidState(pos).is(FluidTags.LAVA);
    }

    private record Step(BlockPos pos, double cost) {
    }

    private static final class Node {
        private final BlockPos pos;
        private final @Nullable Node parent;
        private final double cost;
        private final double estimate;
        private boolean closed;

        private Node(BlockPos pos, @Nullable Node parent, double cost, double estimate) {
            this.pos = pos;
            this.parent = parent;
            this.cost = cost;
            this.estimate = estimate;
        }
    }
}
