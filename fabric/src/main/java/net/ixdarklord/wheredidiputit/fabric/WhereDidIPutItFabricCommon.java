package net.ixdarklord.wheredidiputit.fabric;

import net.ixdarklord.coolcatcore.api.core.ModConstructor;
import net.ixdarklord.coolcatcore.api.platform.Platform;
import net.ixdarklord.wheredidiputit.fabric.compat.TomsStorageNetworkReader;
import net.ixdarklord.wheredidiputit.live.NetworkReaders;

/** Fabric's own setup on both sides: live search's reader for Tom's Simple Storage, whose Fabric jar is its own, when installed. */
public final class WhereDidIPutItFabricCommon implements ModConstructor {
    @Override
    public void onConstructMod() {
        if (Platform.isModLoaded(TomsStorageNetworkReader.MOD_ID)) NetworkReaders.register(new TomsStorageNetworkReader());
    }
}
