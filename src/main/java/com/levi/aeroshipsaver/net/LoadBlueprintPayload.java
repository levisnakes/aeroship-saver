package com.levi.aeroshipsaver.net;

import com.levi.aeroshipsaver.AeroShipSaver;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> server: "load this blueprint, and here are its bytes".
 *
 * <p>Sending the contents rather than just a name is what makes loading work when the file
 * only exists on the player's machine - a ship saved in another world, or saved by a guest
 * on an Essential/LAN host. The server would otherwise look on its own disk and find nothing.
 */
public record LoadBlueprintPayload(String fileName, byte[] contents) implements CustomPacketPayload {
    private static final int MAX_BYTES = 8 * 1024 * 1024;

    public static final CustomPacketPayload.Type<LoadBlueprintPayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(AeroShipSaver.MODID, "blueprint_load"));

    public static final StreamCodec<FriendlyByteBuf, LoadBlueprintPayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeUtf(payload.fileName());
            buf.writeByteArray(payload.contents());
        },
        buf -> new LoadBlueprintPayload(buf.readUtf(), buf.readByteArray(MAX_BYTES))
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
