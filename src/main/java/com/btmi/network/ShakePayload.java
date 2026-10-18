package com.btmi.network;

import com.btmi.BTMI;
import com.btmi.client.ClientShakeHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ShakePayload(double x, double y, double z, float intensity, int duration) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ShakePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "screen_shake"));

    public static final StreamCodec<FriendlyByteBuf, ShakePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeDouble(payload.x);
                buffer.writeDouble(payload.y);
                buffer.writeDouble(payload.z);
                buffer.writeFloat(payload.intensity);
                buffer.writeVarInt(payload.duration);
            },
            buffer -> new ShakePayload(
                    buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                    buffer.readFloat(), buffer.readVarInt()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ShakePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientShakeHandler.handle(payload));
    }
}
