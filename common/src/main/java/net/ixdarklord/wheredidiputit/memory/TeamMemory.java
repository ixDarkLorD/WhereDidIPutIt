package net.ixdarklord.wheredidiputit.memory;

import net.ixdarklord.coolcatcore.api.network.Network;
import net.ixdarklord.wheredidiputit.config.WDIPIServerConfig;
import net.ixdarklord.wheredidiputit.find.LiveSearch;
import net.ixdarklord.wheredidiputit.network.ShareContainerPayload;
import net.ixdarklord.wheredidiputit.network.SharedContainer;
import net.ixdarklord.wheredidiputit.network.SharedContainerPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * The client's half of team memory: on a server with the mod that allows it ({@code sharing.shareTeamMemory}), each
 * container the player closes is told to the server for their team, and what teammates saw is remembered as if seen
 * here, when it is newer than what is known. Storage networks aren't shared: they are far too big.
 */
public final class TeamMemory {
    private TeamMemory() {
    }

    /** Whether shares go out: the server has the mod and allows it, and the player is on a team. */
    public static boolean sharing() {
        Minecraft minecraft = Minecraft.getInstance();
        return LiveSearch.serverHasMod() && WDIPIServerConfig.SHARE_TEAM_MEMORY.get() && minecraft.player != null && minecraft.player.getTeam() != null;
    }

    /** Tells the team of a container just recorded. */
    static void share(ResourceKey<Level> dimension, ContainerRecord record) {
        if (record.network() || !sharing()) return;
        SharedContainer container = SharedContainer.of(dimension, record);
        if (container.withinLimits()) Network.sendToServer(new ShareContainerPayload(container));
    }

    /** A teammate saw a container: remembered, unless what is known of it here is newer. */
    public static void onShared(SharedContainerPayload payload) {
        SharedContainer container = payload.container();
        if (!WDIPIServerConfig.SHARE_TEAM_MEMORY.get()) return;
        DimensionMemory memory = ContainerMemory.dimension(container.dimension());
        if (memory == null) return;
        ContainerRecord known = container.entity().isPresent() ? memory.getEntity(container.entity().get()) : memory.get(container.pos());
        if (known != null && known.seenAt() >= container.seenAt()) return;
        memory.put(container.toRecord(known != null && known.ignored()));
    }
}
