# 参考项目与来源

技术决策点级审计：

- [应用导航架构](reference-audits/06-navigation-architecture.md)
- [酷狗 HTTP 客户端选型](reference-audits/07-kugou-http-client.md)

记录日期：2026-08-05。Stars、活跃度和 Release 会变化，本文重点记录参考价值，不将快照数据作为永久事实。

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

主要依赖许可证：AndroidX、Compose、Room、DataStore、Media3、Hilt、Ktor 与 OkHttp 为 Apache-2.0；Kotlin 与 Coroutines 为 Apache-2.0；Spotless 为 Apache-2.0。它们分别承担平台 UI/存储/媒体能力、依赖注入、语言与并发、格式检查和网络传输，未引入遥测或远程托管服务。

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

阶段 4 的真实服务补审还固定了 SPlayer-Next `75b4301c`、UnblockNeteaseMusic/server `39e21bfb` 和 kugou-music-api Go `950cbf0b`。前两者仅用于验证匿名搜索 Endpoint 的公开实践，因 AGPL-3.0/LGPL-3.0 不复制代码；Go 项目为 MIT，用来交叉复现 `/v3/search/song` 当前返回 `152` 的行为。最终匿名路径由本项目以最小 HTTPS 请求独立实现并通过真实服务测试。

## 许可证注意

- MoeKoeMusic PC 与 Mobile 使用 GPL-2.0。
- Metrolist、Kreate、OuterTune、RiMusic 等多为 GPL-3.0。
- Now in Android、Compose Samples 和 AndroidX Media 使用 Apache-2.0。
- KuGouMusicApi 使用 MIT。
- 参考思想不等同于复制代码；复制或迁移具体实现前必须确认对应许可证义务。
