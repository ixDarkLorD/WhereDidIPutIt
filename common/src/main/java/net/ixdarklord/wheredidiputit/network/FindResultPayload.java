package net.ixdarklord.wheredidiputit.network;

import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.minecraft.core.BlockPos;
import net.ixdarklord.coolcatcore.api.network.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Server to client: the containers round the player that hold the item searched for. Only where they are and how many
 * of the matching item each holds, never what else is in them.
 *
 * @param requestId the request this answers
 * @param status    whether the search ran, and why not
 * @param radius    how far round the player the server searched, in blocks
 * @param hits      the containers found, nearest first
 */
public record FindResultPayload(int requestId, Status status, int radius, List<Hit> hits) implements CustomPacketPayload {
    public static final Type<FindResultPayload> TYPE = new Type<>(WhereDidIPutIt.id("find_result"));

    @Override
    public Type<FindResultPayload> type() {
        return TYPE;
    }

    public enum Status {
        OK,
        /** The server doesn't allow live search: the client uses its memory. */
        DISABLED,
        /** Asked again too soon: the client uses its memory this time. */
        COOLDOWN
    }

    /**
     * @param pos   the container's position, a double chest's lower half
     * @param block its block's id
     * @param count how many of the matching item it holds
     * @param shown the matching item it holds most of, a stack of one
     */
    public record Hit(BlockPos pos, ResourceLocation block, long count, ItemStack shown) {
    }
}
