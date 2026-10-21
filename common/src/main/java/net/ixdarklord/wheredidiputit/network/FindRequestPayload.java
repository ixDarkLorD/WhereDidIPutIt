package net.ixdarklord.wheredidiputit.network;

import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.ixdarklord.wheredidiputit.config.MatchMode;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Client to server: search the containers round me for this item. The server decides how far, how often and in which
 * containers; the client only says what it is looking for and how alike a match must be.
 *
 * @param requestId   echoed in the answer, so a late answer to an older search is known
 * @param query       the item searched for
 * @param mode        how alike a stack must be
 * @param nested      whether the client would like shulker boxes and bundles looked into (the server may say no)
 * @param ignoredTags the client's tags the item tag mode leaves out (client settings don't exist on the server)
 */
public record FindRequestPayload(int requestId, ItemStack query, MatchMode mode, boolean nested, List<String> ignoredTags)
        implements CustomPacketPayload {
    public static final Type<FindRequestPayload> TYPE = new Type<>(WhereDidIPutIt.id("find_request"));
    /** More ignored tags than this, or longer ones, and the request is refused unread. */
    public static final int MAX_IGNORED_TAGS = 128;
    public static final int MAX_TAG_LENGTH = 128;

    @Override
    public Type<FindRequestPayload> type() {
        return TYPE;
    }

    /** Whether the request is within the size the server accepts. */
    public boolean withinLimits() {
        return this.ignoredTags.size() <= MAX_IGNORED_TAGS && this.ignoredTags.stream().allMatch(tag -> tag.length() <= MAX_TAG_LENGTH);
    }
}
