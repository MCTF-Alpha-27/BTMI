package com.btmi;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.btmi.entity.BellToll;
import com.btmi.entity.ColonBullet;
import com.btmi.entity.FlameButterfly;
import com.btmi.entity.GroundCrack;
import com.btmi.entity.LaserBeam;
import com.btmi.entity.LaserBolt;
import com.btmi.entity.LightOrb;
import com.btmi.entity.DeathSignSentinel;
import com.btmi.entity.KnellCharge;
import com.btmi.entity.KnellStrike;
import com.btmi.entity.ProjectionPortal;
import com.btmi.entity.ProjectionShockwave;
import com.btmi.entity.RedLine;
import com.btmi.entity.SpearBeam;
import com.btmi.block.BloodRainMortarBlock;
import com.btmi.block.BloodRainMortarBlockEntity;
import com.btmi.block.MessengerBoxBlock;
import com.btmi.block.MessengerBoxBlockEntity;
import com.btmi.block.TowerCoreBlock;
import com.btmi.block.TowerCoreBlockEntity;
import com.btmi.block.TowerCoreMenu;
import com.btmi.belltower.BellTowerPermission;
import com.btmi.entity.BellTowerClock;
import com.btmi.entity.BloodRainShell;
import com.btmi.item.BloodRainMortarItem;
import com.btmi.item.BloodRainShellItem;
import com.btmi.item.BulletItem;
import com.btmi.item.ChairmanFileItem;
import com.btmi.item.ColonItem;
import com.btmi.item.FixedChairmanFileItem;
import com.btmi.item.ScarletFangItem;
import com.btmi.item.DashItem;
import com.btmi.item.EqualSignItem;
import com.btmi.item.ExclamationMarkItem;
import com.btmi.item.FlameCapeItem;
import com.btmi.item.DeathSignItem;
import com.btmi.item.DeathSignRecallItem;
import com.btmi.item.SlashItem;
import com.btmi.item.StateAffairsChronicleItem;
import com.btmi.item.StorytellerBookItem;
import com.btmi.menu.BloodRainMenu;
import com.btmi.menu.ColonMenu;
import com.btmi.weapon.ModWeaponTypes;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@Mod(BTMI.MODID)
public class BTMI {
    public static final String MODID = "btmi";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> BELL_TOWER =
            ATTACHMENT_TYPES.register("bell_tower", BellTowerPermission::create);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<String>> BELL_TOWER_ACTIVE =
            ATTACHMENT_TYPES.register("bell_tower_active", () -> AttachmentType.builder(() -> "")
                    .serialize(Codec.STRING)
                    .copyOnDeath()
                    .sync(ByteBufCodecs.STRING_UTF8)
                    .build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<String>> BELL_TOWER_DISABLED_PASSIVES =
            ATTACHMENT_TYPES.register("bell_tower_disabled_passives", () -> AttachmentType.builder(() -> "")
                    .serialize(Codec.STRING)
                    .copyOnDeath()
                    .sync(ByteBufCodecs.STRING_UTF8)
                    .build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<String>> BELL_TOWER_CLASS =
            ATTACHMENT_TYPES.register("bell_tower_class", () -> AttachmentType.builder(() -> "none")
                    .serialize(Codec.STRING)
                    .copyOnDeath()
                    .sync(ByteBufCodecs.STRING_UTF8)
                    .build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<String>> BELL_TOWER_QUESTS =
            ATTACHMENT_TYPES.register("bell_tower_quests", () -> AttachmentType.builder(() -> "")
                    .serialize(Codec.STRING)
                    .copyOnDeath()
                    .sync(ByteBufCodecs.STRING_UTF8)
                    .build());

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

    public static final DeferredItem<ScarletFangItem> SCARLET_FANG = ITEMS.register("scarlet_fang",
            () -> new ScarletFangItem(new Item.Properties()
                    .attributes(ScarletFangItem.createWeaponAttributes())
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

    public static final DeferredItem<StorytellerBookItem> STORYTELLER_BOOK = ITEMS.register("storyteller_book",
            () -> new StorytellerBookItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredBlock<MessengerBoxBlock> MESSENGER_BOX_BLOCK = BLOCKS.register("messenger_box",
            () -> new MessengerBoxBlock(BlockBehaviour.Properties.of()
                    .strength(3.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));
    public static final DeferredItem<BlockItem> MESSENGER_BOX_ITEM = ITEMS.register("messenger_box",
            () -> new BlockItem(MESSENGER_BOX_BLOCK.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MessengerBoxBlockEntity>> MESSENGER_BOX_BE =
            BLOCK_ENTITY_TYPES.register("messenger_box",
                    () -> BlockEntityType.Builder.of(MessengerBoxBlockEntity::new, MESSENGER_BOX_BLOCK.get()).build(null));

    public static final DeferredBlock<TowerCoreBlock> TOWER_CORE = BLOCKS.register("tower_core",
            () -> new TowerCoreBlock(BlockBehaviour.Properties.of()
                    .strength(5.0F, 1200.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));
    public static final DeferredItem<BlockItem> TOWER_CORE_ITEM = ITEMS.register("tower_core",
            () -> new BlockItem(TOWER_CORE.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TowerCoreBlockEntity>> TOWER_CORE_BE =
            BLOCK_ENTITY_TYPES.register("tower_core",
                    () -> BlockEntityType.Builder.of(TowerCoreBlockEntity::new, TOWER_CORE.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<TowerCoreMenu>> TOWER_CORE_MENU = MENU_TYPES.register(
            "tower_core",
            () -> IMenuTypeExtension.create((windowId, inventory, data) -> {
                BlockPos pos = data.readBlockPos();
                if (inventory.player.level().getBlockEntity(pos) instanceof TowerCoreBlockEntity core) {
                    return new TowerCoreMenu(windowId, inventory, pos, core);
                }
                return new TowerCoreMenu(windowId, inventory, pos, new SimpleContainer(1));
            }));

    public static final DeferredItem<ChairmanFileItem> CHAIRMAN_FILE_APPOINTMENT =
            ITEMS.register("chairman_file_appointment",
                    () -> new ChairmanFileItem("file_appointment",
                            new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<ChairmanFileItem> CHAIRMAN_FILE_RECORD =
            ITEMS.register("chairman_file_record",
                    () -> new ChairmanFileItem("file_record",
                            new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<ChairmanFileItem> CHAIRMAN_FILE_LAST =
            ITEMS.register("chairman_file_last",
                    () -> new ChairmanFileItem("file_last",
                            new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<FixedChairmanFileItem> CHAIRMAN_FILE =
            ITEMS.register("chairman_file",
                    () -> new FixedChairmanFileItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> DIGAMMA = ITEMS.register("digamma",
            () -> new Item(new Item.Properties().durability(100).stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<StateAffairsChronicleItem> STATE_AFFAIRS_CHRONICLE =
            ITEMS.register("state_affairs_chronicle",
                    () -> new StateAffairsChronicleItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    // ------------------------------------------------------------------
    // 武器三：示亡号
    // ------------------------------------------------------------------

    public static final DeferredItem<DeathSignItem> DEATH_SIGN = ITEMS.register("death_sign",
            () -> new DeathSignItem(new Item.Properties()
                    .attributes(DeathSignItem.createWeaponAttributes())
                    .durability(1800)
                    .rarity(Rarity.EPIC)
                    .fireResistant()
                    .stacksTo(1)));

    public static final DeferredItem<DeathSignRecallItem> DEATH_SIGN_RECALL = ITEMS.register("death_sign_recall",
            () -> new DeathSignRecallItem(new Item.Properties()
                    .attributes(DeathSignItem.createWeaponAttributes())
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
                    .attributes(DeathSignItem.createWeaponAttributes())
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
    // 武器九：血雨
    // ------------------------------------------------------------------

    public static final DeferredBlock<BloodRainMortarBlock> BLOOD_RAIN_MORTAR = BLOCKS.register("blood_rain_mortar",
            () -> new BloodRainMortarBlock(BlockBehaviour.Properties.of()
                    .strength(5.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredItem<BlockItem> BLOOD_RAIN_MORTAR_ITEM = ITEMS.register("blood_rain_mortar",
            () -> new BloodRainMortarItem(BLOOD_RAIN_MORTAR.get(), new Item.Properties()));

    public static final DeferredItem<BloodRainShellItem> BLOOD_RAIN_SHELL = ITEMS.register("blood_rain_shell",
            () -> new BloodRainShellItem(new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BloodRainMortarBlockEntity>> BLOOD_RAIN_MORTAR_BE =
            BLOCK_ENTITY_TYPES.register("blood_rain_mortar",
                    () -> BlockEntityType.Builder.of(BloodRainMortarBlockEntity::new, BLOOD_RAIN_MORTAR.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<BloodRainMenu>> BLOOD_RAIN_MORTAR_MENU = MENU_TYPES.register(
            "blood_rain_mortar",
            () -> IMenuTypeExtension.create((windowId, inventory, data) -> new BloodRainMenu(windowId, inventory, data.readBlockPos())));

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

    public static final DeferredHolder<EntityType<?>, EntityType<DeathSignSentinel>> DEATH_SIGN_SENTINEL = ENTITY_TYPES.register(
            "death_sign_sentinel",
            () -> EntityType.Builder.<DeathSignSentinel>of(DeathSignSentinel::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("death_sign_sentinel"));

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
    public static final DeferredHolder<EntityType<?>, EntityType<ProjectionShockwave>> PROJECTION_SHOCKWAVE = ENTITY_TYPES.register(
            "projection_shockwave",
            () -> EntityType.Builder.<ProjectionShockwave>of(ProjectionShockwave::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("projection_shockwave"));
    public static final DeferredHolder<EntityType<?>, EntityType<ColonBullet>> COLON_BULLET = ENTITY_TYPES.register(
            "colon_bullet",
            () -> EntityType.Builder.<ColonBullet>of(ColonBullet::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("colon_bullet"));
    public static final DeferredHolder<EntityType<?>, EntityType<BloodRainShell>> BLOOD_RAIN_SHELL_ENTITY = ENTITY_TYPES.register(
            "blood_rain_shell",
            () -> EntityType.Builder.<BloodRainShell>of(BloodRainShell::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("blood_rain_shell"));
    public static final DeferredHolder<EntityType<?>, EntityType<KnellCharge>> KNELL_CHARGE = ENTITY_TYPES.register(
            "knell_charge",
            () -> EntityType.Builder.<KnellCharge>of(KnellCharge::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .build("knell_charge"));
    public static final DeferredHolder<EntityType<?>, EntityType<KnellStrike>> KNELL_STRIKE = ENTITY_TYPES.register(
            "knell_strike",
            () -> EntityType.Builder.<KnellStrike>of(KnellStrike::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .clientTrackingRange(20)
                    .updateInterval(1)
                    .build("knell_strike"));
    public static final DeferredHolder<EntityType<?>, EntityType<BellTowerClock>> BELL_TOWER_CLOCK = ENTITY_TYPES.register(
            "bell_tower_clock",
            () -> EntityType.Builder.<BellTowerClock>of(BellTowerClock::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .clientTrackingRange(200)
                    .updateInterval(2)
                    .build("bell_tower_clock"));
    public static final DeferredHolder<EntityType<?>, EntityType<BellToll>> BELL_TOLL_ENTITY = ENTITY_TYPES.register(
            "bell_toll",
            () -> EntityType.Builder.<BellToll>of(BellToll::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build("bell_toll"));

    // 本模组的创造模式物品栏，位置在原版战斗标签页之前
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.btmi"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> SCARLET_FANG.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(SCARLET_FANG.get());
                        output.accept(FLAME_CAPE.get());
                        output.accept(BLACK_STEEL_INGOT.get());
                        output.accept(CRIMSON_ALLOY.get());
                        output.accept(ENERGY_CORE.get());
                        output.accept(EXCLAMATION_MARK.get());
                        output.accept(DASH.get());
                        output.accept(EQUAL_SIGN.get());
                        output.accept(SLASH.get());
                        output.accept(DEATH_SIGN.get());
                        output.accept(COLON.get());
                        BULLET_ITEMS.forEach(bullet -> output.accept(bullet.get()));
                        output.accept(BLOOD_RAIN_MORTAR_ITEM.get());
                        output.accept(BLOOD_RAIN_SHELL.get());
                        output.accept(STORYTELLER_BOOK.get());
                        output.accept(MESSENGER_BOX_ITEM.get());
                        output.accept(CHAIRMAN_FILE.get());
                        output.accept(DIGAMMA.get());
                        output.accept(STATE_AFFAIRS_CHRONICLE.get());
                        output.accept(TOWER_CORE_ITEM.get());
                    })
                    .build());

    public BTMI(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        ARMOR_MATERIALS.register(modEventBus);
        DATA_COMPONENTS.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        BLOCKS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        ATTACHMENT_TYPES.register(modEventBus);

        ModWeaponTypes.register();

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
