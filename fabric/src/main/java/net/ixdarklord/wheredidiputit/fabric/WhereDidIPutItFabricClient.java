package net.ixdarklord.wheredidiputit.fabric;

import net.ixdarklord.coolcatcore.api.core.ClientModConstructor;
import net.ixdarklord.coolcatcore.api.platform.Platform;
import net.ixdarklord.wheredidiputit.fabric.compat.Ae2Terminal;
import net.ixdarklord.wheredidiputit.fabric.compat.TomsStorageTerminal;
import net.ixdarklord.wheredidiputit.memory.StorageTerminals;

/**
 * Fabric's own client setup, a second {@code coolcatlib:client} entrypoint: the storage mods whose terminal readers
 * need Fabric jars, each only when installed.
 */
public final class WhereDidIPutItFabricClient implements ClientModConstructor {
    @Override
    public void onConstructMod() {
        if (Platform.isModLoaded(Ae2Terminal.MOD_ID)) StorageTerminals.register(new Ae2Terminal());
        if (Platform.isModLoaded(TomsStorageTerminal.MOD_ID)) StorageTerminals.register(new TomsStorageTerminal());
    }
}
