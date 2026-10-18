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
├── BTMI.java                        # 主类：物品 / 实体 / 数据组件 / 菜单 / 创造模式物品栏注册
├── BTMIClient.java                  # 客户端入口（配置界面）
├── Config.java                      # 配置定义
├── item/
│   ├── ExclamationMarkItem.java     # 感叹号：属性与蓄力逻辑
│   ├── SmashHandler.java            # 感叹号：服务端重击
│   ├── DashItem.java                # 破折号：属性、右键光球
│   ├── DashAbilityHandler.java      # 破折号：分离状态、蓄力劈砍、输入抑制
│   ├── ObeliskItem.java             # 示亡号：属性、召唤悬浮实体
│   ├── ObeliskRecallItem.java       # 召回示亡号：激光、召回
│   ├── ObeliskHandler.java          # 示亡号：召唤/召回/光剑/激光/引爆
│   ├── ColonItem.java               # 冒号：属性、右键打开弹巢
│   ├── ColonAmmo.java               # 冒号：六个弹巢槽位的存取
│   ├── ColonHandler.java            # 冒号：发射与选弹
│   ├── BulletItem.java              # 六种子弹的共用物品
│   └── BulletType.java              # 弹药类型与物品的对应关系
├── entity/
│   ├── LightOrb.java                # 光球投射物
│   ├── LaserBolt.java               # 激光弹投射物
│   ├── ColonBullet.java             # 冒号的子弹投射物（六种弹道效果）
│   └── ObeliskSentinel.java         # 示亡号的悬浮实体
├── menu/
│   └── ColonMenu.java               # 冒号的六格圆盘弹巢界面
├── weapon/
│   └── ModWeaponTypes.java          # 冒号的自定义武器类型（单手持握 + 自定义待机动画）
├── network/
│   ├── ModNetworking.java           # 网络包注册
│   ├── ShakePayload.java            # 屏幕震动（服务端 → 客户端）
│   ├── DashActionPayload.java       # 破折号动作（客户端 → 服务端）
│   ├── ObeliskActionPayload.java    # 示亡号动作（客户端 → 服务端）
│   └── ColonActionPayload.java      # 冒号发射 / 选弹（客户端 → 服务端）
└── client/
    ├── ClientShakeHandler.java      # 摄像机震动
    ├── ModClientSetup.java          # 实体渲染器 + 物品模型属性 + 菜单界面注册
    ├── ModKeyMappings.java          # 按键绑定（引爆键 V、选弹轮盘键 B）
    ├── DashClientInput.java         # 破折号：左键 / 蓄力输入追踪
    ├── ObeliskClientInput.java      # 示亡号：左键 / 引爆键输入追踪
    ├── ColonClientInput.java        # 冒号：左键发射输入
    ├── ColonRecoilHandler.java      # 冒号：开火时的镜头后坐力
    ├── AmmoWheelHandler.java        # 冒号：长按唤出选弹轮盘
    ├── AmmoWheelScreen.java         # 冒号：选弹轮盘界面（不暂停、释放鼠标）
    ├── ColonScreen.java             # 冒号：圆盘弹巢界面渲染
    ├── ObeliskSentinelRenderer.java # 示亡号悬浮实体的姿态渲染
    └── LaserBoltRenderer.java       # 激光弹沿飞行方向的渲染
```

---

## 许可证

本项目以 **GNU General Public License v3.0（GPL-3.0）** 发布，与史诗战斗的代码许可保持一致。
