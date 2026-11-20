package com.btmi.belltower.skill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.btmi.BTMI;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = BTMI.MODID)
public final class BellTowerMarks {
    private static final Map<UUID, Mark> MARKS = new HashMap<>();

    private BellTowerMarks() {
    }

    public static void mark(ServerPlayer owner, LivingEntity target, int ticks) {
        long expire = target.level().getGameTime() + ticks;
        MARKS.put(target.getUUID(), new Mark(owner.getUUID(), expire));
    }

    public static boolean isMarkedBy(ServerPlayer owner, LivingEntity target) {
        Mark mark = MARKS.get(target.getUUID());
        if (mark == null) {
            return false;
        }
        if (target.level().getGameTime() > mark.expire()) {
            MARKS.remove(target.getUUID());
            return false;
        }
        return mark.owner().equals(owner.getUUID());
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        if (isMarkedBy(attacker, event.getEntity())) {
            event.setAmount(event.getAmount() * 1.25F);
        }
    }

    private record Mark(UUID owner, long expire) {
    }
}
