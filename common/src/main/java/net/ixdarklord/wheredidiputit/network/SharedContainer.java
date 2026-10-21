package net.ixdarklord.wheredidiputit.network;

import net.ixdarklord.wheredidiputit.memory.ContainerRecord;
import net.ixdarklord.wheredidiputit.memory.StoredItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A container one player saw, as shared with their team (team memory, a server's opt-in): what a {@link ContainerRecord}
 * holds, less what is the player's own (whether they skip it in searches).
 */
public record SharedContainer(ResourceKey<Level> dimension, BlockPos pos, Identifier block, Optional<Component> label, long seenAt, List<StoredItem> items,
                              int slots, Optional<UUID> entity) {
    /** More stacks than a large chest and a half's worth, and the share is refused unread. */
    public static final int MAX_ITEMS = 256;
    public static final int MAX_LABEL_LENGTH = 256;

    public static SharedContainer of(ResourceKey<Level> dimension, ContainerRecord record) {
        return new SharedContainer(dimension, record.pos(), record.block(), Optional.ofNullable(record.label()), record.seenAt(), record.items(),
                record.slots(), Optional.ofNullable(record.entity()));
    }

    /** As a record of the receiver's memory, keeping whether they skip it in searches. */
    public ContainerRecord toRecord(boolean ignored) {
        return new ContainerRecord(this.pos, this.block, this.label.orElse(null), this.seenAt, this.items, false, ignored, this.slots, this.entity.orElse(null));
    }

    public boolean withinLimits() {
        return this.items.size() <= MAX_ITEMS && this.label.map(label -> label.getString().length() <= MAX_LABEL_LENGTH).orElse(true)
                && this.items.stream().allMatch(item -> item.count() > 0 && !item.stack().isEmpty());
    }
}
