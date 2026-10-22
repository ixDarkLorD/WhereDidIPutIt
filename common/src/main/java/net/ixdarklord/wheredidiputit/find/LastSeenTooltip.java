package net.ixdarklord.wheredidiputit.find;

import net.ixdarklord.coolcatcore.api.client.registry.KeyMappingRegistry;
import net.ixdarklord.coolcatcore.api.event.v2.client.ItemTooltipEvents;
import net.ixdarklord.wheredidiputit.client.hud.ItemMarker;
import net.ixdarklord.wheredidiputit.config.MatchMode;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.ixdarklord.wheredidiputit.memory.ContainerMemory;
import net.ixdarklord.wheredidiputit.memory.ContainerRecord;
import net.ixdarklord.wheredidiputit.memory.DimensionMemory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;

/**
 * The tooltip section saying where an item is stored, while the tooltip key (Shift) is held: how many in how many
 * containers, the nearest of them, and the one that most recently held it in the player's dimension ("Stored: 412 in 3
 * containers", "Nearest: Barrel · 6 blocks away", "Last seen: Chest at 120, 64, -30 · 2m ago"; one "In:" line for a
 * single container), in the shared look of {@link TooltipLines}. An item never seen in a remembered container but found
 * by a recent search ({@link SearchHistory}) says what that search found ("Found: 30 in 2 containers · 3m ago" and the
 * nearest). Any other item gets nothing at all; one that has something to say gets a hint to hold the key. Searching every tooltip frame would be wasteful, so the last answer is kept until the item, the
 * settings or the remembered containers change.
 */
public final class LastSeenTooltip {
    public static final HeldKeyMapping KEY = new HeldKeyMapping("key.wheredidiputit.last_seen", GLFW.GLFW_KEY_LEFT_SHIFT, FindKey.CATEGORY);

    private static @Nullable Answer last;

    private LastSeenTooltip() {
    }

    public static void init() {
        KeyMappingRegistry.register(KEY);
        // At the very end of the tooltip, after every other mod's lines; the sorting hint follows (registered after).
        ItemTooltipEvents.APPEND.register((stack, player, flag, lines) -> append(stack, lines));
    }

    private static void append(ItemStack stack, List<Component> lines) {
        Minecraft minecraft = Minecraft.getInstance();
        if (stack.isEmpty() || minecraft.level == null || !WDIPIClientConfig.ENABLED.get() || !WDIPIClientConfig.LAST_SEEN_TOOLTIP.get()) return;
        DimensionMemory memory = ContainerMemory.dimension(minecraft.level.dimension());
        if (memory == null) return;

        Answer answer = answer(memory, stack);
        // Only items seen in a container, or found by a search, have anything to say.
        SearchHistory.Search search = answer.latest == null ? SearchHistory.find(minecraft.level.dimension(), stack) : null;
        if (answer.latest == null && search == null) return;
        if (!KEY.isHeld()) {
            lines.add(Component.translatable("tooltip.wheredidiputit.hold", KEY.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.GRAY))
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        // A section of its own, set off from the item's own lines.
        lines.add(Component.empty());
        Vec3 from = minecraft.player != null ? minecraft.player.position() : Vec3.ZERO;
        if (answer.latest == null) {
            searched(lines, search, from);
            return;
        }
        lines.add(TooltipLines.line("stored", TooltipLines.stored(answer.total, answer.containers)));
        if (answer.containers == 1) {
            lines.add(TooltipLines.line("in", TooltipLines.joined(TooltipLines.place(answer.latest), distance(answer.latest, from), TooltipLines.age(answer.latest))));
            return;
        }
        ContainerRecord nearest = answer.hits.stream().map(SearchHit::record)
                .min(Comparator.comparingDouble(record -> Vec3.atCenterOf(record.pos()).distanceToSqr(from))).orElse(answer.latest);
        lines.add(TooltipLines.line("nearest", TooltipLines.joined(FindMessages.name(nearest), distance(nearest, from))));
        lines.add(TooltipLines.line("last_seen", TooltipLines.joined(TooltipLines.place(answer.latest), TooltipLines.age(answer.latest))));
    }

    // An item never seen in a remembered container, but found by a search (in containers the server looked in): what it
    // found, and when.
    private static void searched(List<Component> lines, SearchHistory.Search search, Vec3 from) {
        Component age = ItemMarker.age(Util.getMillis() - search.at());
        lines.add(TooltipLines.line("found", TooltipLines.joined(TooltipLines.stored(search.total(), search.places().size()),
                age != null ? age : Component.translatable("gui.wheredidiputit.browser.just_now"))));
        SearchHistory.Place nearest = search.places().stream()
                .min(Comparator.comparingDouble(place -> Vec3.atCenterOf(place.pos()).distanceToSqr(from))).orElseThrow();
        lines.add(TooltipLines.line("nearest", TooltipLines.joined(nearest.name(), distance(nearest.pos(), from))));
    }

    private static Component distance(ContainerRecord record, Vec3 from) {
        return distance(record.pos(), from);
    }

    private static Component distance(BlockPos pos, Vec3 from) {
        return Component.translatable("tooltip.wheredidiputit.distance", TooltipLines.number(Math.round(Vec3.atCenterOf(pos).distanceTo(from))));
    }

    private static Answer answer(DimensionMemory memory, ItemStack stack) {
        MatchMode mode = WDIPIClientConfig.MATCH_MODE.get();
        boolean nested = WDIPIClientConfig.INDEX_NESTED_CONTENTS.get();
        Answer cached = last;
        if (cached != null && cached.memory == memory && cached.version == memory.version() && cached.mode == mode && cached.nested == nested
                && ItemStack.isSameItemSameTags(cached.stack, stack)) {
            return cached;
        }
        // Every container in the dimension, however far: the tooltip says where, the find key goes there.
        List<SearchHit> hits = ContainerFinder.search(memory, ItemMatcher.of(stack, mode, nested), Vec3.ZERO, Double.MAX_VALUE);
        ContainerRecord latest = hits.stream().map(SearchHit::record).max(Comparator.comparingLong(ContainerRecord::seenAt)).orElse(null);
        long total = hits.stream().mapToLong(SearchHit::count).sum();
        Answer answer = new Answer(stack.copyWithCount(1), mode, nested, memory, memory.version(), List.copyOf(hits), latest, hits.size(), total);
        last = answer;
        return answer;
    }

    // The containers holding it are kept too: the nearest one changes as the player moves, without searching again.
    private record Answer(ItemStack stack, MatchMode mode, boolean nested, DimensionMemory memory, int version, List<SearchHit> hits,
                          @Nullable ContainerRecord latest, int containers, long total) {
    }
}
