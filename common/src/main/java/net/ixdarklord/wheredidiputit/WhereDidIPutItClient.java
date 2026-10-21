package net.ixdarklord.wheredidiputit;

import net.ixdarklord.coolcatcore.api.core.ClientModConstructor;
import net.ixdarklord.coolcatcore.api.platform.Platform;
import net.ixdarklord.wheredidiputit.client.hud.Compass;
import net.ixdarklord.wheredidiputit.client.hud.ContentsPreview;
import net.ixdarklord.wheredidiputit.client.hud.ItemMarker;
import net.ixdarklord.wheredidiputit.client.hud.TargetArrow;
import net.ixdarklord.wheredidiputit.client.render.ContainerHighlight;
import net.ixdarklord.wheredidiputit.client.render.Guidance;
import net.ixdarklord.wheredidiputit.client.screen.ScreenKeys;
import net.ixdarklord.wheredidiputit.compat.refinedstorage.RefinedStorageTerminal;
import net.ixdarklord.wheredidiputit.compat.rei.ReiCompat;
import net.ixdarklord.wheredidiputit.find.FindKey;
import net.ixdarklord.wheredidiputit.find.IgnoreKey;
import net.ixdarklord.wheredidiputit.find.LiveSearch;
import net.ixdarklord.wheredidiputit.find.LastSeenTooltip;
import net.ixdarklord.wheredidiputit.find.SortingHints;
import net.ixdarklord.wheredidiputit.memory.ContainerMemory;
import net.ixdarklord.wheredidiputit.memory.MemoryCommands;
import net.ixdarklord.wheredidiputit.memory.StorageTerminals;

/**
 * Client mod constructor (CoolCatLib): container memory, the find key, highlights and the HUD.
 * Fabric lists it under {@code coolcatlib:client}; NeoForge's {@code WhereDidIPutItNeoForge} passes it to {@code client(...)}.
 */
public final class WhereDidIPutItClient implements ClientModConstructor {
    @Override
    public void onConstructMod() {
        ContainerMemory.init();
        MemoryCommands.init();
        FindKey.init();
        IgnoreKey.init();
        ScreenKeys.init();
        LiveSearch.init();
        LastSeenTooltip.init();
        SortingHints.init();
        ContainerHighlight.init();
        Guidance.init();
        // The marker before the arrows: HUD layers draw in the order they are registered, and arrows go on top.
        ItemMarker.init();
        ContentsPreview.init();
        TargetArrow.init();
        Compass.init();
        // JEI's plugin registers itself when JEI loads it; REI is asked directly.
        if (Platform.isModLoaded(ReiCompat.MOD_ID)) ReiCompat.init();
        // Storage terminals whose mods have a loader-free jar; the others are registered by the loader modules.
        if (Platform.isModLoaded(RefinedStorageTerminal.MOD_ID)) StorageTerminals.register(new RefinedStorageTerminal());
    }
}
