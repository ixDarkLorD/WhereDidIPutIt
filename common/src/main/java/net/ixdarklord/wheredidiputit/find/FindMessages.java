package net.ixdarklord.wheredidiputit.find;

import net.ixdarklord.wheredidiputit.memory.ContainerRecord;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** The words the mod uses for containers and searches. */
public final class FindMessages {
    private FindMessages() {
    }

    /** What a container is called: its screen's title, else its block's name. */
    public static Component name(ContainerRecord record) {
        return record.label() != null ? record.label() : BuiltInRegistries.BLOCK.getValue(record.block()).getName();
    }

    public static Component notFound(ItemStack query, int maxDistance) {
        return Component.translatable("gui.wheredidiputit.not_found", query.getHoverName(), maxDistance).withStyle(ChatFormatting.RED);
    }
}
