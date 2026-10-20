package net.ixdarklord.wheredidiputit.config;

/** Loads the configs. */
public final class WDIPIConfigs {
    private WDIPIConfigs() {
    }

    /** Builds both configs, which registers them and loads their files. */
    public static void register() {
        WDIPIServerConfig.CONFIG.id();
        WDIPIClientConfig.CONFIG.id();
    }
}
