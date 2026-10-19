package com.btmi.network;

import com.btmi.BTMI;
import com.btmi.item.SmashHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ExclamationPayload(int action) implements CustomPacketPayload {
    public static final int ACTION_HOMERUN = 0;

    public static final CustomPacketPayload.Type<ExclamationPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "exclamation_action"));

    public static final StreamCodec<FriendlyByteBuf, ExclamationPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.action()),
            buffer -> new ExclamationPayload(buffer.readVarInt()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ExclamationPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && payload.action() == ACTION_HOMERUN) {
                SmashHandler.homeRun((ServerLevel) player.level(), player);
            }
        });
    }
}
