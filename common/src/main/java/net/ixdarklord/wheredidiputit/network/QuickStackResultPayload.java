package net.ixdarklord.wheredidiputit.network;

import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.minecraft.core.BlockPos;
import net.ixdarklord.coolcatcore.api.network.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Server to client: what quick stack moved, so the client can point out where things went and keep its memory of
 * those containers up to date. Only the player's own items are named, never what else a container holds.
 *
 * @param requestId the request this answers
 * @param status    whether it ran, and why not
 * @param moved     each item put into each container, nearest first
 */
public record QuickStackResultPayload(int requestId, Status status, List<Moved> moved) implements CustomPacketPayload {
    public static final Type<QuickStackResultPayload> TYPE = new Type<>(WhereDidIPutIt.id("quick_stack_result"));

    @Override
    public Type<QuickStackResultPayload> type() {
        return TYPE;
    }

    public enum Status {
        OK,
        /** The server doesn't allow quick stack. */
        DISABLED,
        /** Asked again too soon. */
        COOLDOWN
    }

    /**
     * @param pos   the container's position, a double chest's lower half
     * @param item  what was put in, a stack of one
     * @param count how many
     */
    public record Moved(BlockPos pos, ItemStack item, int count) {
    }
}
