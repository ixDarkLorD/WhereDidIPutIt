package net.ixdarklord.wheredidiputit.neoforge;

import net.ixdarklord.coolcatcore.api.core.neoforge.NeoForgeModEntrypoint;
import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.ixdarklord.wheredidiputit.WhereDidIPutItClient;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/** NeoForge entrypoint: hands the common and client mod constructors to CoolCatLib. */
@Mod(WhereDidIPutIt.MOD_ID)
public final class WhereDidIPutItNeoForge extends NeoForgeModEntrypoint {
    public WhereDidIPutItNeoForge(ModContainer container) {
        super(container);
        this.common(WhereDidIPutIt::new);
        this.client(() -> WhereDidIPutItClient::new);
    }
}
