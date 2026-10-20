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
    // 感叹号
    // ------------------------------------------------------------------

    public static final ModConfigSpec.DoubleValue EXCLAMATION_HOMERUN_DAMAGE = BUILDER
            .comment("全垒打对扇形范围内每个目标造成的伤害。")
            .defineInRange("exclamationHomeRunDamage", 26.0D, 0.0D, 1000.0D);

    public static final ModConfigSpec.DoubleValue EXCLAMATION_HOMERUN_RADIUS = BUILDER
            .comment("全垒打的扇形半径（格）。")
            .defineInRange("exclamationHomeRunRadius", 6.0D, 0.0D, 32.0D);

    public static final ModConfigSpec.DoubleValue EXCLAMATION_HOMERUN_KNOCKBACK = BUILDER
            .comment("全垒打把目标向外击飞的水平速度。")
            .defineInRange("exclamationHomeRunKnockback", 2.0D, 0.0D, 20.0D);

    public static final ModConfigSpec.IntValue EXCLAMATION_HOMERUN_COOLDOWN = BUILDER
            .comment("全垒打的冷却时间（刻）。")
            .defineInRange("exclamationHomeRunCooldown", 160, 0, 7200);

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

    public static final ModConfigSpec.DoubleValue OBELISK_KNELL_DAMAGE = BUILDER
            .comment("丧钟落点中心造成的伤害。")
            .defineInRange("obeliskKnellDamage", 22.0D, 0.0D, 1000.0D);

    public static final ModConfigSpec.DoubleValue OBELISK_KNELL_ARM_DAMAGE = BUILDER
            .comment("光之十字四条臂造成的伤害。")
            .defineInRange("obeliskKnellArmDamage", 12.0D, 0.0D, 1000.0D);

    public static final ModConfigSpec.DoubleValue OBELISK_KNELL_RADIUS = BUILDER
            .comment("丧钟落点中心的伤害半径（格），十字臂从中心向外延伸。")
            .defineInRange("obeliskKnellRadius", 3.0D, 0.0D, 32.0D);

    public static final ModConfigSpec.DoubleValue OBELISK_KNELL_ARM_LENGTH = BUILDER
            .comment("光之十字每条臂的长度（格）。")
            .defineInRange("obeliskKnellArmLength", 6.0D, 0.0D, 32.0D);

    public static final ModConfigSpec.IntValue OBELISK_KNELL_COOLDOWN = BUILDER
            .comment("丧钟的冷却时间（刻）。")
            .defineInRange("obeliskKnellCooldown", 200, 0, 7200);

    public static final ModConfigSpec.DoubleValue OBELISK_KNELL_MARK_BONUS = BUILDER
            .comment("亡印使激光弹伤害提高的比例（0.5 表示 +50%）。")
            .defineInRange("obeliskKnellMarkBonus", 0.5D, 0.0D, 10.0D);

    // ------------------------------------------------------------------
    // 等号 / 斜杠
    // ------------------------------------------------------------------

    public static final ModConfigSpec.DoubleValue DUAL_STRIKETHROUGH_DAMAGE = BUILDER
            .comment("删除线对范围内每个目标造成的伤害。")
            .defineInRange("dualStrikethroughDamage", 24.0D, 0.0D, 1000.0D);

    public static final ModConfigSpec.DoubleValue DUAL_STRIKETHROUGH_RADIUS = BUILDER
            .comment("删除线的伤害半径（格）。")
            .defineInRange("dualStrikethroughRadius", 8.0D, 0.0D, 32.0D);

    public static final ModConfigSpec.IntValue DUAL_STRIKETHROUGH_COOLDOWN = BUILDER
            .comment("删除线的冷却时间（刻）。")
            .defineInRange("dualStrikethroughCooldown", 220, 0, 7200);

    public static final ModConfigSpec.DoubleValue DUAL_SPEAR_DAMAGE = BUILDER
            .comment("删除线触发斩杀后，投掷长矛造成的伤害。")
            .defineInRange("dualSpearDamage", 40.0D, 0.0D, 1000.0D);

    public static final ModConfigSpec.DoubleValue DUAL_SPEAR_RANGE = BUILDER
            .comment("投掷长矛的贯穿距离（格）。")
            .defineInRange("dualSpearRange", 24.0D, 1.0D, 64.0D);

    public static final ModConfigSpec.DoubleValue DUAL_SPEAR_DAMAGE_PER_KILL = BUILDER
            .comment("删除线投矛时，每有一个被斧斩击杀的浮空单位，额外增加的伤害。")
            .defineInRange("dualSpearDamagePerKill", 15.0D, 0.0D, 1000.0D);

    public static final ModConfigSpec.DoubleValue EQUAL_SLASH_DAMAGE = BUILDER
            .comment("等号的断界伤害。")
            .defineInRange("equalSlashDamage", 22.0D, 0.0D, 1000.0D);

    public static final ModConfigSpec.DoubleValue EQUAL_SLASH_RADIUS = BUILDER
            .comment("断界的扇形半径（格）。")
            .defineInRange("equalSlashRadius", 4.5D, 0.0D, 32.0D);

    public static final ModConfigSpec.IntValue EQUAL_SLASH_COOLDOWN = BUILDER
            .comment("断界的冷却时间（刻）。")
            .defineInRange("equalSlashCooldown", 160, 0, 7200);

    public static final ModConfigSpec.DoubleValue SLASH_THRUST_DAMAGE = BUILDER
            .comment("斜杠的贯穿线伤害。")
            .defineInRange("slashThrustDamage", 18.0D, 0.0D, 1000.0D);

    public static final ModConfigSpec.DoubleValue SLASH_THRUST_RANGE = BUILDER
            .comment("贯穿线的距离（格）。")
            .defineInRange("slashThrustRange", 10.0D, 1.0D, 64.0D);

    public static final ModConfigSpec.IntValue SLASH_THRUST_COOLDOWN = BUILDER
            .comment("贯穿线的冷却时间（刻）。")
            .defineInRange("slashThrustCooldown", 140, 0, 7200);

    // ------------------------------------------------------------------
    // 冒号
    // ------------------------------------------------------------------

    public static final ModConfigSpec.IntValue COLON_FAN_COOLDOWN = BUILDER
            .comment("六发齐射的冷却时间（刻）。")
            .defineInRange("colonFanCooldown", 200, 0, 7200);

    public static final ModConfigSpec.IntValue COLON_FAN_INTERVAL = BUILDER
            .comment("六发齐射时每两发之间的间隔（刻）。")
            .defineInRange("colonFanInterval", 2, 1, 40);

    public static final ModConfigSpec.DoubleValue COLON_FAN_SPREAD = BUILDER
            .comment("六发齐射时每发额外增加的最大散布角（弧度）。")
            .defineInRange("colonFanSpread", 0.035D, 0.0D, 1.0D);

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
