package com.btmi.network;

import com.btmi.BTMI;
import com.btmi.client.DashChargeHud;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// 服务端 -> 客户端：同步破折号万象投影的层数
public record DashChargePayload(int charge) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DashChargePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "dash_charge"));

    public static final StreamCodec<FriendlyByteBuf, DashChargePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeVarInt(payload.charge()),
            buffer -> new DashChargePayload(buffer.readVarInt()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DashChargePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> DashChargeHud.setCharge(payload.charge()));
    }
}
