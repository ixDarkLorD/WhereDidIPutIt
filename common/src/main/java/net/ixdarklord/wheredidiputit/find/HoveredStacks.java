package net.ixdarklord.wheredidiputit.find;

import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Items under the mouse that aren't in a slot: entries of recipe viewers (JEI, REI) in their overlays and recipe
 * screens. Each installed viewer registers a provider from {@code compat/}; the find key asks them when no slot is
 * hovered.
 */
public final class HoveredStacks {
    private static final List<Provider> PROVIDERS = new ArrayList<>();

    private HoveredStacks() {
    }

    public static void register(Provider provider) {
        PROVIDERS.add(provider);
    }

    /** The item a recipe viewer has under the mouse over {@code screen}, or an empty stack. */
    public static ItemStack fromViewers(Screen screen) {
        for (Provider provider : PROVIDERS) {
            try {
                ItemStack stack = provider.hovered(screen);
                if (!stack.isEmpty()) return stack;
            } catch (RuntimeException e) {
                // A viewer's internals changed or it isn't ready yet: the key still works on slots.
                WhereDidIPutIt.LOGGER.debug("A recipe viewer couldn't say what is under the mouse", e);
            }
        }
        return ItemStack.EMPTY;
    }

    @FunctionalInterface
    public interface Provider {
        /** The item under the mouse, or an empty stack. */
        ItemStack hovered(Screen screen);
    }
}
