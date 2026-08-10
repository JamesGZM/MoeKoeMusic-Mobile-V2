# 应用语言与 Settings 准入审计

状态：Deferred。审计日期：2026-08-10。

## 决策

Settings 的“语言”保持不可提交，不能先新增偏好、DataStore key、Dialog 或 Locale API 调用。PC 的多语言产品概念不能弥补 Android V2 没有任何第二语言资源、系统 per-app locale 声明或跨 API 生命周期方案的事实。

`09-settings.png` 中的尾值“简体中文”是未来已确认视觉的目标值，不是当前运行时能力。当前实现使用 `SettingsRowUi.Unavailable(SettingsRowId.Language)`（`feature/settings/src/main/kotlin/cn/james/music/feature/settings/SettingsUiModels.kt:302-308`），`SettingsContent` 因而显示“尚未开放”（`SettingsContent.kt:115-121`、`res/values/strings.xml:44`）并禁用点击；这是诚实状态，本切片不改 UI。

## 固定产品与资源事实

| 来源 | 固定事实 | 采用 / 拒绝 |
| --- | --- | --- |
| PC `MoeKoeMusic@52c9833afe2e7fedcba8d5b23ff8d1f9731af73a`，GPL-2.0-only | `src/config/settings.js:9-23` 定义 `language` 及 `zh-CN/zh-TW/en/ru/ja/ko` 六项；`src/utils/i18n.js:1-35` 加载六份 JSON、优先持久化选择，否则跟随浏览器，并以 `zh-CN` fallback；`src/views/Settings.vue:445-451,569-578` 立即应用 locale、更新 `documentElement.lang`、写回 localStorage。 | 只迁移“跟随系统/显式选择”的产品概念；拒绝 Vue、浏览器 locale、localStorage、Electron IPC 与 GPL 实现。 |
| 同一 PC 的资源统计 | 基于六份 `src/language/*.json` 的递归标量 key 集合：`zh-CN/en/ru` 各 322，均与 `zh-CN` 相同；`ja/ko/zh-TW` 各 302，均相对 `zh-CN` 缺 20，且没有额外 key。 | PC 不能证明六语种均完成翻译；目标语言与完整度需由 Android 单独验收。 |
| 旧 Mobile `MoeKoeMusic-Mobile@ab71195d4cf3297332490fd37704d1ae8973d4c5`，GPL-2.0-only | `src/features/settings/store.ts:7-94` 的状态与持久化动作只含 `themeMode/accentId`；`src/features/settings/storage.ts:3-29` 也只存这两项。 | Mobile 没有可迁移的语言偏好或本地化消费者。 |
| Android V2 资源 | 资源目录清单只有 11 份未限定 locale 的 `src/main/res/values/strings.xml`，共 448 个 string/plurals/array 项；唯一 `values-night` 是夜间主题，不是 `values-xx`。 | 当前只有中文基础资源，不能声明支持第二语言。 |
| Android V2 文案 | 生产 Kotlin 有 14 个文件命中中文；用户可见例子包括 `app/.../AudioImportViewModel.kt:31,72-83,111-148`、`AppPlaybackViewModel.kt:142-161`、`AudioImportActivity.kt:63-69`、`data/.../LocalImportWorker.kt:62,72,262,375-382`、`RiskCaptchaActivity.kt:208`。 | 必须先资源化 App、导入 Worker、播放错误和验证加载文案；歌曲/歌词等用户内容不翻译。 |

## Android 平台与架构阻塞

- `app/src/main/AndroidManifest.xml:11-72` 没有 `android:localeConfig`；工程没有 `LocaleConfig`、`LocaleManager`、`LocaleManagerCompat` 或 `AppCompatDelegate` 消费。Android 13+ 的 [per-app language guidance](https://developer.android.com/guide/topics/resources/app-languages) 要求自动生成或显式 `LocaleConfig`，以把真实支持的语言公开给系统设置；[LocaleConfig API](https://developer.android.com/reference/android/app/LocaleConfig) 明确该声明是系统支持语言的来源。
- `MainActivity`、`AudioImportActivity`、`RiskCaptchaActivity` 分别是 `ComponentActivity`（`app/src/main/kotlin/cn/james/music/MainActivity.kt:28`、`AudioImportActivity.kt:33`、`RiskCaptchaActivity.kt:40`），且 `app/build.gradle.kts:50-78` 没有 AppCompat 依赖。官方指南说明：Compose 在 API 32 及以下采用 `AppCompatDelegate.setApplicationLocales` 时，host 必须是 `AppCompatActivity`；设置 locale 会触发 configuration change / Activity recreate。
- 冻结语义：**跟随系统**等于没有 app override（空 application locale list），不是把当前系统语言持久化；**应用内选择**是 app override。API 33+ 系统设置与应用内选择必须共享 platform locale 的单一事实来源，不能由 DataStore 和 `LocaleManager` 分别裁决。API 26–32 的兼容存储、迁移和 Activity 生命周期须在 Accepted 方案中统一决定。
- AntennaPod `d39bf05`，GPL-3.0：`app/src/main/AndroidManifest.xml:30-45` 以 `android:localeConfig` 注册系统可选语言，`app/src/main/res/xml/locale_config.xml:1-42` 只枚举其真实资源语言；其 `ui/i18n/src/main/res/values-*` 有对应资源目录。采用“先有完整资源再暴露系统语言列表”的顺序；拒绝其源码、偏好/UI 架构与 GPL 代码。

## 五项准入条件

1. 用户确认第一批目标 BCP-47 语言清单、fallback 与翻译质量责任；不能从 PC 六项自动推断 Android 范围。
2. 11 个资源模块为每个目标 locale 完整提供 strings/plurals/format，清除所有用户可见 Kotlin 硬编码，建立缺键、占位符和 plural 检查。
3. 冻结 API 26–36 的单一 locale 所有权：API 33+ 系统 `LocaleManager` 与应用选择同步；API 26–32 的 AppCompat/Activity 基类迁移或其他兼容策略必须先获架构批准，禁止手写 `Configuration` 包装。
4. 定义 Main、导入、风险验证、后台播放/通知、Worker、日期/数字、深链和进程重建的 locale 生命周期；网络协议、下载请求和远端歌曲/歌词内容不因 UI locale 擅自改变。
5. 获得 Settings 选择 Dialog、英文/目标语言、长文本与 RTL 状态确认，并完成翻译 QA、无障碍和截图基线设计；在此之前保留 `Language` Unavailable。

## 后续原子顺序与测试矩阵

1. 资源与非资源化文案审计、目标语言翻译和键集门禁；先证明每个目标 locale 可完整渲染。
2. 仅在第 1 步完成后，进行 locale 所有权、AppCompat/系统 API、`LocaleConfig` 与全部 Activity/Service 生命周期的架构准入；不先做 Settings UI。
3. 最后接入 DataStore 的迁移辅助（若 Accepted）、Settings 选择 UI、系统设置同步和视觉/无障碍状态。

最低自动化矩阵：每 locale 的资源 key/plural/格式化占位符；“跟随系统”、应用 override、系统设置回写、未知/移除 locale fallback；API 26/32/33/36 的 Activity recreate、Main/导入/风险入口与进程恢复；Worker/通知/MediaSession 文案；LTR/RTL、字体缩放、TalkBack、截图和翻译 QA。指定真机还须验证系统设置入口、后台通知、Bluetooth/锁屏媒体控制和语言切换后恢复；本审计未运行这些项目。

## 非目标与未验证项

- 本切片不新增资源、依赖、权限、网络 Header、LocaleConfig、DataStore、公共接口或 UI。
- 不把“默认简体中文”误写为“已支持跟随系统”；当前基础资源只会以中文显示。
- 不复制 PC/Mobile/AntennaPod 的 GPL 代码。设备上的系统 locale、通知和后台生命周期只能由用户指定真机验证。
