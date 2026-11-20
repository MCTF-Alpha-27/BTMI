package com.btmi.network;

import com.btmi.BTMI;
import com.btmi.client.BellTowerSkillClient;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BellTowerSkillStatePayload(String disabledPassives, String activeId, int cooldownTicks,
        String classId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BellTowerSkillStatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "bell_tower_skill_state"));

    public static final StreamCodec<FriendlyByteBuf, BellTowerSkillStatePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUtf(payload.disabledPassives());
                buffer.writeUtf(payload.activeId());
                buffer.writeVarInt(payload.cooldownTicks());
                buffer.writeUtf(payload.classId());
            },
            buffer -> new BellTowerSkillStatePayload(
                    buffer.readUtf(), buffer.readUtf(), buffer.readVarInt(), buffer.readUtf()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BellTowerSkillStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> BellTowerSkillClient.apply(
                payload.disabledPassives(), payload.activeId(), payload.cooldownTicks(), payload.classId()));
    }
}
