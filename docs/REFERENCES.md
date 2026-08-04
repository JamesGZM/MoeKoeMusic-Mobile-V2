# 参考项目与来源

记录日期：2026-08-04。Stars、活跃度和 Release 会变化，本文重点记录参考价值，不将快照数据作为永久事实。

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

### 阶段 1 构建与 UI 基础

- AGP 9.3 版本说明与兼容矩阵：https://developer.android.com/build/releases/agp-9-3-0-release-notes
- Compose Preview Screenshot Testing：https://developer.android.com/studio/preview/compose-screenshot-testing
- Dagger / Hilt Releases：https://github.com/google/dagger/releases
- Material 3 Compose：https://developer.android.com/develop/ui/compose/designsystems/material3

采用点：AGP/Gradle/JDK 组合、官方截图测试、Hilt Android 组合根与 Material 3 主题体系。Hilt 2.59 开始明确支持 AGP 9，因此工程使用 2.59.2，而不是计划中无法加载 AGP 9 扩展的 2.57.1。Compose Screenshot Testing 仍为实验插件，仅用于视觉回归。

主要阶段 1 依赖许可证：AndroidX、Compose、Room、DataStore、Media3 与 Hilt 为 Apache-2.0；Kotlin 与 Coroutines 为 Apache-2.0；Spotless 为 Apache-2.0；OkHttp 为 Apache-2.0。它们分别承担平台 UI/存储/媒体能力、依赖注入、语言与并发、格式检查和后续网络传输，未引入遥测或远程托管服务。

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

## Android 本地文件与系统集成

- Storage Access Framework：https://developer.android.com/training/data-storage/shared/documents-files
- MediaStore 与音频权限：https://developer.android.com/training/data-storage/shared/media
- Intent 与 Intent Filter：https://developer.android.com/guide/components/intents-filters
- 接收外部分享：https://developer.android.com/training/sharing/receive
- Media3 支持格式：https://developer.android.com/media/media3/exoplayer/supported-formats

采用点：使用窄范围内容 URI 授权、按 Android 版本申请音频读取权限、使用 `audio/*` 外部入口并由 Media3 验证可播放格式。不采用 `MANAGE_EXTERNAL_STORAGE`，不把临时外部 URI 当作长期音乐库来源。

## 许可证注意

- MoeKoeMusic PC 与 Mobile 使用 GPL-2.0。
- Metrolist、Kreate、OuterTune、RiMusic 等多为 GPL-3.0。
- Now in Android、Compose Samples 和 AndroidX Media 使用 Apache-2.0。
- KuGouMusicApi 使用 MIT。
- 参考思想不等同于复制代码；复制或迁移具体实现前必须确认对应许可证义务。
