package net.ixdarklord.wheredidiputit.neoforge;

import net.ixdarklord.coolcatcore.api.core.ModConstructor;
import net.ixdarklord.coolcatcore.api.platform.Platform;
import net.ixdarklord.wheredidiputit.live.NetworkReaders;
import net.ixdarklord.wheredidiputit.neoforge.compat.Ae2NetworkReader;
import net.ixdarklord.wheredidiputit.neoforge.compat.TomsStorageNetworkReader;

/** NeoForge's own setup on both sides: live search's readers for the storage mods whose jars are NeoForge's, each only when installed. */
public final class WhereDidIPutItNeoForgeCommon implements ModConstructor {
    @Override
    public void onConstructMod() {
        if (Platform.isModLoaded(Ae2NetworkReader.MOD_ID)) NetworkReaders.register(new Ae2NetworkReader());
        // Simple Storage Network's reader is left out until it has a 26.2 build.
        if (Platform.isModLoaded(TomsStorageNetworkReader.MOD_ID)) NetworkReaders.register(new TomsStorageNetworkReader());
    }
}
