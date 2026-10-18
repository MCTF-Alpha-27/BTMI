package com.btmi;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MIN_CHARGE_TICKS = BUILDER
            .comment("释放前右键至少需要按住的时长（刻，20 刻 = 1 秒），低于该值不会触发重击。")
            .defineInRange("minChargeTicks", 8, 0, 1200);

    public static final ModConfigSpec.IntValue MAX_CHARGE_TICKS = BUILDER
            .comment("达到满蓄力所需的时长（刻）。超过该时长继续按住不会再增强。")
            .defineInRange("maxChargeTicks", 60, 1, 1200);

    public static final ModConfigSpec.DoubleValue MIN_CHARGE_DAMAGE_MULTIPLIER = BUILDER
            .comment("最小蓄力时重击的伤害倍率（1.0 = 武器的常态攻击伤害）。")
            .defineInRange("minChargeDamageMultiplier", 0.75D, 0.0D, 100.0D);

    public static final ModConfigSpec.DoubleValue MAX_CHARGE_DAMAGE_MULTIPLIER = BUILDER
            .comment("满蓄力时重击的伤害倍率，也就是重击伤害的上限。")
            .defineInRange("maxChargeDamageMultiplier", 2.5D, 0.0D, 100.0D);

    public static final ModConfigSpec.DoubleValue MIN_CHARGE_RADIUS = BUILDER
            .comment("最小蓄力时破坏区域（球体）的半径，单位为格。")
            .defineInRange("minChargeRadius", 1.0D, 0.0D, 64.0D);

    public static final ModConfigSpec.DoubleValue MAX_CHARGE_RADIUS = BUILDER
            .comment("满蓄力时破坏区域的半径（格），也就是破坏规模的上限。")
            .defineInRange("maxChargeRadius", 4.5D, 0.0D, 64.0D);

    public static final ModConfigSpec.BooleanValue BREAK_BLOCKS = BUILDER
            .comment("重击命中世界时是否破坏附近的方块。")
            .define("breakBlocks", true);

    public static final ModConfigSpec.BooleanValue DROP_BLOCKS = BUILDER
            .comment("被破坏的方块是否掉落物品。关闭可显著降低大规模破坏时的卡顿。")
            .define("dropBlocks", false);

    public static final ModConfigSpec.IntValue MAX_BLOCKS_BROKEN = BUILDER
            .comment("单次重击最多可破坏的方块数量上限。")
            .defineInRange("maxBlocksBroken", 512, 0, 100000);

    public static final ModConfigSpec.BooleanValue SCREEN_SHAKE = BUILDER
            .comment("重击是否让附近玩家的镜头震动。")
            .define("screenShake", true);

    public static final ModConfigSpec.DoubleValue SHAKE_STRENGTH = BUILDER
            .comment("镜头震动的基础强度，单位为角度。")
            .defineInRange("shakeStrength", 6.0D, 0.0D, 45.0D);

    // ------------------------------------------------------------------
    // 破折号
    // ------------------------------------------------------------------

    public static final ModConfigSpec.DoubleValue DASH_ORB_DAMAGE = BUILDER
            .comment("右键射出光球造成的伤害。")
            .defineInRange("dashOrbDamage", 20.0D, 0.0D, 1000.0D);

    public static final ModConfigSpec.IntValue DASH_ORB_COOLDOWN_TICKS = BUILDER
            .comment("射出光球后的冷却时间（刻）。")
            .defineInRange("dashOrbCooldownTicks", 14, 0, 1200);

    public static final ModConfigSpec.IntValue DASH_MIN_CHARGE_TICKS = BUILDER
            .comment("劈砍伤害开始随蓄力增长的起点（刻）。低于该值时仍会发动劈砍，但只有最弱的威力。")
            .defineInRange("dashMinChargeTicks", 10, 0, 1200);

    public static final ModConfigSpec.IntValue DASH_MAX_CHARGE_TICKS = BUILDER
            .comment("达到满蓄力所需的按住时长（刻），也就是蓄力上限。")
            .defineInRange("dashMaxChargeTicks", 40, 1, 1200);

    public static final ModConfigSpec.DoubleValue DASH_MAX_DAMAGE_MULTIPLIER = BUILDER
            .comment("满蓄力时劈砍的伤害倍率（相对武器的常态攻击伤害）。")
            .defineInRange("dashMaxDamageMultiplier", 3.0D, 0.0D, 100.0D);

    public static final ModConfigSpec.DoubleValue DASH_MAX_SLASH_RADIUS = BUILDER
            .comment("满蓄力时劈砍的作用半径（格），也就是范围上限。")
            .defineInRange("dashMaxSlashRadius", 3.5D, 0.0D, 64.0D);

    // ------------------------------------------------------------------
    // 示亡号
    // ------------------------------------------------------------------

    public static final ModConfigSpec.IntValue OBELISK_SABER_TICKS = BUILDER
            .comment("左键攻击后，示亡号前端的光剑保持展开的时长（刻）。")
            .defineInRange("obeliskSaberTicks", 40, 1, 1200);

    public static final ModConfigSpec.DoubleValue OBELISK_RAM_DAMAGE = BUILDER
            .comment("悬浮实体突刺冲撞时对单个目标造成的伤害。")
            .defineInRange("obeliskRamDamage", 12.0D, 0.0D, 1000.0D);

    public static final ModConfigSpec.DoubleValue OBELISK_LASER_DAMAGE = BUILDER
            .comment("激光弹命中造成的伤害。")
            .defineInRange("obeliskLaserDamage", 6.0D, 0.0D, 1000.0D);

    public static final ModConfigSpec.IntValue OBELISK_LASER_INTERVAL = BUILDER
            .comment("持续右键时，每多少刻射出一枚激光弹。")
            .defineInRange("obeliskLaserInterval", 4, 1, 200);

    public static final ModConfigSpec.DoubleValue OBELISK_EXPLOSION_RADIUS = BUILDER
            .comment("示亡号引爆时产生的爆炸半径（格）。")
            .defineInRange("obeliskExplosionRadius", 6.0D, 0.0D, 64.0D);

    public static final ModConfigSpec.BooleanValue OBELISK_EXPLOSION_BREAKS_BLOCKS = BUILDER
            .comment("引爆的爆炸是否破坏方块。")
            .define("obeliskExplosionBreaksBlocks", true);

    // ------------------------------------------------------------------
    // 冒号
    // ------------------------------------------------------------------

    public static final ModConfigSpec.DoubleValue COLON_BULLET_DAMAGE = BUILDER
            .comment("普通子弹命中的基础伤害。")
            .defineInRange("colonBulletDamage", 7.0D, 0.0D, 1000.0D);

    public static final ModConfigSpec.DoubleValue COLON_PIERCING_DAMAGE = BUILDER
            .comment("穿甲弹对单个目标造成的伤害。")
            .defineInRange("colonPiercingDamage", 15.0D, 0.0D, 1000.0D);

    public static final ModConfigSpec.DoubleValue COLON_EXPLOSION_RADIUS = BUILDER
            .comment("高爆弹爆炸的半径（格）。")
            .defineInRange("colonExplosionRadius", 4.5D, 0.0D, 64.0D);

    public static final ModConfigSpec.BooleanValue COLON_EXPLOSION_BREAKS_BLOCKS = BUILDER
            .comment("高爆弹的爆炸是否破坏方块。")
            .define("colonExplosionBreaksBlocks", true);

    public static final ModConfigSpec.IntValue COLON_INCENDIARY_RADIUS = BUILDER
            .comment("燃烧弹点火区域的半径（格）。")
            .defineInRange("colonIncendiaryRadius", 3, 0, 16);

    public static final ModConfigSpec.DoubleValue COLON_CRYO_RADIUS = BUILDER
            .comment("冰霜弹影响区域的半径（格）。")
            .defineInRange("colonCryoRadius", 3.5D, 0.0D, 32.0D);

    public static final ModConfigSpec.IntValue COLON_PARALYSIS_TICKS = BUILDER
            .comment("麻痹弹使目标僵直的时长（刻）。")
            .defineInRange("colonParalysisTicks", 120, 1, 1200);

    static final ModConfigSpec SPEC = BUILDER.build();
}
