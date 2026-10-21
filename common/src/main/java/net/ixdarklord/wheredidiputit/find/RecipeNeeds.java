package net.ixdarklord.wheredidiputit.find;

import net.ixdarklord.wheredidiputit.client.render.ContainerHighlight;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.ixdarklord.wheredidiputit.memory.ContainerMemory;
import net.ixdarklord.wheredidiputit.memory.ContainerRecord;
import net.ixdarklord.wheredidiputit.memory.DimensionMemory;
import net.ixdarklord.wheredidiputit.memory.StoredItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds what is missing to craft an item: from the recipes the player has unlocked (the client's recipe book; the
 * client knows no others), the one making the item that lacks the fewest ingredients, and for each ingredient the
 * player hasn't enough of, the nearest remembered container holding some. From memory only: live search answers one
 * item at a time.
 */
public final class RecipeNeeds {
    private RecipeNeeds() {
    }

    /** Points out where the missing ingredients for {@code target} are, or says why it can't. */
    public static void find(ItemStack target) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        DimensionMemory memory = level != null ? ContainerMemory.dimension(level.dimension()) : null;
        if (player == null || level == null || memory == null) return;

        Map<Ingredient, Integer> missing = null;
        ContextMap context = SlotDisplayContext.fromLevel(level);
        for (RecipeCollection collection : player.getRecipeBook().getCollections()) {
            for (RecipeDisplayEntry recipe : collection.getRecipes()) {
                if (recipe.craftingRequirements().isEmpty() || recipe.resultItems(context).stream().noneMatch(result -> result.is(target.getItem()))) continue;
                Map<Ingredient, Integer> lacking = missing(recipe.craftingRequirements().get(), player.getInventory());
                if (missing == null || total(lacking) < total(missing)) missing = lacking;
            }
        }
        if (missing == null) {
            FindKey.tell(minecraft, Component.translatable("gui.wheredidiputit.recipe.unknown", target.getHoverName()), false);
            return;
        }
        if (missing.isEmpty()) {
            FindKey.tell(minecraft, Component.translatable("gui.wheredidiputit.recipe.have_all", target.getHoverName()), true);
            return;
        }

        Vec3 eyes = player.getEyePosition();
        int maxDistance = WDIPIClientConfig.MAX_SEARCH_DISTANCE.get();
        List<ContainerHighlight.Found> found = new ArrayList<>();
        Set<BlockPos> used = new HashSet<>();
        for (Ingredient ingredient : missing.keySet()) {
            ContainerHighlight.Found nearest = nearest(memory, ingredient, eyes, maxDistance);
            // One container holding several missing ingredients is pointed out once, with the first of them.
            if (nearest != null && used.add(nearest.pos())) found.add(nearest);
        }
        if (found.isEmpty()) {
            FindKey.tell(minecraft, Component.translatable("gui.wheredidiputit.recipe.not_found", target.getHoverName()), false);
            return;
        }
        FindKey.highlight(minecraft, level, found);
    }

    // How many of each ingredient the recipe needs that the inventory hasn't got; empty when it has everything.
    private static Map<Ingredient, Integer> missing(List<Ingredient> requirements, Inventory inventory) {
        Map<Ingredient, Integer> needed = new LinkedHashMap<>();
        for (Ingredient ingredient : requirements) {
            if (!ingredient.isEmpty()) needed.merge(ingredient, 1, Integer::sum);
        }
        Map<Ingredient, Integer> missing = new LinkedHashMap<>();
        needed.forEach((ingredient, count) -> {
            int have = 0;
            for (ItemStack stack : inventory.getNonEquipmentItems()) {
                if (ingredient.test(stack)) have += stack.getCount();
            }
            if (have < count) missing.put(ingredient, count - have);
        });
        return missing;
    }

    private static int total(Map<Ingredient, Integer> missing) {
        return missing.values().stream().mapToInt(Integer::intValue).sum();
    }

    // The nearest remembered container holding some of the ingredient, with the matching item it holds most of.
    private static @Nullable ContainerHighlight.Found nearest(DimensionMemory memory, Ingredient ingredient, Vec3 from, int maxDistance) {
        ContainerRecord best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ContainerRecord record : memory.records()) {
            if (record.ignored()) continue;
            double distance = Vec3.atCenterOf(record.pos()).distanceTo(from);
            if (distance > maxDistance || distance >= bestDistance) continue;
            if (record.items().stream().anyMatch(item -> ingredient.test(item.stack()))) {
                best = record;
                bestDistance = distance;
            }
        }
        if (best == null) return null;
        StoredItem most = best.items().stream().filter(item -> ingredient.test(item.stack())).max(Comparator.comparingLong(StoredItem::count)).orElseThrow();
        long count = best.items().stream().filter(item -> ingredient.test(item.stack())).mapToLong(StoredItem::count).sum();
        return new ContainerHighlight.Found(best.pos(), most.stack().copyWithCount(1), count, best.seenAt(), best.entity());
    }
}
