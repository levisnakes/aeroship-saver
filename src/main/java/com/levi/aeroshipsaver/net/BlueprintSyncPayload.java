package com.levi.aeroshipsaver.net;

import com.levi.aeroshipsaver.AeroShipSaver;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server -> client: hands a freshly saved blueprint back to the player who saved it, so the
 * file lands on <em>their</em> machine and not just the host's.
 *
 * <p>Needed because {@code /aeroship save} executes on the server: under Essential/LAN the
 * host's game directory is where the file would otherwise be written. Registered as an
 * <em>optional</em> payload so servers without this mod still accept the connection.
 */
public record BlueprintSyncPayload(String fileName, byte[] contents) implements CustomPacketPayload {
    /** Generous cap; a single blueprint is normally a few KB to a few hundred KB. */
    private static final int MAX_BYTES = 8 * 1024 * 1024;

    public static final CustomPacketPayload.Type<BlueprintSyncPayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AeroShipSaver.MODID, "blueprint_sync"));

    public static final StreamCodec<FriendlyByteBuf, BlueprintSyncPayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeUtf(payload.fileName());
            buf.writeByteArray(payload.contents());
        },
        buf -> new BlueprintSyncPayload(buf.readUtf(), buf.readByteArray(MAX_BYTES))
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
