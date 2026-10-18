package com.btmi.weapon;

import com.btmi.BTMI;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.AnimationManager.AnimationBuilder;
import yesman.epicfight.api.animation.AnimationManager.AnimationRegistryEvent;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Armatures;

// 在代码里注册本模组的动画：这样注册时机早于武器类型解析，
// livingmotion_modifier 里引用它们才不会拿到 null。
// 关键帧仍然来自 assets/btmi/animmodels/animations/biped/** 下的 json。
@EventBusSubscriber(modid = BTMI.MODID)
public final class ModAnimations {
    public static AnimationManager.AnimationAccessor<StaticAnimation> COLON_HOLD;
    public static AnimationManager.AnimationAccessor<StaticAnimation> COLON_SHOT;
    public static AnimationManager.AnimationAccessor<StaticAnimation> DASH_SHOT;

    private ModAnimations() {
    }

    @SubscribeEvent
    public static void registerAnimations(AnimationRegistryEvent event) {
        AnimationManager.addNoWarningModId(BTMI.MODID);
        event.newBuilder(BTMI.MODID, ModAnimations::build);
    }

    private static void build(AnimationBuilder builder) {
        // 持握：循环，右臂前伸
        COLON_HOLD = builder.nextAccessor("biped/living/colon_hold",
                (accessor) -> new StaticAnimation(true, accessor, Armatures.BIPED));
        // 开火：一次性后坐
        COLON_SHOT = builder.nextAccessor("biped/combat/colon_shot",
                (accessor) -> new StaticAnimation(false, accessor, Armatures.BIPED));
        // 破折号开火：双手前伸瞄准 + 后坐
        DASH_SHOT = builder.nextAccessor("biped/combat/dash_shot",
                (accessor) -> new StaticAnimation(false, accessor, Armatures.BIPED));
    }
}
