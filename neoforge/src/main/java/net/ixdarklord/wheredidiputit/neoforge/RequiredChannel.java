package net.ixdarklord.wheredidiputit.neoforge;

import io.netty.buffer.ByteBuf;
import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Makes NeoForge require the mod on both sides (author, 2026-10-22). CoolCatLib registers every payload optional, so a
 * side without the mod would be let in; this one channel isn't, so NeoForge's own network check turns away a client
 * without the mod from a server with it, and the other way round, with its own message. Nothing is ever sent on it.
 */
public final class RequiredChannel {
    private static final String VERSION = "1";

    private RequiredChannel() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterPayloadHandlersEvent event) ->
                event.registrar(VERSION).playToClient(Marker.TYPE, Marker.CODEC, (payload, context) -> {}));
    }

    private record Marker() implements CustomPacketPayload {
        static final Type<Marker> TYPE = new Type<>(WhereDidIPutIt.id("required"));
        static final StreamCodec<ByteBuf, Marker> CODEC = StreamCodec.unit(new Marker());

        @Override
        public Type<Marker> type() {
            return TYPE;
        }
    }
}
