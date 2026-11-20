package com.btmi.network;

import com.btmi.BTMI;
import com.btmi.client.StateAffairsChronicleClient;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ChronicleOpenPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ChronicleOpenPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "chronicle_open"));

    public static final StreamCodec<FriendlyByteBuf, ChronicleOpenPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
            },
            buffer -> new ChronicleOpenPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChronicleOpenPayload payload, IPayloadContext context) {
        context.enqueueWork(StateAffairsChronicleClient::open);
    }
}
