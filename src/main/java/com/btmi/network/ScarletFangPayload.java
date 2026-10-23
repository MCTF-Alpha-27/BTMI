package com.btmi.network;

import com.btmi.BTMI;
import com.btmi.item.ScarletFangHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// 客户端 -> 服务端：猩红之牙的开火与技能
public record ScarletFangPayload(int action) implements CustomPacketPayload {
    public static final int ACTION_FIRE = 0;
    public static final int ACTION_RING = 1;

    public static final CustomPacketPayload.Type<ScarletFangPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "scarlet_fang"));

    public static final StreamCodec<FriendlyByteBuf, ScarletFangPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.action()),
            buffer -> new ScarletFangPayload(buffer.readVarInt()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ScarletFangPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (payload.action() == ACTION_FIRE) {
                    ScarletFangHandler.fire(player);
                } else if (payload.action() == ACTION_RING) {
                    ScarletFangHandler.triggerRing(player);
                }
            }
        });
    }
}
