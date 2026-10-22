package net.ixdarklord.wheredidiputit.live;

import net.ixdarklord.coolcatcore.api.event.v2.common.PlayerEvents;
import net.ixdarklord.coolcatcore.api.network.Network;
import net.ixdarklord.wheredidiputit.config.WDIPIServerConfig;
import net.ixdarklord.wheredidiputit.network.SharedContainer;
import net.ixdarklord.wheredidiputit.network.ShareContainerPayload;
import net.ixdarklord.wheredidiputit.network.SharedContainerPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.Util;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The server's half of team memory ({@code sharing.shareTeamMemory}, off by default): a player's client tells it of a
 * container they just closed, and it passes that on to their teammates who are online. It holds nothing and reads no
 * container itself; it only checks the player could have seen it (there, close by, allowed to interact) and keeps
 * shares from coming too fast.
 */
public final class TeamSharingServer {
    /** How near the player a shared container must be: they just had it open. */
    private static final double REACH = 8.0;
    /** The least time between one player's shares, in milliseconds. */
    private static final long MIN_INTERVAL_MILLIS = 150L;

    private static final Map<UUID, Long> LAST_SHARE = new HashMap<>();

    private TeamSharingServer() {
    }

    public static void init() {
        PlayerEvents.LEAVE.register(player -> LAST_SHARE.remove(player.getUUID()));
    }

    public static void onShare(ServerPlayer player, ShareContainerPayload payload) {
        SharedContainer container = payload.container();
        if (!WDIPIServerConfig.SHARE_TEAM_MEMORY.get() || !container.withinLimits()) return;
        if (!(player.getTeam() instanceof PlayerTeam team) || container.dimension() != player.level().dimension()) return;
        if (Vec3.atCenterOf(container.pos()).distanceTo(player.getEyePosition()) > REACH || !player.mayInteract(player.level(), container.pos())) return;
        long now = Util.getMillis();
        Long last = LAST_SHARE.get(player.getUUID());
        if (last != null && now - last < MIN_INTERVAL_MILLIS) return;
        LAST_SHARE.put(player.getUUID(), now);
        // The server's clock, not the client's: a share can't claim to be newer than it is.
        SharedContainer stamped = new SharedContainer(container.dimension(), container.pos(), container.block(), container.label(), System.currentTimeMillis(),
                container.items(), container.slots(), container.entity());
        SharedContainerPayload shared = new SharedContainerPayload(player.getGameProfile().getName(), stamped);
        for (ServerPlayer other : player.level().getServer().getPlayerList().getPlayers()) {
            if (other != player && other.getTeam() == team && Network.canPlayerReceive(other, SharedContainerPayload.TYPE)) {
                Network.sendToPlayer(other, shared);
            }
        }
    }
}
