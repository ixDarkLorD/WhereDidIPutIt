package net.ixdarklord.wheredidiputit.find;

import net.ixdarklord.coolcatcore.api.client.registry.KeyMappingRegistry;
import net.ixdarklord.coolcatcore.api.event.v2.client.ItemTooltipEvents;
import net.ixdarklord.wheredidiputit.config.MatchMode;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.ixdarklord.wheredidiputit.memory.ContainerMemory;
import net.ixdarklord.wheredidiputit.memory.ContainerRecord;
import net.ixdarklord.wheredidiputit.memory.DimensionMemory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;

/**
 * The tooltip line saying where an item was last seen: "Last seen in: Chest at 120, 64, -30 (412 in 3 containers)",
 * the container that most recently held it in the player's dimension and how many of it are stored there in all,
 * while the tooltip key (Shift) is held. Without the
 * key an item that is remembered somewhere gets a hint to hold it. Searching every tooltip frame would be wasteful, so
 * the last answer is kept until the item, the settings or the remembered containers change.
 */
public final class LastSeenTooltip {
    public static final HeldKeyMapping KEY = new HeldKeyMapping("key.wheredidiputit.last_seen", GLFW.GLFW_KEY_LEFT_SHIFT, FindKey.CATEGORY);

    private static @Nullable Answer last;

    private LastSeenTooltip() {
    }

    public static void init() {
        KeyMappingRegistry.register(KEY);
        ItemTooltipEvents.MODIFY.register(LastSeenTooltip::modify);
    }

    private static void modify(ItemStack stack, Item.TooltipContext context, TooltipFlag flag, List<Component> lines) {
        Minecraft minecraft = Minecraft.getInstance();
        if (stack.isEmpty() || minecraft.level == null || !WDIPIClientConfig.ENABLED.get() || !WDIPIClientConfig.LAST_SEEN_TOOLTIP.get()) return;
        DimensionMemory memory = ContainerMemory.dimension(minecraft.level.dimension());
        if (memory == null) return;

        Answer answer = answer(memory, stack);
        if (!KEY.isHeld()) {
            if (answer.latest != null) lines.add(Component.translatable("tooltip.wheredidiputit.hold", KEY.getTranslatedKeyMessage()).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (answer.latest == null) {
            lines.add(Component.translatable("tooltip.wheredidiputit.never_seen").withStyle(ChatFormatting.GRAY));
            return;
        }
        BlockPos pos = answer.latest.pos();
        Component line = Component.translatable("tooltip.wheredidiputit.last_seen", FindMessages.name(answer.latest), pos.getX(), pos.getY(), pos.getZ());
        line = answer.containers > 1
                ? Component.translatable("tooltip.wheredidiputit.last_seen.more", line, answer.total, answer.containers)
                : Component.translatable("tooltip.wheredidiputit.last_seen.count", line, answer.total);
        lines.add(line.copy().withStyle(ChatFormatting.GRAY));
    }

    private static Answer answer(DimensionMemory memory, ItemStack stack) {
        MatchMode mode = WDIPIClientConfig.MATCH_MODE.get();
        boolean nested = WDIPIClientConfig.INDEX_NESTED_CONTENTS.get();
        Answer cached = last;
        if (cached != null && cached.memory == memory && cached.version == memory.version() && cached.mode == mode && cached.nested == nested
                && ItemStack.isSameItemSameComponents(cached.stack, stack)) {
            return cached;
        }
        // Every container in the dimension, however far: the tooltip says where, the find key goes there.
        List<SearchHit> hits = ContainerFinder.search(memory, ItemMatcher.of(stack, mode, nested), Vec3.ZERO, Double.MAX_VALUE);
        ContainerRecord latest = hits.stream().map(SearchHit::record).max(Comparator.comparingLong(ContainerRecord::seenAt)).orElse(null);
        long total = hits.stream().mapToLong(SearchHit::count).sum();
        Answer answer = new Answer(stack.copyWithCount(1), mode, nested, memory, memory.version(), latest, hits.size(), total);
        last = answer;
        return answer;
    }

    private record Answer(ItemStack stack, MatchMode mode, boolean nested, DimensionMemory memory, int version,
                          @Nullable ContainerRecord latest, int containers, long total) {
    }
}
