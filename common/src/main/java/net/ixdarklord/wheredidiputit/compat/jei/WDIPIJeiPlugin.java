package net.ixdarklord.wheredidiputit.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.ixdarklord.wheredidiputit.find.HoveredStacks;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * JEI plugin: keeps JEI's runtime so the find key works on the item under the mouse in its ingredient list, its
 * bookmarks and its recipe screen. Found by JEI only (Fabric: the {@code jei_mod_plugin} entrypoint; NeoForge: the
 * annotation), so nothing here loads without JEI.
 */
@JeiPlugin
public final class WDIPIJeiPlugin implements IModPlugin {
    private static @Nullable IJeiRuntime runtime;

    @Override
    public Identifier getPluginUid() {
        return WhereDidIPutIt.id("jei");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        if (runtime == null) HoveredStacks.register(screen -> hovered());
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    private static ItemStack hovered() {
        IJeiRuntime current = runtime;
        if (current == null) return ItemStack.EMPTY;
        ItemStack stack = current.getIngredientListOverlay().getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
        if (stack == null) stack = current.getBookmarkOverlay().getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
        if (stack == null) stack = current.getRecipesGui().getIngredientUnderMouse(VanillaTypes.ITEM_STACK).orElse(null);
        return stack != null ? stack : ItemStack.EMPTY;
    }
}
