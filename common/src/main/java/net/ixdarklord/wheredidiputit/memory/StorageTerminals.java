package net.ixdarklord.wheredidiputit.memory;

import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Terminals of storage mods (AE2, Refined Storage, Simple Storage Network, Tom's Simple Storage): screens that show
 * a whole network's items instead of slots. A terminal opened from a block is remembered with everything its network
 * showed, so a search points to the terminal. Each mod's {@link Terminal} lives in the loader module(s) that mod
 * exists for, and is registered there only when the mod is installed.
 */
public final class StorageTerminals {
    private static final List<Terminal> TERMINALS = new ArrayList<>();

    private StorageTerminals() {
    }

    public static void register(Terminal terminal) {
        TERMINALS.add(terminal);
    }

    /** The terminal that knows this screen, or null for an ordinary container screen. */
    public static @Nullable Terminal of(AbstractContainerScreen<?> screen) {
        for (Terminal terminal : TERMINALS) {
            try {
                if (terminal.handles(screen)) return terminal;
            } catch (RuntimeException | LinkageError e) {
                // Another version of that mod: its terminal is treated as an ordinary screen.
                WhereDidIPutIt.LOGGER.debug("A storage terminal reader failed", e);
            }
        }
        return null;
    }

    /** The network item under the mouse in a terminal screen, or an empty stack. */
    public static ItemStack hovered(AbstractContainerScreen<?> screen) {
        Terminal terminal = of(screen);
        if (terminal == null) return ItemStack.EMPTY;
        try {
            return terminal.hovered(screen);
        } catch (RuntimeException | LinkageError e) {
            WhereDidIPutIt.LOGGER.debug("A storage terminal reader failed", e);
            return ItemStack.EMPTY;
        }
    }

    /** One storage mod's terminal screens. */
    public interface Terminal {
        boolean handles(AbstractContainerScreen<?> screen);

        /** Everything the network shows in the open terminal, with its amounts. */
        List<StoredItem> contents(AbstractContainerScreen<?> screen);

        /** The network item under the mouse, or an empty stack; the default is for terminals whose entries are slots. */
        default ItemStack hovered(AbstractContainerScreen<?> screen) {
            return ItemStack.EMPTY;
        }
    }
}
