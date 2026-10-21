package net.ixdarklord.wheredidiputit.memory;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.jetbrains.annotations.Nullable;

/** Where a container is remembered, the same on the client (memory) and the server (live search). */
public final class ContainerKeys {
    private ContainerKeys() {
    }

    /**
     * The position a container is remembered under, or {@code null} if the block there isn't one. A double chest is one
     * container, remembered under the lower of its two halves' positions so either half finds the same record.
     */
    public static @Nullable BlockPos keyOf(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        // An ender chest's contents are the player's own wherever it is opened, not stored in the block.
        if (blockEntity == null || blockEntity instanceof EnderChestBlockEntity) return null;
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos other = pos.relative(ChestBlock.getConnectedDirection(state));
            if (level.getBlockState(other).is(state.getBlock())) return pos.compareTo(other) <= 0 ? pos.immutable() : other;
        }
        return pos.immutable();
    }
}
