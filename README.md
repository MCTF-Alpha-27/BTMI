# 钟楼军工 / Bell Tower Military Industry (BTMI)

一个基于 **史诗战斗（Epic Fight）** 的 NeoForge 附属模组。

## 网络环境说明

本项目所在的网络环境无法直连部分海外源，因此做了如下处理，**如果你的网络可以直接访问官方源，可以按注释删除对应改动**：

1. `gradle/wrapper/gradle-wrapper.properties`
   - `distributionUrl` 改为腾讯云镜像（`services.gradle.org` 无法连接）。

2. `settings.gradle`
   - 增加了一段把 `maven.neoforged.net` 重写为 `neoforged.forgecdn.net` 的逻辑（前者在此网络下被 DNS 污染）。

3. `libs/epic-fight-21.17.3.1-mc1.21.1-neoforge.jar`
   - Epic Fight 以本地 jar 形式依赖（`flatDir { dir 'libs' }`）。

## 目录结构

```
src/main/java/com/btmi/
├── BTMI.java                    # 主类：物品 / 实体 / 数据组件 / 菜单 / 创造栏
├── BTMIClient.java              # 客户端入口（配置界面）
├── Config.java                  # 配置定义
├── item/
│   ├── ExclamationMarkItem.java / SmashHandler.java        # 感叹号：蓄力重击 / 全垒打
│   ├── DashItem.java / DashAbilityHandler.java / DashProjectionSkill.java
│   ├── ObeliskItem.java / ObeliskRecallItem.java / ObeliskHandler.java
│   ├── ColonItem.java / ColonAmmo.java / ColonHandler.java
│   ├── BulletItem.java / BulletType.java                   # 六种子弹
│   ├── CrimsonFangItem.java / CrimsonFangHandler.java
│   ├── FlameCapeItem.java / FlameCapeHandler.java
│   ├── EqualSignItem.java                                  # 等号（双头斧）
│   ├── SlashItem.java                                      # 斜杠（矛）
│   └── DualWeaponHandler.java                              # 双持联动：等分 / 贯穿线 / 删除线
├── entity/
│   ├── LightOrb.java / LaserBolt.java / LaserBeam.java
│   ├── ColonBullet.java
│   ├── ObeliskSentinel.java / ProjectionPortal.java
│   ├── FlameButterfly.java
│   └── RedLine.java / SpearBeam.java / GroundCrack.java    # 红线 / 投矛光束 / 地面裂隙
├── menu/
│   └── ColonMenu.java
├── weapon/
│   ├── ModWeaponTypes.java        # 武器类型相关的代码钩子
│   └── ModAnimations.java         # 代码侧动画注册（冒号 / 等号 / 举矛）
├── network/
│   ├── ModNetworking.java
│   ├── ShakePayload.java / DashChargePayload.java
│   ├── DashActionPayload.java / ObeliskActionPayload.java / ColonActionPayload.java
│   ├── CrimsonFangPayload.java / FlameCapePayload.java
│   └── ExclamationPayload.java / DualWeaponPayload.java
└── client/
    ├── ModClientSetup.java / ModKeyMappings.java / ClientShakeHandler.java
    ├── DashClientInput.java / DashClientAnimations.java / DashChargeHud.java
    ├── ObeliskClientInput.java / ObeliskSentinelRenderer.java
    ├── ColonClientInput.java / ColonRecoilHandler.java / ColonBulletRenderer.java
    ├── AmmoWheelHandler.java / AmmoWheelScreen.java / ColonScreen.java
    ├── CrimsonFangClientInput.java / GunFirstPersonHandler.java
    ├── FlameCapeCloakRenderer.java / FlameCapeShape.java / FlameCapeClient.java
    ├── FlameButterflyRenderer.java
    ├── RedLineRenderer.java / SpearBeamRenderer.java / GroundCrackRenderer.java
    └── LaserBoltRenderer.java / LaserBeamRenderer.java / ProjectionPortalRenderer.java
```

---

## 许可证

本项目以 **GNU General Public License v3.0（GPL-3.0）** 发布，与史诗战斗的代码许可保持一致。
