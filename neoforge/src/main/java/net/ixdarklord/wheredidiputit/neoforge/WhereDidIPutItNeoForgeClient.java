package net.ixdarklord.wheredidiputit.neoforge;

import net.ixdarklord.coolcatcore.api.core.ClientModConstructor;
import net.ixdarklord.coolcatcore.api.platform.Platform;
import net.ixdarklord.wheredidiputit.memory.StorageTerminals;
import net.ixdarklord.wheredidiputit.neoforge.compat.Ae2Terminal;
import net.ixdarklord.wheredidiputit.neoforge.compat.StorageNetworkTerminal;
import net.ixdarklord.wheredidiputit.neoforge.compat.TomsStorageTerminal;

/** NeoForge's own client setup: the storage mods whose terminal readers need NeoForge jars, each only when installed. */
public final class WhereDidIPutItNeoForgeClient implements ClientModConstructor {
    @Override
    public void onConstructMod() {
        if (Platform.isModLoaded(Ae2Terminal.MOD_ID)) StorageTerminals.register(new Ae2Terminal());
        if (Platform.isModLoaded(StorageNetworkTerminal.MOD_ID)) StorageTerminals.register(new StorageNetworkTerminal());
        if (Platform.isModLoaded(TomsStorageTerminal.MOD_ID)) StorageTerminals.register(new TomsStorageTerminal());
    }
}
