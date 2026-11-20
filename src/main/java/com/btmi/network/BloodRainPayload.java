package com.btmi.network;

import com.btmi.BTMI;
import com.btmi.client.BloodRainClient;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BloodRainPayload(boolean active, int durationTicks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BloodRainPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "blood_rain"));

    public static final StreamCodec<FriendlyByteBuf, BloodRainPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeBoolean(payload.active());
                buffer.writeVarInt(payload.durationTicks());
            },
            buffer -> new BloodRainPayload(buffer.readBoolean(), buffer.readVarInt()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BloodRainPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> BloodRainClient.apply(payload.active(), payload.durationTicks()));
    }
}
