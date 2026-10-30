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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
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
        for (RecipeCollection collection : player.getRecipeBook().getCollections()) {
            for (Recipe<?> recipe : collection.getRecipes()) {
                // 1.20.1's recipe book lists every recipe; only the unlocked ones count, as on 26.1.
                if (!player.getRecipeBook().contains(recipe) || recipe.getIngredients().isEmpty()
                        || !recipe.getResultItem(level.registryAccess()).is(target.getItem())) continue;
                Map<Ingredient, Integer> lacking = missing(recipe.getIngredients(), player.getInventory());
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

    /**
     * What crafting {@code target} takes, ingredient by ingredient, from the unlocked recipe making it that lacks the
     * fewest ingredients against the inventory; null when no unlocked recipe makes it (the gathering list's plans).
     */
    public static @Nullable Map<Ingredient, Integer> requirements(ItemStack target) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) return null;
        Map<Ingredient, Integer> best = null;
        int bestMissing = Integer.MAX_VALUE;
        for (RecipeCollection collection : player.getRecipeBook().getCollections()) {
            for (Recipe<?> recipe : collection.getRecipes()) {
                // 1.20.1's recipe book lists every recipe; only the unlocked ones count, as on 26.1.
                if (!player.getRecipeBook().contains(recipe) || recipe.getIngredients().isEmpty()
                        || !recipe.getResultItem(level.registryAccess()).is(target.getItem())) continue;
                int lacking = total(missing(recipe.getIngredients(), player.getInventory()));
                if (lacking < bestMissing) {
                    best = counted(recipe.getIngredients());
                    bestMissing = lacking;
                }
            }
        }
        return best;
    }

    // How many of each ingredient a recipe takes.
    private static Map<Ingredient, Integer> counted(List<Ingredient> requirements) {
        // By what they accept: 1.20.1's ingredients aren't equal to one another, so a shapeless recipe's three planks
        // are three of them.
        Map<String, Ingredient> byJson = new LinkedHashMap<>();
        Map<Ingredient, Integer> needed = new LinkedHashMap<>();
        for (Ingredient ingredient : requirements) {
            if (ingredient.isEmpty()) continue;
            Ingredient same = byJson.computeIfAbsent(ingredient.toJson().toString(), json -> ingredient);
            needed.merge(same, 1, Integer::sum);
        }
        return needed;
    }

    // How many of each ingredient the recipe needs that the inventory hasn't got; empty when it has everything.
    private static Map<Ingredient, Integer> missing(List<Ingredient> requirements, Inventory inventory) {
        Map<Ingredient, Integer> needed = counted(requirements);
        Map<Ingredient, Integer> missing = new LinkedHashMap<>();
        needed.forEach((ingredient, count) -> {
            int have = 0;
            for (ItemStack stack : inventory.items) {
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
