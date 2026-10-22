package net.ixdarklord.wheredidiputit.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.ixdarklord.coolcatcore.api.client.registry.KeyMappingRegistry;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.ixdarklord.wheredidiputit.find.FindKey;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.Supplier;

/** The keys that open the mod's screens while playing: the item search (Y) and the container browser (K). */
public final class ScreenKeys {
    public static final KeyMapping SEARCH = new KeyMapping("key.wheredidiputit.search", InputConstants.Type.KEYBOARD, InputConstants.KEY_Y, FindKey.CATEGORY);
    public static final KeyMapping BROWSER = new KeyMapping("key.wheredidiputit.browser", InputConstants.Type.KEYBOARD, InputConstants.KEY_K, FindKey.CATEGORY);

    private ScreenKeys() {
    }

    public static void init() {
        KeyMappingRegistry.register(SEARCH);
        KeyMappingRegistry.register(BROWSER);
        ClientTickEvents.END.register(minecraft -> {
            open(minecraft, SEARCH, SearchScreen::new);
            open(minecraft, BROWSER, ContainerBrowserScreen::new);
        });
    }

    private static void open(Minecraft minecraft, KeyMapping key, Supplier<Screen> screen) {
        while (key.consumeClick()) {
            if (minecraft.gui.screen() == null && minecraft.level != null && WDIPIClientConfig.ENABLED.get()) minecraft.gui.setScreen(screen.get());
        }
    }
}
