# 钟楼军工 / Bell Tower Military Industry (BTMI)

一个基于 **史诗战斗（Epic Fight）** 的 NeoForge 附属模组。

## 网络环境说明

本项目所在的网络环境无法直连部分海外源，因此做了如下处理，**如果你的网络可以直接访问官方源，可以按注释删除对应改动**：

1. `gradle/wrapper/gradle-wrapper.properties`
   - `distributionUrl` 改为腾讯云镜像（`services.gradle.org` 无法连接）。

2. `settings.gradle`
   - 增加了一段把 `maven.neoforged.net` 重写为 `neoforged.forgecdn.net` 的逻辑（前者在此网络下被 DNS 污染）。

## 前置依赖

Epic Fight 通过 **Curse Maven** 自动解析（CurseForge 项目 `405076`，文件 `8175609`，对应 `21.17.3.1-mc1.21.1-neoforge`）。构建时需能访问 `cursemaven.com`：

## 目录结构

```
src/main/java/com/btmi/
├── BTMI.java                    # 主类：物品 / 方块 / 实体 / 数据组件 / 菜单 / 权限附件 / 创造栏
├── BTMIClient.java              # 客户端入口（配置界面）
├── Config.java                  # 配置定义
├── command/
│   └── BTMICommands.java          # /btmi 测试指令：钟楼权限与血雨
├── belltower/
│   ├── BellTowerPermission.java   # 钟楼权限附件（持久化 + 同步）
│   ├── BellTowerClass.java / BellTowerClasses.java  # 两种职业与选择逻辑
│   ├── BellTowerQuests.java       # 说书人任务、文件收集与认可判定
│   ├── VillageFileLoot.java       # 把有些年代的文件注入村庄箱子
│   ├── BellTowerNameFormat.java   # 玩家名前显示职业
│   ├── BellTowerShield.java       # 护盾半径与生效条件
│   ├── BellTowerShieldEvents.java # 护盾拦截弹射物与推开生物
│   └── skill/
│       ├── BellTowerSkills.java      # 技能注册表、职业过滤与开关状态
│       ├── BellTowerSkillManager.java # 主动技能冷却、施放与状态同步
│       ├── BellTowerSkillEffects.java # 通用技能与职业技能
│       ├── BellTowerMarks.java       # 标记与增伤
│       ├── BellTowerSkillScheduler.java # 技能动作阶段的延时结算
│       └── BellTowerSkillEvents.java  # 登录与重生时补发技能状态
├── item/
│   ├── ExclamationMarkItem.java / SmashHandler.java        # 感叹号：蓄力重击 / 全垒打
│   ├── DashItem.java / DashAbilityHandler.java / DashProjectionSkill.java
│   ├── DeathSignItem.java / DeathSignRecallItem.java / DeathSignHandler.java
│   ├── ColonItem.java / ColonAmmo.java / ColonHandler.java
│   ├── BulletItem.java / BulletType.java                   # 六种子弹
│   ├── ScarletFangItem.java / ScarletFangHandler.java
│   ├── FlameCapeItem.java / FlameCapeHandler.java
│   ├── EqualSignItem.java                                  # 等号（双头斧）
│   ├── SlashItem.java                                      # 斜杠（矛）
│   ├── BloodRainMortarItem.java / BloodRainShellItem.java  # 血雨：炮筒 / 发射弹
│   ├── StorytellerBookItem.java                               # 说书人之书
│   ├── FixedChairmanFileItem.java                             # 有些年代的文件
│   ├── StateAffairsChronicleItem.java                         # 国事纪要
│   └── DualWeaponHandler.java                              # 双持联动：等分 / 贯穿线 / 删除线
├── block/
│   ├── BloodRainMortarBlock.java / BloodRainMortarBlockEntity.java
│   └── MessengerBoxBlock.java / MessengerBoxBlockEntity.java / MessengerStrike.java
├── bloodrain/
│   └── BloodRainHandler.java       # 血雨天气计时与同步
├── entity/
│   ├── LightOrb.java / LaserBolt.java / LaserBeam.java
│   ├── ColonBullet.java
│   ├── DeathSignSentinel.java / ProjectionPortal.java / ProjectionShockwave.java
│   ├── FlameButterfly.java
│   ├── BloodRainShell.java
│   ├── KnellCharge.java / KnellStrike.java              # 示亡号丧钟的贴图特效实体
│   ├── BellTowerClock.java                              # 信使匣高空的巨大钟表盘
│   └── RedLine.java / SpearBeam.java / GroundCrack.java    # 红线 / 投矛光束 / 地面裂隙
├── menu/
│   └── ColonMenu.java / BloodRainMenu.java
├── weapon/
│   ├── ModWeaponTypes.java        # 武器类型相关的代码钩子
│   └── ModAnimations.java         # 代码侧动画注册（冒号 / 等号 / 举矛）
├── network/
│   ├── ModNetworking.java
│   ├── ShakePayload.java / DashChargePayload.java
│   ├── DashActionPayload.java / DeathSignActionPayload.java / ColonActionPayload.java
│   ├── ScarletFangPayload.java / FlameCapePayload.java
│   ├── BloodRainPayload.java
│   ├── BellTowerSkillPayload.java / BellTowerSkillStatePayload.java
│   ├── BellTowerOverlayPayload.java
│   ├── ChronicleOpenPayload.java
│   └── ExclamationPayload.java / DualWeaponPayload.java
└── client/
    ├── ModClientSetup.java / ModKeyMappings.java / ClientShakeHandler.java
    ├── DashClientInput.java / DashClientAnimations.java / DashChargeHud.java
    ├── DashStaffRenderer.java / DashStaffItemRenderer.java
    ├── DeathSignClientInput.java / DeathSignRenderer.java / DeathSignItemRenderer.java
    ├── DeathSignSentinelRenderer.java
    ├── ColonClientInput.java / ColonRecoilHandler.java / ColonBulletRenderer.java
    ├── AmmoWheelHandler.java / AmmoWheelScreen.java / ColonScreen.java
    ├── ScarletFangClientInput.java / GunFirstPersonHandler.java
    ├── FlameCapeCloakRenderer.java / FlameCapeShape.java / FlameCapeClient.java
    ├── FlameButterflyRenderer.java
    ├── BloodRainClient.java / BloodRainScreen.java
    ├── BellTowerShieldRenderer.java  # 血雨期间的钟楼权限护盾
    ├── BellTowerSkillClient.java / BellTowerSkillScreen.java / BellTowerSkillInput.java
    ├── BellTowerOverlay.java          # 红色任务 / 天启信息浮层
    ├── KnellChargeRenderer.java / KnellStrikeRenderer.java
    ├── BellTowerClockRenderer.java
    ├── StateAffairsChronicleClient.java / StateAffairsChronicleScreen.java
    ├── LightOrbRenderer.java / ProjectionPortalRenderer.java / ProjectionShockwaveRenderer.java
    ├── RedLineRenderer.java / SpearBeamRenderer.java / GroundCrackRenderer.java
    └── LaserBoltRenderer.java / LaserBeamRenderer.java
```

资源目录按用途分组：

```
src/main/resources/
├── assets/btmi/
│   ├── lang/                    # 中英文语言文件
│   ├── blockstates/             # 血雨迫击炮方块状态
│   ├── models/item/             # 物品模型：2D 图标与 *_3d / *_3d_fp 手持模型
│   ├── models/block/            # 方块模型
│   ├── textures/item/           # 物品图标与 3D 模型贴图
│   ├── textures/block/          # 方块贴图
│   ├── textures/environment/    # 血雨雨滴材质
│   ├── textures/gui/skill/      # 钟楼技能图标
│   ├── textures/entity/         # 实体、技能特效与武器贴图
│   ├── textures/models/armor/   # 烈焰蝶护甲贴图
│   ├── animmodels/animations/biped/
│   │   ├── combat/              # 冒号射击 / 等号断界 / 投矛等战斗动画
│   │   └── living/              # 持枪待机 / 投矛瞄准等待机动画
│   └── item_skins/              # Epic Fight 物品皮肤与第一人称设置
└── data/btmi/
    ├── capabilities/weapons/    # Epic Fight 武器能力
    │   └── types/               # 自定义武器类型：斜杠 / 等号 / 冒号 / 猩红之牙
    ├── structure/bell_tower.nbt # 主世界钟楼结构
    ├── worldgen/                # 钟楼的自然生成配置
    │   ├── structure/bell_tower.json
    │   ├── structure_set/bell_towers.json
    │   └── template_pool/bell_tower.json
    ├── tags/worldgen/biome/has_bell_tower.json
    ├── loot_table/blocks/       # 方块掉落表
    ├── loot_table/chests/       # 钟楼战利品
    ├── recipe/                  # 合成表
    └── advancement/             # 进度
```

---

## 测试指令

- `/btmi bell grant`：创造模式下为自己获取钟楼权限
- `/btmi bell grant <玩家>`：管理员为其他创造模式玩家授予钟楼权限
- `/btmi bell revoke`：放弃自己的钟楼权限
- `/btmi bell status`：查看自己是否拥有钟楼权限
- `/btmi bloodrain start`：本维度立即降下血雨
- `/btmi bloodrain stop`：本维度立即停止血雨
- `/btmi class set <harbinger|agent|none>`：测试用，直接设置职业
- `/btmi class reset`：清空职业
- `/btmi quest status`：在物品栏上方显示任务进度
- `/btmi quest complete`：直接补齐三份文件（测试用）
- `/btmi quest reset`：重置说书人任务
- `/btmi energy fill`：把自己的钟楼能量补满（测试用）
- `/btmi energy set <0-100>`：设置自己的钟楼能量（测试用）

---

## 许可证

本项目以 **GNU General Public License v3.0（GPL-3.0）** 发布，与史诗战斗的代码许可保持一致。
