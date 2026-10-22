package net.ixdarklord.wheredidiputit.forge;

import net.ixdarklord.coolcatcore.api.core.forge.ForgeModEntrypoint;
import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.ixdarklord.wheredidiputit.WhereDidIPutItClient;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Forge entrypoint: hands the common and client mod constructors to CoolCatLib, then Forge's own common and client ones. */
@Mod(WhereDidIPutIt.MOD_ID)
public final class WhereDidIPutItForge extends ForgeModEntrypoint {
    public WhereDidIPutItForge(FMLJavaModLoadingContext context) {
        super(context);
        this.common(WhereDidIPutIt::new);
        this.common(WhereDidIPutItForgeCommon::new);
        this.client(() -> WhereDidIPutItClient::new);
        this.client(() -> WhereDidIPutItForgeClient::new);
    }
}
