package net.ixdarklord.wheredidiputit.fabric;

import net.ixdarklord.coolcatcore.api.core.ModConstructor;
import net.ixdarklord.coolcatcore.api.platform.Platform;
import net.ixdarklord.wheredidiputit.fabric.compat.Ae2NetworkReader;
import net.ixdarklord.wheredidiputit.fabric.compat.TomsStorageNetworkReader;
import net.ixdarklord.wheredidiputit.live.NetworkReaders;

/** Fabric's own setup on both sides, a second {@code coolcatlib:common} entrypoint: live search's readers for the storage mods whose jars are Fabric's (AE2, Tom's Simple Storage), each only when installed. */
public final class WhereDidIPutItFabricCommon implements ModConstructor {
    @Override
    public void onConstructMod() {
        if (Platform.isModLoaded(Ae2NetworkReader.MOD_ID)) NetworkReaders.register(new Ae2NetworkReader());
        if (Platform.isModLoaded(TomsStorageNetworkReader.MOD_ID)) NetworkReaders.register(new TomsStorageNetworkReader());
    }
}
