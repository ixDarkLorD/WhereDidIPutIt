package net.ixdarklord.wheredidiputit.forge;

import net.ixdarklord.coolcatcore.api.core.ClientModConstructor;
import net.ixdarklord.coolcatcore.api.platform.Platform;
import net.ixdarklord.wheredidiputit.memory.StorageTerminals;
import net.ixdarklord.wheredidiputit.forge.compat.Ae2Terminal;
import net.ixdarklord.wheredidiputit.forge.compat.RefinedStorageTerminal;
import net.ixdarklord.wheredidiputit.forge.compat.StorageNetworkTerminal;
import net.ixdarklord.wheredidiputit.forge.compat.TomsStorageTerminal;

/** Forge's own client setup: the storage mods whose terminal readers need Forge jars, each only when installed. */
public final class WhereDidIPutItForgeClient implements ClientModConstructor {
    @Override
    public void onConstructMod() {
        if (Platform.isModLoaded(Ae2Terminal.MOD_ID)) StorageTerminals.register(new Ae2Terminal());
        if (Platform.isModLoaded(RefinedStorageTerminal.MOD_ID)) StorageTerminals.register(new RefinedStorageTerminal());
        if (Platform.isModLoaded(StorageNetworkTerminal.MOD_ID)) StorageTerminals.register(new StorageNetworkTerminal());
        if (Platform.isModLoaded(TomsStorageTerminal.MOD_ID)) StorageTerminals.register(new TomsStorageTerminal());
    }
}
