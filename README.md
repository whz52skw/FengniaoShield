# FnShield（蜂鸟盾）— 蜂鸟骑士违规抑制 + 定位增强模块

> 面向饿了么蜂鸟骑士（me.ele.hbdteam，10.11.1）的 Xposed 模块。
> 品牌名 FnShield，包名 com.fnshield。本仓库 FengniaoShield。

**核心作用**：超速 / 逆行 / 闯红灯 / 分心驾驶 / 未戴头盔 / 路口超速 判定屏蔽，违规语音播报静音，附带定位增强（GPS 可信 + 无视距离）。两个版本、全量日志、BLA 反逆向加固。

---

## ⚠️ 免责声明（请先读）

1. 本项目仅供学习研究与技术交流，请勿用于商业用途。
2. 使用本项目产生的任何后果（含但不限于账号风险、平台规则处罚）由使用者自行承担。
3. 平台规则随时变化，功能存在失效可能，本项目不提供永久可用保证。
4. 不同意以上任一条，请勿使用。

---

## 两个版本怎么选

| 版本 | 要不要 Root / LSPosed | 假定位 | 下载 |
|---|---|---|---|
| LSPosed 版 | 要（手机已装 LSPosed 框架） | 无（定位走官方） | Releases 的 FnShield_lsp.apk |
| 整合版（免 root） | 不要，装一个 APK 就能用 | 有（假定位 + 路线模拟） | Releases 的 蜂鸟骑士_10.11.1_FnShield整合版.apk |

---

## 功能明细

### 两版都有
- 超速 / 路口超速：判定链 + 上报接口拦截
- 逆行：判定拦截
- 闯红灯：判定 + 上报点拦截（点杀，不伤地图蓝点）
- 分心驾驶（骑行用手机）：判定 / 上报拦截
- 未戴头盔：采集 / 识别 / 上传全链拦截
- 违规语音播报静音
- GPS 可信：假定位不被识破
- 遵守交规：默认开启、无开关（行为数据与判定对得上，不露馅）
- 无视距离：有开关，开启前弹风险提示（终点/停车/拉回商圈距离校验放行，抽查可能算违规）
- 高德签名伪造：修整合版重打包后的地图/定位异常
- 全量日志：hook 命中 / 签名 / 测点 / 开关 全程落盘

### 仅整合版
- 假定位：悬浮球设假坐标
- 路线模拟：按预设路线推进坐标（配遵守交规）
- 悬浮球：假定位 / 路线模拟 / 无视距离 开关

---

## 安装（一步步）

### LSPosed 版
1. Releases 下载 FnShield_lsp.apk，传到手机安装
2. 打开 LSPosed，模块里勾选 FnShield
3. 作用域勾选「蜂鸟骑士」（me.ele.hbdteam）
4. 重启蜂鸟骑士，正常跑单
5. 打开 FnShield 设置页，可按需开/关「无视距离」（开启会弹风险提示）

### 整合版（免 root）
1. Releases 下载 蜂鸟骑士_10.11.1_FnShield整合版.apk（约 152MB）
2. 先卸载手机上的官方蜂鸟骑士
3. 直接安装整合版，打开即有悬浮球
4. 悬浮球里控制：假定位 / 路线模拟 / 无视距离

---

## 日志（全量、自动）

目录：/sdcard/Android/data/me.ele.hbdteam/files/fnshield_logs/
每 60 秒一次自检心跳 + hook 命中 + 签名替换 + 假坐标 + 探针 + 开关，全程落盘，出问题直接拷日志文件上报。

---

## 防护

- BLA 反 AI 逆向加固：dex 字符串致密化 + 诱饵陷阱（REA 暴露分 81→7，逆向 token 放大 231 倍）
- 去原作者痕迹：品牌/包名/日志/密钥全部为 FnShield 自有
- R8 裁剪优化

---

## 常见问题

Q：整合版装不上？
A：先卸载官方版（签名不同，不能覆盖安装）。

Q：地图小蓝点 / 地名消失了？
A：已修复（历史 bug，根因是 mj0.a / jj0.c 全类拦截地图回调，已改点杀）。

Q：无视距离开了会被判吗？
A：距离校验被放行后，若被平台抽查判定，属于异常操作，有违规风险（开关处已弹提示）。

---

## 项目结构

FNHookModule/app/src/main/kotlin/com/fnshield/
  MainHook.kt           入口，各层挂载
  SafetyHooks.kt        违规判定屏蔽（点杀版）
  ShieldFake.kt         假定位 + 路线模拟 + 遵守交规（整合版）
  ShieldFloat.kt        悬浮球（整合版）
  ShieldProbe.kt        高德 key 注入
  SignatureBypass.kt    签名伪造（系统层 + 高德层）
  GpsTrustHooks.kt      GPS 可信
  IgnoreDistanceHooks.kt 无视距离
  PrefsHolder.kt        跨进程配置
  FnLogger.kt           全量日志

构建：AGP 8.5.2 / Gradle 8.7 / JDK17 / SDK34。LSP 版 assembleRelease；整合版内嵌模块 assembleRelease -PfncMode=nation，再用 NPatch（Embed，sigBypass L3，hidelibs）嵌入官方 10.11.1。
