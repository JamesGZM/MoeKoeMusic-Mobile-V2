# 软件架构

## 架构目标

- 支撑 PC 端主要功能逐步迁移。
- 保持播放器生命周期与页面生命周期解耦。
- 酷狗协议变化时，影响集中在 `:kugou-api` 和数据层。
- 对小型开源团队保持可理解、可测试和可迭代。
- 避免过度模块化和形式化 Clean Architecture。

## 总体模式

项目采用 MVVM、单向数据流和分层 Repository：

```text
Compose UI
    │ UiEvent
    ▼
ViewModel
    │ StateFlow<UiState>
    ▼
UseCase（仅复杂或跨仓库业务）
    ▼
Repository
    ├── KuGou API
    ├── Room
    ├── DataStore / secure storage
    └── PlaybackController
```

事件向下流动，状态向上流动。UI 不直接操作数据源。

## 初始 Gradle 模块

```text
:app
:core:model
:core:common
:core:designsystem
:core:database
:kugou-api
:data
:playback
:features
```

### `:app`

- Application、MainActivity 和应用级导航壳。
- Hilt 组合根与模块装配。
- Deep Link、外部音频 Intent、启动流程和顶层错误恢复。
- 不放具体页面业务。

### `:core:model`

- Song、Album、Artist、Playlist、Lyrics 等稳定领域模型。
- 不依赖 Android UI、网络 DTO 或数据库 Entity。

### `:core:common`

- Dispatcher、时间、日志抽象、通用结果和错误类型。
- 不成为无边界的 `utils` 垃圾桶。

### `:core:designsystem`

- 颜色、排版、形状、间距、动效 token。
- MoeKoe 主题、图标和通用 Compose 组件。
- Preview 与截图测试基础设施。

### `:core:database`

- Room Database、DAO 和 Entity。
- 播放历史、队列快照、缓存元数据和本地歌单数据。
- Entity 不泄露到 UI。

### `:kugou-api`

- 酷狗平台配置、设备身份、会话和 Cookie。
- MD5、SHA1、AES、RSA 和请求签名。
- OkHttp 请求执行与网络 DTO。
- 不依赖 Compose、Media3、Activity 或 Service。

### `:data`

- Repository 实现和 DTO/Entity/Domain 映射。
- 决定远端、本地和缓存的组合策略。
- 对上提供稳定领域接口。
- 通过平台数据源完成本地音频复制、哈希、元数据读取和 App 专属存储管理。

### `:playback`

- ExoPlayer、MediaSession、MediaLibraryService。
- 播放队列运行时、系统媒体命令和通知栏。
- `PlaybackController` 与可观察的 `PlaybackState`。
- 歌曲地址解析通过接口注入，不直接依赖具体酷狗实现。

### `:features`

初期使用单一 Android Library，内部按功能 package 划分：

```text
feature/home
feature/discover
feature/search
feature/details
feature/library
feature/player
feature/account
feature/settings
feature/cloud
feature/localmusic
feature/recognize
```

当单个功能编译时间、所有权或复用需求明显增长时，再拆成独立 Gradle Module。

## 模块依赖约束

```text
:app ───────────────► :features
  │                       │
  ├──────────────► :playback
  └──────────────► :data

:features ────────► :core:model
:features ────────► :core:designsystem
:features ────────► :playback
:features ────────► Repository interfaces

:data ────────────► :kugou-api
:data ────────────► :core:database
:data ────────────► :core:model
:data ────────────► :playback（实现 Snapshot、已导入来源解析及导入完成命令）

:playback ────────► :core:model
:kugou-api ───────► :core:common
```

禁止反向依赖：数据层不能依赖 feature，播放器不能依赖具体 UI，API 模块不能依赖 Android 页面。

## UI 状态模型

每个页面使用不可变状态：

```kotlin
sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Content(val result: SearchResultUiModel) : SearchUiState
    data class Error(val message: UiMessage) : SearchUiState
}
```

- ViewModel 使用 `StateFlow` 暴露状态。
- 一次性反馈使用可消费事件或 UI 层明确处理的 effect，不把 Toast 文案永久放入状态。
- 导航由 UI 响应事件执行，Repository 不持有 NavController。

## 播放器边界

```text
PlayerScreen / MiniPlayer
          │
          ▼
PlaybackController
          │ MediaController
          ▼
MediaLibraryService
          │
          ▼
ExoPlayer + MediaSession
```

### Service 负责

- ExoPlayer 和 MediaSession 生命周期。
- 音频焦点、系统媒体按钮、通知栏和锁屏。
- 当前运行队列与播放命令。
- 在进程允许范围内恢复播放状态。

### Service 不负责

- 搜索、推荐、登录和收藏业务。
- 页面 UI 状态。
- 直接拼装酷狗 API 请求。
- 大量数据库业务和设置页面逻辑。

### 状态来源

- Media3 是运行时播放状态的唯一事实来源。
- `:playback` 定义 `PlaybackSnapshotStore` 端口，`:data` 使用 `:core:database` 的 Room DAO 实现；该 SPI 依赖不允许数据层控制 ExoPlayer。
- Room 原子保存有序队列、当前索引、位置和模式，不保存 `isPlaying`，也不与播放器争夺实时状态所有权。
- 冷启动恢复固定暂停；只有 App 操作、系统媒体卡片或媒体按钮等主动入口可以请求继续播放。
- Compose 通过 Controller 事件转换出的 StateFlow 观察播放器。
- 播放进度使用独立低粒度流，避免整页高频重组。

## 数据策略

- 在线内容默认 network-first，并对短期可复用页面数据做内存缓存。
- 用户歌单和收藏以远端为权威，Room 可保存展示快照和待重试操作。
- 本地音乐以 App 专属目录中的已提交副本和 Room 索引为权威；MediaStore、Storage Access Framework 和外部 Intent 只提供导入来源。
- WorkManager 的输入只保存 `batchId`；URI、逐项状态和进度归 Room 所有，全局唯一工作链保证复制串行执行。
- `:core:database` 拥有 Schema 与 Migration；`:data` 负责文件事务、映射和导入编排。
- 设置使用 DataStore；酷狗敏感会话由 `:data` 使用 Android Keystore AES-256-GCM 加密后写入独立 DataStore。`:kugou-api` 只依赖 `KugouSessionStore` 端口，不依赖 Android Framework。
- 会话密文损坏、Keystore key 缺失或 GCM 校验失败时清除密文与旧 key，重新进入匿名注册；不把不可解密状态降级为明文存储。
- 不为了模仿 Now in Android 而强制所有在线内容采用完整 offline-first。

## 本地音乐与外部 Intent

```text
AudioImportActivity
        │ 解析并验证 ACTION_VIEW / SEND / SEND_MULTIPLE
        ▼
Local import coordinator
        │
        ▼
LocalMusicRepository ──► App 专属 Music 目录 + Room
        │                         │
        └──── 成功后的领域结果 ────┘
                                  │
                                  ▼
                         PlaybackController
```

- 外部入口 Activity 只解析和转交，不直接访问 Room 或 ExoPlayer。
- `AudioImportActivity` 只观察领域导入进度；Hilt Worker 执行复制，播放器仍只能由 `PlaybackController` 控制。
- 所有入口共用复制、校验、去重和提交管线。
- `ACTION_VIEW` 只有在文件落盘与 Room 提交成功后才发送播放命令。
- 外部 URI、ContentResolver 和绝对路径不得泄露到稳定领域模型。
- 本地导入的详细事务语义见 [`LOCAL_MUSIC.md`](LOCAL_MUSIC.md)。

## 依赖注入

默认使用 Hilt：

- 构造函数注入优先。
- 接口仅出现在确实存在替换、平台边界或测试替身的地方。
- 不使用 Service Locator 或静态全局容器访问业务依赖。

## 错误模型

至少区分：

- Network：超时、断网和 HTTP 错误。
- Protocol：签名、解密、响应结构和风控错误。
- Auth：未登录、会话过期和二次验证。
- Playback：无版权、VIP 限制、URL 失效和解码错误。
- Storage：数据库、缓存和文件访问错误。

网络异常不能直接以服务端原文暴露给用户；同时保留脱敏诊断信息供日志定位。

## 测试架构

- `:kugou-api`：加密、签名、Cookie、序列化和请求快照测试。
- `:data`：Repository、映射和加密会话存储测试，使用真实替身而非过度 mock；Android Keystore 行为必须在设备上验证。
- `:playback`：队列、播放模式、恢复和错误跳过状态机测试。
- `:features`：ViewModel 单元测试、Compose UI 测试和关键截图测试。
- `:app`：导航、启动、登录和播放闭环的设备测试。
- 外部 Intent：解析与导入协调单测，以及 API 26、32、33、36 设备测试。
- 稳定后增加 Macrobenchmark 与 Baseline Profile。

完整测试矩阵和 CI 门槛见 [`TESTING_STRATEGY.md`](TESTING_STRATEGY.md)。
