package net.ixdarklord.wheredidiputit;

import net.ixdarklord.coolcatcore.api.core.ClientModConstructor;
import net.ixdarklord.wheredidiputit.memory.ContainerMemory;

/**
 * Client mod constructor (CoolCatLib): container memory, the find key, highlights and the HUD.
 * Fabric lists it under {@code coolcatlib:client}; NeoForge's {@code WhereDidIPutItNeoForge} passes it to {@code client(...)}.
 */
public final class WhereDidIPutItClient implements ClientModConstructor {
    @Override
    public void onConstructMod() {
        ContainerMemory.init();
    }
}
