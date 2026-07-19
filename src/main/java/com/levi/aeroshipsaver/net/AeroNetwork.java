package com.levi.aeroshipsaver.net;

import com.enxv.aeronauticsstructuretool.SubLevelFileStore;
import com.levi.aeroshipsaver.AeroShipSaver;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The mod's single network channel, registered as <b>optional</b>.
 *
 * <p>Optional payloads are ignored during the connection handshake by parties that don't
 * have them, so this does not stop you joining a server that lacks AeroShip Saver - which
 * is the whole point of the mod being content-free.
 */
public final class AeroNetwork {
    private AeroNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(AeroShipSaver.MODID)
            .optional()
            .playToClient(BlueprintSyncPayload.TYPE, BlueprintSyncPayload.STREAM_CODEC, AeroNetwork::receiveOnClient);
    }

    /** Runs on the client: write the synced blueprint into this player's own local folder. */
    private static void receiveOnClient(BlueprintSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                SubLevelFileStore.writeClientBlueprint(payload.fileName(), payload.contents());
            } catch (Exception e) {
                AeroShipSaver.LOGGER.warn("Could not write synced blueprint '{}'", payload.fileName(), e);
            }
        });
    }

    /**
     * Send a saved blueprint to the player who saved it. No-op if the client can't accept it
     * (e.g. they somehow lack the optional channel) - the server-side copy still exists.
     */
    public static void sendBlueprint(ServerPlayer player, String fileName, byte[] contents) {
        try {
            PacketDistributor.sendToPlayer(player, new BlueprintSyncPayload(fileName, contents));
        } catch (Throwable t) {
            AeroShipSaver.LOGGER.debug("Blueprint sync skipped for {}: {}", player.getGameProfile().getName(), t.toString());
        }
    }
}
