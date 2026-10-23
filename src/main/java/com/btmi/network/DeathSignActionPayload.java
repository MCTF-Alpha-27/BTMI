package com.btmi.network;

import com.btmi.BTMI;
import com.btmi.item.DeathSignHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DeathSignActionPayload(int hand, int action) implements CustomPacketPayload {
    public static final int ACTION_ATTACK = 0;
    public static final int ACTION_DETONATE = 1;
    public static final int ACTION_KNELL = 2;

    public static final CustomPacketPayload.Type<DeathSignActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "death_sign_action"));

    public static final StreamCodec<FriendlyByteBuf, DeathSignActionPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.hand());
                buffer.writeVarInt(payload.action());
            },
            buffer -> new DeathSignActionPayload(buffer.readVarInt(), buffer.readVarInt()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeathSignActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                DeathSignHandler.handleAction(player, payload.hand(), payload.action());
            }
        });
    }
}
