package net.ixdarklord.wheredidiputit.find;

import net.ixdarklord.coolcatcore.api.event.v2.client.ItemTooltipEvents;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.ixdarklord.wheredidiputit.memory.ContainerMemory;
import net.ixdarklord.wheredidiputit.memory.ContainerRecord;
import net.ixdarklord.wheredidiputit.memory.ContainerWatcher;
import net.ixdarklord.wheredidiputit.memory.DimensionMemory;
import net.ixdarklord.wheredidiputit.memory.StoredItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A hint for keeping like with like: with a remembered container open, an item's tooltip says when a container nearby
 * holds more of it ("Most of these are in Chest at 3, -60, 0 (63)"), so a stray stack can join the rest
 * ({@code memory.sortingHints}). The answer is kept until the item, the open container or the memory changes.
 */
public final class SortingHints {
    /** How far from the open container another one counts as nearby, in blocks. */
    private static final double NEARBY = 16.0;

    private static @Nullable Answer last;

    private SortingHints() {
    }

    public static void init() {
        ItemTooltipEvents.MODIFY.register(SortingHints::modify);
    }

    private static void modify(ItemStack stack, Item.TooltipContext context, TooltipFlag flag, List<Component> lines) {
        Minecraft minecraft = Minecraft.getInstance();
        if (stack.isEmpty() || minecraft.level == null || minecraft.player == null || !WDIPIClientConfig.ENABLED.get() || !WDIPIClientConfig.SORTING_HINTS.get()) return;
        if (!(minecraft.screen instanceof AbstractContainerScreen<?> screen)) return;
        BlockPos open = ContainerWatcher.openContainer(screen);
        DimensionMemory memory = ContainerMemory.dimension(minecraft.level.dimension());
        if (open == null || memory == null) return;

        Answer answer = last;
        if (answer == null || !answer.open.equals(open) || answer.version != memory.version() || !ItemStack.isSameItemSameComponents(answer.stack, stack)) {
            answer = answer(memory, open, stack, here(screen, stack, minecraft.player.getInventory()));
            last = answer;
        }
        if (answer.more == null) return;
        BlockPos pos = answer.more.pos();
        lines.add(Component.translatable("tooltip.wheredidiputit.sorting", FindMessages.name(answer.more), pos.getX(), pos.getY(), pos.getZ(), answer.count)
                .withStyle(ChatFormatting.DARK_AQUA));
    }

    // How many of the item the open container holds now (its own slots, not the player's).
    private static long here(AbstractContainerScreen<?> screen, ItemStack stack, Inventory inventory) {
        long count = 0;
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container != inventory && ItemStack.isSameItemSameComponents(slot.getItem(), stack)) count += slot.getItem().getCount();
        }
        return count;
    }

    // The nearby container holding the most of the item, if it holds more than the open one.
    private static Answer answer(DimensionMemory memory, BlockPos open, ItemStack stack, long here) {
        ContainerRecord most = null;
        long mostCount = here;
        Vec3 from = Vec3.atCenterOf(open);
        for (ContainerRecord record : memory.records()) {
            if (record.ignored() || record.pos().equals(open) || Vec3.atCenterOf(record.pos()).distanceTo(from) > NEARBY) continue;
            long count = 0;
            for (StoredItem item : record.items()) {
                if (ItemStack.isSameItemSameComponents(item.stack(), stack)) count += item.count();
            }
            if (count > mostCount) {
                most = record;
                mostCount = count;
            }
        }
        return new Answer(stack.copyWithCount(1), open, memory.version(), most, mostCount);
    }

    private record Answer(ItemStack stack, BlockPos open, int version, @Nullable ContainerRecord more, long count) {
    }
}
