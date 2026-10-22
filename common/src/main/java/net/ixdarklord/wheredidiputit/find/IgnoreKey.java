package net.ixdarklord.wheredidiputit.find;

import com.mojang.blaze3d.platform.InputConstants;
import net.ixdarklord.coolcatcore.api.client.registry.KeyMappingRegistry;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.ixdarklord.wheredidiputit.memory.ContainerWatcher;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * The key that leaves the container open now out of searches (a trash chest, a farm's output), or takes it back in.
 * Unbound by default, so it clashes with nothing; the container browser can do the same.
 */
public final class IgnoreKey {
    public static final KeyMapping KEY = new KeyMapping("key.wheredidiputit.toggle_ignore", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), FindKey.CATEGORY);

    private IgnoreKey() {
    }

    public static void init() {
        KeyMappingRegistry.register(KEY);
    }

    /** A key was pressed in a container screen; true when it was this key and a remembered container is open. */
    public static boolean onKeyPressed(KeyEvent event, AbstractContainerScreen<?> screen) {
        if (!WDIPIClientConfig.ENABLED.get() || !KEY.matches(event)) return false;
        Boolean ignored = ContainerWatcher.toggleIgnored(screen);
        if (ignored == null) return false;
        ScreenNotice.show(Component.translatable(ignored ? "gui.wheredidiputit.ignored" : "gui.wheredidiputit.unignored"));
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, ignored ? 0.8F : 1.2F));
        return true;
    }
}
