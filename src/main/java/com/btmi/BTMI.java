package com.btmi;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.btmi.entity.ColonBullet;
import com.btmi.entity.FlameButterfly;
import com.btmi.entity.GroundCrack;
import com.btmi.entity.LaserBeam;
import com.btmi.entity.LaserBolt;
import com.btmi.entity.LightOrb;
import com.btmi.entity.ObeliskSentinel;
import com.btmi.entity.ProjectionPortal;
import com.btmi.entity.RedLine;
import com.btmi.entity.SpearBeam;
import com.btmi.item.BulletItem;
import com.btmi.item.ColonItem;
import com.btmi.item.CrimsonFangItem;
import com.btmi.item.DashItem;
import com.btmi.item.EqualSignItem;
import com.btmi.item.ExclamationMarkItem;
import com.btmi.item.FlameCapeItem;
import com.btmi.item.ObeliskItem;
import com.btmi.item.ObeliskRecallItem;
import com.btmi.item.SlashItem;
import com.btmi.menu.ColonMenu;
import com.btmi.weapon.ModWeaponTypes;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(BTMI.MODID)
public class BTMI {
    public static final String MODID = "btmi";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MODID);

    // ------------------------------------------------------------------
    // 武器一：感叹号
    // ------------------------------------------------------------------

    public static final DeferredItem<ExclamationMarkItem> EXCLAMATION_MARK = ITEMS.register("exclamation_mark",
            () -> new ExclamationMarkItem(new Item.Properties()
                    .attributes(ExclamationMarkItem.createWeaponAttributes())
                    .durability(2031)
                    .rarity(Rarity.RARE)
                    .fireResistant()
                    .stacksTo(1)));

    // ------------------------------------------------------------------
    // 武器二：破折号
    // ------------------------------------------------------------------

    // 猩红之牙的开火模式（0 连发 / 1 激光 / 2 霰弹）
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FIRE_MODE = DATA_COMPONENTS.register(
            "fire_mode",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());
    public static final DeferredItem<DashItem> DASH = ITEMS.register("dash",
            () -> new DashItem(new Item.Properties()
                    .attributes(DashItem.createWeaponAttributes())
                    .durability(1500)
                    .rarity(Rarity.EPIC)
                    .fireResistant()
                    .stacksTo(1)));

    // ------------------------------------------------------------------
    // 猩红之牙
    // ------------------------------------------------------------------

    public static final DeferredItem<CrimsonFangItem> CRIMSON_FANG = ITEMS.register("crimson_fang",
            () -> new CrimsonFangItem(new Item.Properties()
                    .attributes(CrimsonFangItem.createWeaponAttributes())
                    .durability(2000)
                    .rarity(Rarity.EPIC)
                    .fireResistant()
                    .stacksTo(1)));

    // ------------------------------------------------------------------
    // 双持联动：等号 / 斜杠
    // ------------------------------------------------------------------

    public static final DeferredItem<EqualSignItem> EQUAL_SIGN = ITEMS.register("equal_sign",
            () -> new EqualSignItem(new Item.Properties()
                    .attributes(EqualSignItem.createWeaponAttributes())
                    .durability(1800)
                    .rarity(Rarity.EPIC)
                    .fireResistant()
                    .stacksTo(1)));

    public static final DeferredItem<SlashItem> SLASH = ITEMS.register("slash",
            () -> new SlashItem(new Item.Properties()
                    .attributes(SlashItem.createWeaponAttributes())
                    .durability(1500)
                    .rarity(Rarity.EPIC)
                    .fireResistant()
                    .stacksTo(1)));

    // ------------------------------------------------------------------
    // 烈焰蝶
    // ------------------------------------------------------------------

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, MODID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> FLAME_CAPE_MATERIAL = ARMOR_MATERIALS.register(
            "flame_cape",
            () -> new ArmorMaterial(
                    Map.of(
                            ArmorItem.Type.HELMET, 3,
                            ArmorItem.Type.CHESTPLATE, 7,
                            ArmorItem.Type.LEGGINGS, 5,
                            ArmorItem.Type.BOOTS, 3),
                    18,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(Items.BLAZE_POWDER),
                    List.of(new ArmorMaterial.Layer(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "flame_butterfly"))),
                    2.0F,
                    0.05F));

    public static final DeferredItem<FlameCapeItem> FLAME_CAPE = ITEMS.register("flame_cape",
            () -> new FlameCapeItem(FLAME_CAPE_MATERIAL, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties()
                            .durability(ArmorItem.Type.CHESTPLATE.getDurability(33))
                            .rarity(Rarity.EPIC)
                            .fireResistant()));

    // ------------------------------------------------------------------
    // 材料
    // ------------------------------------------------------------------

    public static final DeferredItem<Item> BLACK_STEEL_INGOT = ITEMS.register("black_steel_ingot",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> CRIMSON_ALLOY = ITEMS.register("crimson_alloy",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ENERGY_CORE = ITEMS.register("energy_core",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> LIGHT_ORB = ITEMS.register("light_orb",
            () -> new Item(new Item.Properties()));

    // ------------------------------------------------------------------
    // 武器三：示亡号
    // ------------------------------------------------------------------

    public static final DeferredItem<ObeliskItem> OBELISK = ITEMS.register("obelisk",
            () -> new ObeliskItem(new Item.Properties()
                    .attributes(ObeliskItem.createWeaponAttributes())
                    .durability(1800)
                    .rarity(Rarity.EPIC)
                    .fireResistant()
                    .stacksTo(1)));

    public static final DeferredItem<ObeliskRecallItem> OBELISK_RECALL = ITEMS.register("obelisk_recall",
            () -> new ObeliskRecallItem(new Item.Properties()
                    .attributes(ObeliskItem.createWeaponAttributes())
                    .durability(1800)
                    .rarity(Rarity.EPIC)
                    .fireResistant()
                    .stacksTo(1)));

    public static final DeferredItem<Item> LASER_BOLT = ITEMS.register("laser_bolt",
            () -> new Item(new Item.Properties()));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> LIT = DATA_COMPONENTS.register(
            "lit",
            () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> UNSHEATHED = DATA_COMPONENTS.register(
            "unsheathed",
            () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    // ------------------------------------------------------------------
    // 武器四：冒号
    // ------------------------------------------------------------------

    public static final DeferredItem<ColonItem> COLON = ITEMS.register("colon",
            () -> new ColonItem(new Item.Properties()
                    .attributes(ObeliskItem.createWeaponAttributes())
                    .durability(1200)
                    .rarity(Rarity.EPIC)
                    .stacksTo(1)));

    public static final DeferredItem<Item> HIGH_EXPLOSIVE_BULLET = ITEMS.register("high_explosive_bullet",
            () -> new BulletItem(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> INCENDIARY_BULLET = ITEMS.register("incendiary_bullet",
            () -> new BulletItem(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PARALYSIS_BULLET = ITEMS.register("paralysis_bullet",
            () -> new BulletItem(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> PIERCING_BULLET = ITEMS.register("piercing_bullet",
            () -> new BulletItem(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> CRYO_BULLET = ITEMS.register("cryo_bullet",
            () -> new BulletItem(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> SEEKER_BULLET = ITEMS.register("seeker_bullet",
            () -> new BulletItem(new Item.Properties().stacksTo(64)));

    public static final List<DeferredItem<Item>> BULLET_ITEMS = List.of(
            HIGH_EXPLOSIVE_BULLET, INCENDIARY_BULLET, PARALYSIS_BULLET,
            PIERCING_BULLET, CRYO_BULLET, SEEKER_BULLET);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<ItemStack>>> AMMO = DATA_COMPONENTS.register(
            "ammo",
            () -> DataComponentType.<List<ItemStack>>builder()
                    .persistent(ItemStack.OPTIONAL_CODEC.listOf())
                    .networkSynchronized(ItemStack.OPTIONAL_LIST_STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SELECTED_AMMO = DATA_COMPONENTS.register(
            "selected_ammo",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    public static final DeferredHolder<MenuType<?>, MenuType<ColonMenu>> COLON_MENU = MENU_TYPES.register(
            "colon",
            () -> IMenuTypeExtension.create((windowId, inventory, data) -> new ColonMenu(windowId, inventory)));

    // ------------------------------------------------------------------
    // 实体
    // ------------------------------------------------------------------

    public static final DeferredHolder<EntityType<?>, EntityType<LightOrb>> LIGHT_ORB_ENTITY = ENTITY_TYPES.register(
            "light_orb",
            () -> EntityType.Builder.<LightOrb>of(LightOrb::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(6)
                    .updateInterval(2)
                    .build("light_orb"));

    public static final DeferredHolder<EntityType<?>, EntityType<ObeliskSentinel>> OBELISK_SENTINEL = ENTITY_TYPES.register(
            "obelisk_sentinel",
            () -> EntityType.Builder.<ObeliskSentinel>of(ObeliskSentinel::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("obelisk_sentinel"));

    public static final DeferredHolder<EntityType<?>, EntityType<LaserBolt>> LASER_BOLT_ENTITY = ENTITY_TYPES.register(
            "laser_bolt",
            () -> EntityType.Builder.<LaserBolt>of(LaserBolt::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("laser_bolt"));

    public static final DeferredHolder<EntityType<?>, EntityType<FlameButterfly>> FLAME_BUTTERFLY = ENTITY_TYPES.register(
            "flame_butterfly",
            () -> EntityType.Builder.<FlameButterfly>of(FlameButterfly::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("flame_butterfly"));
    public static final DeferredHolder<EntityType<?>, EntityType<LaserBeam>> LASER_BEAM = ENTITY_TYPES.register(
            "laser_beam",
            () -> EntityType.Builder.<LaserBeam>of(LaserBeam::new, MobCategory.MISC)
                    .sized(0.2F, 0.2F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("laser_beam"));
    public static final DeferredHolder<EntityType<?>, EntityType<RedLine>> RED_LINE = ENTITY_TYPES.register(
            "red_line",
            () -> EntityType.Builder.<RedLine>of(RedLine::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("red_line"));
    public static final DeferredHolder<EntityType<?>, EntityType<SpearBeam>> SPEAR_BEAM = ENTITY_TYPES.register(
            "spear_beam",
            () -> EntityType.Builder.<SpearBeam>of(SpearBeam::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .clientTrackingRange(24)
                    .updateInterval(1)
                    .build("spear_beam"));
    public static final DeferredHolder<EntityType<?>, EntityType<GroundCrack>> GROUND_CRACK = ENTITY_TYPES.register(
            "ground_crack",
            () -> EntityType.Builder.<GroundCrack>of(GroundCrack::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("ground_crack"));
    public static final DeferredHolder<EntityType<?>, EntityType<ProjectionPortal>> PROJECTION_PORTAL = ENTITY_TYPES.register(
            "projection_portal",
            () -> EntityType.Builder.<ProjectionPortal>of(ProjectionPortal::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build("projection_portal"));
    public static final DeferredHolder<EntityType<?>, EntityType<ColonBullet>> COLON_BULLET = ENTITY_TYPES.register(
            "colon_bullet",
            () -> EntityType.Builder.<ColonBullet>of(ColonBullet::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("colon_bullet"));

    // 本模组的创造模式物品栏，位置在原版战斗标签页之前
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.btmi"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> CRIMSON_FANG.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(CRIMSON_FANG.get());
                        output.accept(FLAME_CAPE.get());
                        output.accept(BLACK_STEEL_INGOT.get());
                        output.accept(CRIMSON_ALLOY.get());
                        output.accept(ENERGY_CORE.get());
                        output.accept(EXCLAMATION_MARK.get());
                        output.accept(DASH.get());
                        output.accept(EQUAL_SIGN.get());
                        output.accept(SLASH.get());
                        output.accept(OBELISK.get());
                        output.accept(COLON.get());
                        BULLET_ITEMS.forEach(bullet -> output.accept(bullet.get()));
                    })
                    .build());

    public BTMI(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        ARMOR_MATERIALS.register(modEventBus);
        DATA_COMPONENTS.register(modEventBus);
        MENU_TYPES.register(modEventBus);

        ModWeaponTypes.register();

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
