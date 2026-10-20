package net.ixdarklord.wheredidiputit;

import net.ixdarklord.coolcatcore.api.core.ClientModConstructor;
import net.ixdarklord.coolcatcore.api.platform.Platform;
import net.ixdarklord.wheredidiputit.client.hud.ItemMarker;
import net.ixdarklord.wheredidiputit.client.hud.TargetArrow;
import net.ixdarklord.wheredidiputit.client.render.ContainerHighlight;
import net.ixdarklord.wheredidiputit.compat.refinedstorage.RefinedStorageTerminal;
import net.ixdarklord.wheredidiputit.compat.rei.ReiCompat;
import net.ixdarklord.wheredidiputit.find.FindKey;
import net.ixdarklord.wheredidiputit.find.LastSeenTooltip;
import net.ixdarklord.wheredidiputit.memory.ContainerMemory;
import net.ixdarklord.wheredidiputit.memory.StorageTerminals;

/**
 * Client mod constructor (CoolCatLib): container memory, the find key, highlights and the HUD.
 * Fabric lists it under {@code coolcatlib:client}; NeoForge's {@code WhereDidIPutItNeoForge} passes it to {@code client(...)}.
 */
public final class WhereDidIPutItClient implements ClientModConstructor {
    @Override
    public void onConstructMod() {
        ContainerMemory.init();
        FindKey.init();
        LastSeenTooltip.init();
        ContainerHighlight.init();
        TargetArrow.init();
        ItemMarker.init();
        // JEI's plugin registers itself when JEI loads it; REI is asked directly.
        if (Platform.isModLoaded(ReiCompat.MOD_ID)) ReiCompat.init();
        // Storage terminals whose mods have a loader-free jar; the others are registered by the loader modules.
        if (Platform.isModLoaded(RefinedStorageTerminal.MOD_ID)) StorageTerminals.register(new RefinedStorageTerminal());
    }
}
