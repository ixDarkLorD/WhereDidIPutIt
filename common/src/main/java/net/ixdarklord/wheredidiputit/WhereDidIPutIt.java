package net.ixdarklord.wheredidiputit;

import net.ixdarklord.coolcatcore.api.core.ModConstructor;
import net.ixdarklord.coolcatcore.api.platform.Platform;
import net.ixdarklord.wheredidiputit.config.WDIPIConfigs;
import net.ixdarklord.wheredidiputit.live.LiveSearchServer;
import net.ixdarklord.wheredidiputit.live.NetworkReaders;
import net.ixdarklord.wheredidiputit.live.TeamSharingServer;
import net.ixdarklord.wheredidiputit.live.WDIPICommands;
import net.ixdarklord.wheredidiputit.network.WDIPINetwork;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Common mod constructor (CoolCatLib). Fabric lists it under the {@code coolcatlib:common} entrypoint, and
 * Forge's {@code WhereDidIPutItForge} passes it to {@code common(...)}. The client half is {@link WhereDidIPutItClient}.
 */
public final class WhereDidIPutIt implements ModConstructor {
    public static final String MOD_ID = "wheredidiputit";
    public static final String MOD_NAME = "Where Did I Put It?";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    @Override
    public void onConstructMod() {
        LOGGER.info("Loading {} on {}", MOD_NAME, Platform.isFabric() ? "Fabric" : "Forge");

        WDIPIConfigs.register();
        WDIPINetwork.register();
        LiveSearchServer.init();
        WDIPICommands.init();
        TeamSharingServer.init();
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path.toLowerCase(Locale.ROOT));
    }
}
