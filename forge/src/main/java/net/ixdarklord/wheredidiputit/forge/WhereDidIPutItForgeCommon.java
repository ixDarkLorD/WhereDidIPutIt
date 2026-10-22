package net.ixdarklord.wheredidiputit.forge;

import net.ixdarklord.coolcatcore.api.core.ModConstructor;
import net.ixdarklord.coolcatcore.api.platform.Platform;
import net.ixdarklord.wheredidiputit.live.NetworkReaders;
import net.ixdarklord.wheredidiputit.forge.compat.Ae2NetworkReader;
import net.ixdarklord.wheredidiputit.forge.compat.RefinedStorageNetworkReader;
import net.ixdarklord.wheredidiputit.forge.compat.StorageNetworkNetworkReader;
import net.ixdarklord.wheredidiputit.forge.compat.TomsStorageNetworkReader;

/** Forge's own setup on both sides: live search's readers for the storage mods whose jars are Forge's, each only when installed. */
public final class WhereDidIPutItForgeCommon implements ModConstructor {
    @Override
    public void onConstructMod() {
        if (Platform.isModLoaded(Ae2NetworkReader.MOD_ID)) NetworkReaders.register(new Ae2NetworkReader());
        if (Platform.isModLoaded(RefinedStorageNetworkReader.MOD_ID)) NetworkReaders.register(new RefinedStorageNetworkReader());
        if (Platform.isModLoaded(StorageNetworkNetworkReader.MOD_ID)) NetworkReaders.register(new StorageNetworkNetworkReader());
        if (Platform.isModLoaded(TomsStorageNetworkReader.MOD_ID)) NetworkReaders.register(new TomsStorageNetworkReader());
    }
}
