package com.levi.aeroshipsaver.net;

import com.enxv.aeronauticsstructuretool.SubLevelFileStore;
import com.levi.aeroshipsaver.AeroShipSaver;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
            .playToClient(BlueprintSyncPayload.TYPE, BlueprintSyncPayload.STREAM_CODEC, AeroNetwork::receiveOnClient)
            .playToServer(LoadBlueprintPayload.TYPE, LoadBlueprintPayload.STREAM_CODEC, AeroNetwork::receiveLoadOnServer);
    }

    /**
     * Runs on the server: load a ship straight from the bytes the client sent, so it works
     * even when the server has never seen the file. Also drops a copy on the server so a
     * plain {@code /aeroship load <name>} works there afterwards.
     */
    private static void receiveLoadOnServer(LoadBlueprintPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            String name = SubLevelFileStore.sanitize(payload.fileName());
            try {
                SubLevelFileStore.writeClientBlueprint(name, payload.contents());
                SubLevelFileStore.load(player.serverLevel(), player.blockPosition(), Direction.UP, name, payload.contents());
                player.sendSystemMessage(Component.literal("Loaded ship '" + name + "' at your position.")
                    .withStyle(ChatFormatting.GREEN));
            } catch (Exception e) {
                player.sendSystemMessage(Component.literal("Couldn't load '" + name + "': " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            }
        });
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
    /** Client-side: ask the server to load this blueprint, sending the bytes with it. */
    public static void requestLoad(String fileName, byte[] contents) {
        PacketDistributor.sendToServer(new LoadBlueprintPayload(fileName, contents));
    }

    public static void sendBlueprint(ServerPlayer player, String fileName, byte[] contents) {
        try {
            PacketDistributor.sendToPlayer(player, new BlueprintSyncPayload(fileName, contents));
        } catch (Throwable t) {
            AeroShipSaver.LOGGER.debug("Blueprint sync skipped for {}: {}", player.getGameProfile().getName(), t.toString());
        }
    }
}
