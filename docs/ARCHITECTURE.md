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
:feature:home
:feature:discover
:feature:my
:feature:search
:feature:localmusic
:feature:foundation
:feature:login
:feature:player
:feature:playlist
```

### `:app`

- Application、MainActivity 和应用级导航壳。
- Hilt 组合根与模块装配。
- Deep Link、外部音频 Intent、Android 系统启动入口和顶层应用壳。
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
- Ktor Client 请求编排、OkHttp Engine 与网络 DTO。
- 原始 JSON 与协议响应只在模块内部流动；`KugouOnlineClient`、`KugouAuthenticationClient` 与 `KugouUserClient` 分别将公开内容、认证/风险、已认证资料和 VIP 协议解码为类型化结果后才交给 `:data`。
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
- 歌曲地址解析通过 `KugouSourceResolver` 端口注入，不直接依赖具体酷狗实现；`:data` 返回短期 HTTPS 字符串，`:playback` 在 Service 边界转换为 Android `Uri`，地址不进入 Room 快照。

### `:feature:*`

- 每个业务能力独立拥有导航键、导航注册、Route、Screen、ViewModel 和测试。
- 当前模块为 `home`、`discover`、`my`、`search`、`localmusic`、`login`、`player`、`playlist` 与仅 Debug 可达的 `foundation`。
- `:feature:login` 按 [`plans/07-login-flow.md`](plans/07-login-flow.md) 拥有表单、倒计时、多账号选择、导航入口和测试；`:app` 只组合导航和平台安全配置，不持有认证 UI 状态。
- `:feature:my` 消费 `UserProfileRepository` 与 `AuthRepository`，拥有匿名、加载、已认证、部分失败和完整失败状态，并在页面恢复时刷新资料；退出必须经过确认且仅在会话清除成功后切换匿名态。匿名用户点击账号资产时，Feature 只上抛具名导航事件，由 `:app` 复用唯一登录目的地；Screen、ViewModel 和 Repository 不通过禁用文案或字符串判断模拟权限，也不在进入登录前发起受认证资产请求。本地音乐与应用级设置不经过该门禁。
- `:feature:settings` 拥有设置 Destination、Route、Screen、ViewModel 和测试，只依赖 `:core:model` 的应用偏好端口与 `:core:designsystem`；`:feature:my` 只上抛设置导航事件，`:app` 注册唯一目的地并以独立 app-level ViewModel 消费全局主题。设置页不直接访问 DataStore，也不显示没有真实消费者的假开关。
- `:feature:player` 拥有全屏播放器目的地、无状态 Screen、纯 UI 状态和截图基准；它只接收 `:app` 传入的播放状态与事件，不依赖 `:playback`、Service、数据库或网络。歌词协议、领域映射和成功缓存位于 `:kugou-api`、`:data` 与 `:core:database`；后续只通过 `LyricsRepository` 和页面 ViewModel 接入，歌词现有主态与补充状态均已确认。
- `:feature:playlist` 拥有歌单详情目的地、无状态 Screen、纯 UI Model、事件端口和截图基准；详情可由首页、发现或“我的”复用，任何来源 Feature 都不依赖它。首个视觉切片不依赖 Repository、`:playback` 或网络，真实歌单纵向切片后续在本 Feature 内补 ViewModel。
- Screen 与实现细节默认 `internal`；组合根只依赖少量稳定导航入口。
- Feature 不依赖 App，也不直接依赖其他 Feature 的实现。
- 出现跨 Feature API、多 App 复用或可替换实现需求时，再按 ADR-0004 拆为 `api/impl`。

## 模块依赖约束

```text
:app ───────────────► :feature:*
  ├──────────────► :playback
  └──────────────► :data

:feature:* ───────► :core:model（按需）
:feature:* ───────► :core:designsystem
:feature:search ──► SearchRepository
:feature:localmusic ► LocalMusicRepository + :playback
:feature:foundation ► :playback
:feature:login ────► AuthRepository + :core:designsystem
:feature:my ───────► UserProfileRepository + AuthRepository + :core:designsystem
:feature:player ───► :core:model + :core:designsystem
:feature:playlist ─► :core:designsystem

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
PlayerScreen / MiniPlayer (:feature:player / :core:designsystem)
          │ values + event lambdas
          ▼
AppPlaybackViewModel (:app)
          │ StateFlow + commands
          ▼
PlaybackController (:playback)
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
- 播放进度使用独立低粒度流；`:app` 将其映射为独立 `PlayerProgressUiState`，只有进度条和时间子组合读取该 `State`，封面、背景和标题不订阅每秒更新。
- 全屏播放器是 Navigation Compose 子目的地，不是一级 Tab；进入后隐藏 MiniPlayer 和底部导航，退出后恢复原来源页面。队列仍是同层 `ModalBottomSheet`，返回先关闭队列再退出播放器。

## 数据策略

- Android 系统 Splash 结束后立即组合应用壳；`:app` 不建立 `AppStartupRepository`、条件启动门禁或跨业务初始化状态机，也不等待会话、网络、Room、DataStore 或缓存后再绘制首屏。
- `MainActivity` 的本地导入入口使用 Hilt `Lazy`，只有用户实际发起导入或扫描时才解析 `LocalImportGateway`，避免为未使用功能在 Activity 创建阶段构造 Room/WorkManager 依赖图。
- 匿名设备会话由真正需要它的 Repository 按需初始化；首页、搜索、“我的”等 Feature 分别拥有加载、错误、离线、刷新与缓存内容状态。缓存只改变页面取数和恢复策略，不改变启动导航。
- 数据库或缓存错误由对应 Repository 映射为页面状态或功能反馈；不能为保护某个业务数据源而把整个应用阻断在启动页。
- 首页、发现等可缓存内容采用 stale-while-revalidate：页面先读取并展示上次完整成功快照，同时自动在后台刷新；刷新成功后原位替换内容并更新快照，失败时保留旧内容并只给弱提示。只有从未存在可展示缓存时才进入首次加载或全屏错误状态。
- 页面缓存由所属 Repository 管理，ViewModel 通过不可变状态同时表达 `content`、`isRefreshing` 与非阻断刷新错误。缓存读取、远端刷新和快照提交可取消且按请求代际隔离，旧刷新结果不得覆盖更新内容。
- 内存缓存用于同进程快速恢复；需要跨进程保留的首页/发现完整成功快照使用 Room 或经审计的数据存储。缓存键包含用户身份、分页和影响响应的参数，未完成、部分损坏或协议失败结果不得覆盖最后一次成功快照。
- 首页具体使用 `home:v1:anonymous` / `home:v1:user:<userid>` 身份分区的 Room 单行版本化快照；15 分钟只抑制重复自动刷新，不作为展示硬过期。每日推荐和推荐歌单成功且至少一个必需区块可展示时才事务替换持久快照；当前服务拒绝的轮播是可选区块，成功时随完整快照保存，失败时不伪造内容。其他部分结果只可作为当前进程的临时内容，不能覆盖旧快照。损坏 payload、未知 schema、single-flight 与请求代际规则见 [`reference-audits/15-home-content-and-cache.md`](reference-audits/15-home-content-and-cache.md)。
- 用户歌单和收藏以远端为权威，Room 可保存展示快照和待重试操作。
- 本地音乐以 App 专属目录中的已提交副本和 Room 索引为权威；MediaStore、Storage Access Framework 和外部 Intent 只提供导入来源。
- WorkManager 的输入只保存 `batchId`；URI、逐项状态和进度归 Room 所有，全局唯一工作链保证复制串行执行。
- 酷狗歌词缓存只保存 `kugou:<lowercase hash>`、已解包 KRC、parser 版本和更新时间；候选 id/accesskey、失败与 `NotFound` 不持久化。缓存损坏由 `:data` 删除后最多回源一次，`:playback` 与 Service 不读取歌词表。
- `:core:database` 拥有 Schema 与 Migration；`:data` 负责文件事务、映射和导入编排。
- 非敏感应用设置由 `:data` 使用独立 Preferences DataStore，并通过 `:core:model` 的 `AppSettingsRepository` 暴露；读取失败回退安全默认值且保留类型化问题，写入失败不覆盖最后持久值。酷狗敏感会话继续由 `:data` 使用 Android Keystore AES-256-GCM 加密后写入另一独立 DataStore。`:kugou-api` 只依赖 `KugouSessionStore` 端口，不依赖 Android Framework。
- 会话密文损坏、Keystore key 缺失或 GCM 校验失败时清除密文与旧 key，重新进入匿名注册；不把不可解密状态降级为明文存储。
- 登录成功响应由 `:kugou-api` 类型化解码，`:data` 在认证互斥区内合并并一次写入加密会话；存储完成前 UI 不进入已登录。退出只清除 token、userid 和登录 Cookie，保留匿名设备身份与 dfid。
- 密码风险挑战在协议层保留 `ssa-code`，`sid/edt` 仅透传真实响应字段并允许缺失；禁止将固定 Node 层生成的鼠标轨迹或 WebGL 模拟迁入原生客户端。验证写操作与原密码重试由 Feature 显式串行驱动，不进入全局重试器。
- `KugouUserProfileRepository` 并发读取用户资料和 VIP 摘要；用户资料是页面成立的权威结果，VIP 失败可降级为不可用状态。普通资料刷新失败不清除持久会话，也不以缓存或设计稿假数据伪装成功。
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
- `AudioImportActivity` 使用 `singleTop`：冷启动创建独立导入入口；应用已运行但导入页不在顶部时仍创建导入页；导入页已在顶部时通过 `onNewIntent` 复用当前实例。每个新 Intent 停止旧 UI 观察并观察新批次，但不取消已经交给 WorkManager 的导入事务。
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
- `:data`：Repository、DTO/Domain 映射和加密会话存储测试，使用真实替身而非过度 mock；Android Keystore 行为必须在设备上验证。搜索 Repository 只暴露稳定领域分页和类型化错误，匿名公开搜索请求不携带设备会话。
- `:playback`：队列、播放模式、恢复和错误跳过状态机测试。
- `:feature:*`：各自拥有 ViewModel 单元测试、Compose UI 测试和关键截图测试；“我的”覆盖匿名、已认证和 `1.5×` 字体基准。
- `:app`：导航、启动、登录、账户状态和播放闭环的设备测试。
- 外部 Intent：解析与导入协调单测，以及 API 26、32、33、36 设备测试。
- 稳定后增加 Macrobenchmark 与 Baseline Profile。

完整测试矩阵和 CI 门槛见 [`TESTING_STRATEGY.md`](TESTING_STRATEGY.md)。
