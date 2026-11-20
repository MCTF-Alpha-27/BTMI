package com.btmi.network;

import com.btmi.BTMI;
import com.btmi.belltower.skill.BellTowerSkillManager;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BellTowerSkillPayload(int action, String skillId) implements CustomPacketPayload {
    public static final int ACTION_TOGGLE = 0;
    public static final int ACTION_SELECT = 1;
    public static final int ACTION_CAST = 2;
    public static final int ACTION_SET_CLASS = 3;

    public static final CustomPacketPayload.Type<BellTowerSkillPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BTMI.MODID, "bell_tower_skill"));

    public static final StreamCodec<FriendlyByteBuf, BellTowerSkillPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.action());
                buffer.writeUtf(payload.skillId());
            },
            buffer -> new BellTowerSkillPayload(buffer.readVarInt(), buffer.readUtf()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BellTowerSkillPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            context.enqueueWork(() -> BellTowerSkillManager.handle(payload, player));
        }
    }
}
