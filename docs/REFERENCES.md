# 参考项目与来源

技术决策点级审计：

- [应用导航架构](reference-audits/06-navigation-architecture.md)
- [酷狗 HTTP 客户端选型](reference-audits/07-kugou-http-client.md)
- [登录、会话与安全验证](reference-audits/09-login-session-and-risk.md)
- [用户资料与“我的”会话态](reference-audits/10-user-profile-and-my-session.md)
- [每日 VIP 领取协议与数据准入](reference-audits/30-daily-vip-claim.md)
- [独立用户主页纯 UI 与 Feature 所有权](reference-audits/20-user-profile-ui.md)
- [搜索结果页视觉与状态边界](reference-audits/21-search-results-ui.md)
- [酷狗同步歌词协议、解析与缓存](reference-audits/12-kugou-lyrics.md)
- [播放器封面动态调色](reference-audits/13-player-artwork-palette.md)
- [应用品牌与系统启动页](reference-audits/14-app-brand-and-splash.md)
- [首页真实内容与缓存](reference-audits/15-home-content-and-cache.md)
- [UI 设计交付、屏幕适配与视觉验收](reference-audits/16-ui-design-handoff-and-adaptation.md)
- [设置与应用偏好](reference-audits/18-settings-and-preferences.md)
- [品牌主题色与全局配色](reference-audits/25-brand-theme-color.md)
- [缓存管理与清理范围](reference-audits/26-cache-management.md)
- [歌词高亮方式与设置准入](reference-audits/27-lyrics-highlight-mode.md)
- [播放淡入淡出准入](reference-audits/28-playback-fade.md)
- [应用语言与 Settings 准入](reference-audits/29-app-language.md)
- [歌单详情纯 UI 与 Feature 所有权](reference-audits/19-playlist-detail-ui.md)
- [Agent Skills 与可执行开发门禁](reference-audits/22-agent-skills-development-system.md)
- [数据驱动公共 UI 架构与迁移准入](reference-audits/23-data-driven-ui-architecture.md)

记录日期：2026-08-09。Stars、活跃度和 Release 会变化，本文重点记录参考价值，不将快照数据作为永久事实。

## MoeKoeMusic 系列

### MoeKoeMusic PC

- 本地目录：`../MoeKoeMusic`
- 仓库：https://github.com/MoeKoeMusic/MoeKoeMusic
- 技术：Vue 3、Pinia、Electron。
- 主要参考：完整功能范围、音质回退、KRC 逐字歌词、播放队列、账号和云盘。
- 不直接复用：Vue/Electron UI、Web Audio 生命周期和本地 Node 服务结构。

关键文件：

- `src/components/player/songQueue/OnlineMusicQueue.js`
- `src/components/player/LyricsHandler.js`
- `src/components/player/AudioController.js`
- `src/utils/request.js`

### MoeKoeMusic Mobile

- 本地目录：`../MoeKoeMusic-Mobile`
- 仓库：https://github.com/MoeKoeMusic/MoeKoeMusic-Mobile
- 技术：Expo、React Native、TypeScript。
- 主要参考：移动信息架构、设备注册、会话恢复、API 调用顺序和响应兼容。
- 不直接复用：Hermes/CommonJS、Buffer polyfill、Expo Audio 和弱类型动态解析。

关键文件：

- `src/lib/kugou-api/bootstrap.ts`
- `src/lib/kugou-api/device.ts`
- `src/lib/kugou-api/session.ts`
- `src/lib/kugou-api/use-axios.ts`
- `src/features/player/store.ts`
- `src/features/player/song-url.ts`

### KuGouMusicApi

- 仓库：https://github.com/MakcRe/KuGouMusicApi
- 固定基准：`6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb`
- 许可证：MIT。
- 主要参考：接口 Host、参数、Header、签名、加密、Cookie 和错误处理。
- 使用方式：作为协议参考和测试基准，不打包 Node 服务进 Android。

重点文件：

- `util/request.js`
- `util/helper.js`
- `util/crypto.js`
- `util/util.js`
- `module/register_dev.js`
- `module/song_url.js`
- `module/login_cellphone.js`

## Android 音乐项目

### Metrolist

- 仓库：https://github.com/MetrolistGroup/Metrolist
- 特点：活跃的 Material 3 在线音乐客户端，功能覆盖广。
- 借鉴：Media3、高级队列、歌词、动态主题、缓存、Android Auto 和复杂音乐库交互。
- 不照搬：单一大型 `app` 模块和职责过多的 `MusicService`。

### Kreate

- 仓库：https://github.com/knighthat/Kreate
- 特点：RiMusic 后继项目，Kotlin Multiplatform，强调性能和多平台。
- 借鉴：大尺寸封面、封面驱动背景、封面与歌词双模式、沉浸式歌词氛围、播放器性能处理和多种主题实现。
- 不照搬：Kreate Logo、系统栏表现、两层播放器底栏、过密操作按钮、RiMusic 历史结构、过多扩展和当前项目不需要的 KMP 复杂度。

### OuterTune

- 仓库：https://github.com/OuterTune/OuterTune
- 特点：Material 3、本地与在线音乐混合、歌词和高级音频能力。
- 状态：README 已声明不再积极开发。
- 用途：仅作为功能和历史实现参考，不作为长期架构基线。

### RiMusic

- 仓库：https://github.com/fast4x/RiMusic
- 状态：已归档。
- 用途：理解 Kreate 的历史来源，不作为新项目基线。

### Symphony

- 仓库：https://github.com/zyrouge/symphony
- 特点：轻量本地音乐播放器。
- 借鉴：本地媒体库、简洁播放器和轻量信息结构。
- 注意：默认分支活跃度低于仓库 `pushed_at` 表面显示，需以实际分支提交判断。

## 官方架构与媒体参考

- [Now in Android](https://github.com/android/nowinandroid) `7d45eae4f8720a0c77f507712ba2437ff974b6ed`：Feature 模块化、应用组合根、App State、导航所有权、Route/Screen/ViewModel 和最小可见性参考；本项目不机械复制 Navigation 3 或每个 Feature 的 `api/impl` 双模块结构。详见 [`reference-audits/08-feature-modularization.md`](reference-audits/08-feature-modularization.md)。

### 阶段 1 构建与 UI 基础

- AGP 9.3 版本说明与兼容矩阵：https://developer.android.com/build/releases/agp-9-3-0-release-notes
- Compose Preview Screenshot Testing：https://developer.android.com/studio/preview/compose-screenshot-testing
- Dagger / Hilt Releases：https://github.com/google/dagger/releases
- Material 3 Compose：https://developer.android.com/develop/ui/compose/designsystems/material3

采用点：AGP/Gradle/JDK 组合、官方截图测试、Hilt Android 组合根与 Material 3 主题体系。Hilt 2.59 开始明确支持 AGP 9，因此工程使用 2.59.2，而不是计划中无法加载 AGP 9 扩展的 2.57.1。Compose Screenshot Testing 仍为实验插件，仅用于视觉回归。

主要依赖许可证：AndroidX、Compose、Room、DataStore、Media3、Hilt、Ktor、OkHttp 与 Coil 为 Apache-2.0；Kotlin 与 Coroutines 为 Apache-2.0；Spotless 为 Apache-2.0。它们分别承担平台 UI/存储/媒体能力、依赖注入、语言与并发、格式检查、网络传输和图片加载，未引入遥测或远程托管服务。Coil 3 的 Compose 与 OkHttp 网络 artifact 使用同一固定版本；网络模块只在 `:app` 组合根装配，使各 Feature 的 `AsyncImage` 能消费已有 HTTPS 图片地址。

### Now in Android

- 仓库：https://github.com/android/nowinandroid
- 架构说明：https://github.com/android/nowinandroid/blob/main/docs/ArchitectureLearningJourney.md
- 借鉴：UDF、Flow、Repository、测试替身、模块依赖和截图测试。
- 不照搬：面向大型团队的全部模块数量，以及不适用于流媒体 App 的强制完整 offline-first。

### Compose Samples / Jetcaster

- 仓库：https://github.com/android/compose-samples
- 借鉴：封面动态主题、播放器 UI、WindowInsets、Supporting Pane、自适应布局和预测返回。
- 注意：Jetcaster 是媒体 UI 和状态模式参考，不是完整后台音乐播放器架构。

### AndroidX Media3

- 仓库：https://github.com/androidx/media
- 文档：https://developer.android.com/media/media3
- 借鉴：ExoPlayer、MediaSession、MediaLibraryService、MediaController、通知栏和系统媒体集成。
- 规则：播放器生命周期和系统行为优先遵循 Media3 官方文档，而非第三方项目的兼容写法。

### 阶段 2 后台播放与恢复

- 后台播放与 `MediaLibraryService`：https://developer.android.com/media/media3/session/background-playback
- Playback resumption：https://developer.android.com/media/media3/session/background-playback#playback-resumption
- Android 13 通知权限与媒体会话豁免：https://developer.android.com/develop/ui/views/notifications/notification-permission#exemptions

采用点：Service 独占 ExoPlayer、Media3 管理 MediaStyle 通知、仅播放期间维持前台服务，并通过 `onPlaybackResumption` 响应系统媒体入口。App 不申请 `POST_NOTIFICATIONS`，但不会把豁免扩展到普通通知。

阶段 2 演示音频不来自外部作品：仓库中的 `PlaybackSourceResolver.kt` 通过 `DemoAudioFile` 使用固定音符序列在 App 专属目录生成 12 秒 PCM WAV。这样既能离线重复测试，也不提交受版权约束的音频二进制；三个演示条目只复用这一个自制来源。

## Android 本地文件与系统集成

- Storage Access Framework：https://developer.android.com/training/data-storage/shared/documents-files
- MediaStore 与音频权限：https://developer.android.com/training/data-storage/shared/media
- Intent 与 Intent Filter：https://developer.android.com/guide/components/intents-filters
- 接收外部分享：https://developer.android.com/training/sharing/receive
- Media3 支持格式：https://developer.android.com/media/media3/exoplayer/supported-formats
- WorkManager 版本与发布说明：https://developer.android.com/jetpack/androidx/releases/work
- 长时间运行 Worker：https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running
- AndroidX Hilt 版本：https://developer.android.com/jetpack/androidx/releases/hilt
- Coil：https://github.com/coil-kt/coil

采用点：使用窄范围内容 URI 授权、按 Android 版本申请音频读取权限、使用 `audio/*` 外部入口并由 Media3 验证可播放格式。不采用 `MANAGE_EXTERNAL_STORAGE`，不把临时外部 URI 当作长期音乐库来源。

固定版本、具体源码文件及采用/拒绝结论见 [`reference-audits/02-playback-core.md`](reference-audits/02-playback-core.md) 与 [`reference-audits/03-local-music.md`](reference-audits/03-local-music.md)。新增重大功能必须按 [`templates/FEATURE_SPEC_TEMPLATE.md`](templates/FEATURE_SPEC_TEMPLATE.md) 完成同等审计。

阶段 4 的协议、移动初始化与 PC 产品语义审计见 [`reference-audits/04-kugou-online-slice.md`](reference-audits/04-kugou-online-slice.md)。该审计固定了 PC `52c9833`、Mobile `ab71195` 和 KuGouMusicApi `6efe84e` 的具体文件；仅迁移目标 Endpoint 所需的 MIT 协议实现，不复制整个配置或无关第三方凭据。

首页、登录、“我的”、图形验证码和图标策略见 [`reference-audits/05-home-account-ui.md`](reference-audits/05-home-account-ui.md)。该审计固定了 PC 与 Mobile 的登录、风险验证和用户资产源码路径；标准语义优先使用 Apache-2.0 的 Compose Material Icons，验证码 WebView 仅作为腾讯官方 H5 验证的隔离例外。

登录协议、会话原子提交、二维码和隔离安全验证的决策点审计见 [`reference-audits/09-login-session-and-risk.md`](reference-audits/09-login-session-and-risk.md)。审计固定 PC `52c9833`、Mobile `ab71195` 和 KuGouMusicApi `6efe84e` 的请求与消费文件，并采用 ZXing Core 3.5.4 生成二维码、AndroidX WebKit 1.16.0 提供 origin 受限的兼容 WebView API；二者均为 Apache-2.0。

登录后的用户资料、VIP 摘要、刷新、部分失败、会话失效提示和退出边界见 [`reference-audits/10-user-profile-and-my-session.md`](reference-audits/10-user-profile-and-my-session.md)。审计固定 KuGouMusicApi `6efe84e`、PC `52c9833`、Mobile `ab71195` 及其 API submodule `283f1e97`，结论是复用现有 Ktor、RSA、Coil 与会话存储，不新增依赖；资料是主结果，VIP 是可降级附加结果，未接入的资产不得用假数据填充设计稿。

全屏播放器的 Compose Pager、返回、系统栏、无障碍、模块边界和首个封面页切片见 [`reference-audits/11-fullscreen-player-ui.md`](reference-audits/11-fullscreen-player-ui.md)。审计固定 Kreate `f02577e`、Metrolist `289ed45`、PC `52c9833` 与 Mobile `ab71195` 的具体播放器文件；只采用状态与性能策略，不复制 GPL-3.0 实现，也不在歌词协议审计前接入伪歌词数据。

酷狗同步歌词的 `/search`、`/download`、KRC 解包、逐字/翻译/音译解析、成功缓存与 UI 状态门禁见 [`reference-audits/12-kugou-lyrics.md`](reference-audits/12-kugou-lyrics.md)。解析采用 Maven Central `lyrics-core 0.4.7`（Apache-2.0，固定源码 `d1bea0b`），不自行重写完整 KRC parser；协议解包继续使用 JDK Base64/zlib，且公共歌词 Host 不发送设备身份、Cookie 或签名。

播放器封面动态调色的依赖、线程、颜色修正、失败回退和切歌隔离见 [`reference-audits/13-player-artwork-palette.md`](reference-audits/13-player-artwork-palette.md)。后续实现固定 AndroidX Palette `1.0.0`（Apache-2.0），只消费 Coil 已解码图片，不新增封面请求；审计固定 Kreate `f02577e` 与 Metrolist `289ed45` 的具体播放器文件，只借鉴状态和降级策略，不复制 GPL-3.0 代码。当前切片只提交方案说明，尚未接入代码。

应用正式名称、PC/Mobile 同源女孩耳机 Logo、Adaptive/round/monochrome 图标和系统 SplashScreen 边界见 [`reference-audits/14-app-brand-and-splash.md`](reference-audits/14-app-brand-and-splash.md)。系统启动基建固定 AndroidX Core SplashScreen `1.2.0`（Apache-2.0），不增加 Splash Activity、人工延时、网络等待或条件启动门禁；数据状态由所属页面承接。

首页真实内容的三个首批 Endpoint、字段容错、Room 完整快照、身份分区、15 分钟刷新门槛、部分失败恢复和自动测试矩阵见 [`reference-audits/15-home-content-and-cache.md`](reference-audits/15-home-content-and-cache.md)。协议固定 KuGouMusicApi `6efe84e`、Mobile API submodule `283f1e97` 及 PC/Mobile 消费层；排行榜与新歌继续归发现页，不复制 Mobile 的首页重复聚合。2026-08-06 的自动真实兼容测试确认每日推荐与推荐歌单可用；轮播固定请求和最新上游实现均返回类型化 `31136`，恢复前只作为可选区块。

确认设计稿到 Android 当前窗口的统一比例映射、Insets/IME、大字体、宽屏保守派生，以及“设计符合度先于截图回归”的门禁见 [`reference-audits/16-ui-design-handoff-and-adaptation.md`](reference-audits/16-ui-design-handoff-and-adaptation.md)。审计固定 Now in Android `7d45eae4` 与 Compose Samples `84788c81` 的窗口适配和多尺寸测试文件；二者均为 Apache-2.0，只采用基于当前窗口和独立尺寸矩阵的原则，不复制页面布局，也不新增依赖。

全部确认页面与组件板的公共组件准入、`:core:designsystem` 包职责、Toolbar 等组合组件的真实复用门槛，以及登录页面/公共组件所有权见 [`reference-audits/17-design-system-components.md`](reference-audits/17-design-system-components.md)。审计采用 Android 官方 Custom Design System、App Bar 与 Dialog 指南，并固定 Now in Android `7d45eae4` 和 Compose Samples `84788c81` 的组件目录作为 Apache-2.0 架构参考；不复制实现，也不新增依赖。

设置与应用偏好的 DataStore 所有权、主题即时/持久生效、独立 Feature 导航、失败恢复和确认稿渐进交付边界见 [`reference-audits/18-settings-and-preferences.md`](reference-audits/18-settings-and-preferences.md)。审计固定 Now in Android `7d45eae4` 的 DataStore/Settings 链路与 Metrolist `289ed45` 的音乐设置分组源码；后者为 GPL-3.0，只学习状态和布局策略，不复制代码。首批不新增依赖，只实现有真实消费者的主题与关于分组。

品牌主题色的六档来源、Android 默认蓝优先、Material color roles、DataStore 边界和拒绝系统 Monet 的理由见 [`reference-audits/25-brand-theme-color.md`](reference-audits/25-brand-theme-color.md)。本次实际核对 PC `52c9833` 的 `src/config/settings.js` / `src/utils/utils.js`、旧 Mobile `ab71195` 的 `src/constants/accents.ts` / `src/hooks/use-palette.ts` / `src/features/settings/store.ts` / `src/app/settings.tsx`、Now in Android `7d45eae4` 的 `core/datastore/src/main/kotlin/com/google/samples/apps/nowinandroid/core/datastore/NiaPreferencesDataSource.kt`，以及 Android 官方 Material 3、Custom Design System 与 Preferences DataStore 文档；仅采用状态、所有权和语义角色原则，不复制 GPL 代码。

缓存清理的白名单、Room/Coil 失效边界和 CacheLimit 拆分见 [`reference-audits/26-cache-management.md`](reference-audits/26-cache-management.md)。本次实际核对 PC `52c9833` 的 Settings/config、旧 Mobile `ab71195` 的 Settings storage、Coil `51638b0` 的 `SingletonImageLoader` / `MemoryCache` / `DiskCache`（Apache-2.0）、AntennaPod `d39bf05` 的 episode cleanup strategy（GPL-3.0-only，仅参考“容量策略独立”）以及 Android 官方 app-specific storage / Room asynchronous queries 文档；不复制 GPL 代码，不调用全设备清缓存 Intent。

歌词显示的逐字/逐行高亮语义、PC/Mobile 差异与 Android 设计阻塞见 [`reference-audits/27-lyrics-highlight-mode.md`](reference-audits/27-lyrics-highlight-mode.md)。本次实际核对 PC `52c9833` 的 `src/components/FullscreenLyricsSettings.vue`、`src/components/PlayerControl.vue`、`src/assets/style/PlayerControl.scss`，以及旧 Mobile `ab71195` 的 `src/components/ui/lyrics-view.tsx`、`src/features/player/lyrics.ts`、`types.ts` 和 Settings store/storage；只采用产品语义和状态边界，不复制 GPL 代码。

播放“淡入淡出”的 A 曲目开始/结束淡化、B 暂停/恢复音量 ramp、C 相邻曲目 crossfade 边界见 [`reference-audits/28-playback-fade.md`](reference-audits/28-playback-fade.md)。本次实际核对 PC `52c9833` 的 `src/assets/style/PlayerControl.scss` / `src/components/PlayerControl.vue` 和旧 Mobile `ab71195` 的 `src/components/ui/mini-player.tsx` / `src/app/settings.tsx`，确认只有视觉动画而无音频消费者；并核对 AndroidX Media3 的 [ExoPlayer thread 约束](https://developer.android.com/reference/androidx/media3/exoplayer/ExoPlayer) 与 [Player transition/event](https://developer.android.com/media/media3/exoplayer/listening-to-player-events) 文档。Kreate `f02577e` 的 `PlayerSettings.kt` / `StatefulPlayerImpl.kt`（GPL-3.0）仅作为单 Player 暂停/恢复 ramp 风险参考，Metrolist `289ed45` 的 `PlayerSettings.kt` / `MusicService.kt`（GPL-3.0）仅作为双 Player crossfade、offload 和 Session 交接风险参考；不复制 GPL 代码。当前结论为 Deferred，不新增依赖或实现。

应用语言的 PC/Mobile 事实、Android 资源缺口与 Settings 准入见 [`reference-audits/29-app-language.md`](reference-audits/29-app-language.md)。本次实际核对 PC `52c9833` 的 `src/config/settings.js` / `src/utils/i18n.js` / `src/views/Settings.vue` / 六份 `src/language/*.json`，以及旧 Mobile `ab71195` 的 `src/features/settings/store.ts` / `storage.ts`；并采用 Android 官方 [per-app language guidance](https://developer.android.com/guide/topics/resources/app-languages) 与 [LocaleConfig](https://developer.android.com/reference/android/app/LocaleConfig) 的系统 locale 单一事实来源约束。AntennaPod `d39bf05` 的 `app/src/main/AndroidManifest.xml` / `app/src/main/res/xml/locale_config.xml`（GPL-3.0）仅作为“真实资源先于系统语言列表”的固定参考；不复制 GPL 代码。当前结论为 Deferred，不新增资源、语言 API 或 DataStore。

歌单详情确认稿的纯 UI 范围、独立 `:feature:playlist` 所有权、类型安全子页面导航、应用壳 MiniPlayer 边界和事件端口见 [`reference-audits/19-playlist-detail-ui.md`](reference-audits/19-playlist-detail-ui.md)。审计复用 Now in Android `7d45eae4` 与 Compose Samples `84788c81` 的 Apache-2.0 模块和 UI 分层原则，不复制页面实现、不新增依赖，也不把设计示例数据写入业务层。

阶段 4 的真实服务补审还固定了 SPlayer-Next `75b4301c`、UnblockNeteaseMusic/server `39e21bfb` 和 kugou-music-api Go `950cbf0b`。前两者仅用于验证匿名搜索 Endpoint 的公开实践，因 AGPL-3.0/LGPL-3.0 不复制代码；Go 项目为 MIT，用来交叉复现 `/v3/search/song` 当前返回 `152` 的行为。最终匿名路径由本项目以最小 HTTPS 请求独立实现并通过真实服务测试。

## 许可证注意

- MoeKoeMusic PC 与 Mobile 使用 GPL-2.0。
- Metrolist、Kreate、OuterTune、RiMusic 等多为 GPL-3.0。
- Now in Android、Compose Samples 和 AndroidX Media 使用 Apache-2.0。
- KuGouMusicApi 使用 MIT。
- 参考思想不等同于复制代码；复制或迁移具体实现前必须确认对应许可证义务。
