# 酷狗同步歌词协议、解析与缓存参考审计

状态：Accepted（协议与数据层）。审计日期：2026-08-06。

## 决策范围

本审计允许实现酷狗在线歌曲的歌词候选搜索、KRC 下载与解包、逐字/翻译/音译解析、类型化领域映射和本地成功缓存。歌词主内容态 [`07-player-lyrics.png`](../design/mockups/07-player-lyrics.png) 与加载、无歌词、离线无缓存、协议失败、大字体等 `23a` 至 `23h` 补充状态均已确认，允许在既定数据与架构门禁下实现歌词 Compose 页面。

本切片只支持 `PlaybackSource.Kugou`。本地导入歌曲的内嵌歌词、同目录歌词文件、手动搜索/选择候选、歌词偏移设置和多提供方回退分别属于后续规格；不得用标题模糊搜索给本地歌曲自动匹配可能错误的歌词。

## 产品语义与状态矩阵

歌词只在用户首次切换到歌词页时加载。进入全屏播放器的封面页不得产生歌词网络请求；同一歌曲的成功缓存可直接读取。歌曲切换必须取消旧请求并清除旧行，旧歌曲结果不得覆盖新歌曲。

| 状态 | 数据行为 | 未来 UI 行为 | 恢复 |
| --- | --- | --- | --- |
| 未请求 | 不访问缓存或网络 | 封面页无歌词状态 | 进入歌词页后请求 |
| 缓存命中 | 解析已缓存的解包 KRC 文本 | 直接显示内容，不闪全页 Loading | 缓存损坏时删除并回源一次 |
| 首次加载 | 按 hash 搜索候选，再下载 KRC | 保留播放器背景和控制层级，歌词区域内加载 | 成功、无歌词或失败 |
| 内容 | 提供行、逐字时间、翻译和音译；缺少附加文本不影响原文 | 按 `07` 主态显示 | 点击歌词行 seek |
| 无候选 | 返回稳定 `NotFound`，不写持久负缓存 | “暂无歌词”内联空态 | 显式重试；后续再次进入可重试 |
| 离线且有缓存 | 只读缓存 | 正常内容，可弱提示离线 | 网络恢复不强制刷新 |
| 离线且无缓存 | 返回 `Offline` | 内联离线状态 | 显式重试 |
| 超时/连接/5xx | 返回类型化可恢复错误 | 内联错误与重试 | 执行器仅对幂等读取有限重试；用户可再重试 |
| 响应/解包/解析失败 | 返回 `Protocol`，不缓存半成品 | 内联失败，不展示部分乱码 | 新请求或升级解析器后重试 |
| 非酷狗来源 | 返回 `UnsupportedSource`，不发网络请求 | 暂无歌词，不伪造在线匹配 | 后续本地歌词规格 |
| 歌曲切换/离页 | 协作取消当前 Job；`CancellationException` 必须继续抛出 | 新歌曲回到未请求；离页不残留 Loading | 新歌曲进入歌词页时重新加载 |

成功缓存长期保留原始解包 KRC 文本，不持久缓存 `NotFound` 或网络错误。这样解析器升级后可以重新映射已有歌词，也避免一次临时服务失败造成长期空态。

## Android 官方约束

- [Android coroutine best practices](https://developer.android.com/kotlin/coroutines/coroutines-best-practices)：Repository 暴露主线程安全的 `suspend` API，调用方控制生命周期；不能吞掉 `CancellationException`。`PlayerViewModel` 在歌曲变化或清除时取消旧 Job。
- [Room database migrations](https://developer.android.com/training/data-storage/room/migrating-db-versions)：新增缓存表使用显式 `3→4` 迁移并保留现有本地音乐和播放快照；禁止 destructive fallback。提交 schema JSON，并同时验证 `3→4` 与完整 `1→2→3→4`。
- [Test and debug Room](https://developer.android.com/training/data-storage/room/testing-db)：迁移需要在 Android 设备的真实 SQLite 上验证。本项目继续只使用用户指定真机，不创建模拟器。
- [Pager in Compose](https://developer.android.com/develop/ui/compose/layouts/pager)：视觉门禁通过后，封面与歌词使用 `HorizontalPager`；只有歌词页实际进入组合/可见状态后才触发加载。

## 固定协议源码

### KuGouMusicApi

- 仓库：`MakcRe/KuGouMusicApi`；固定提交：`6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb`；许可证：MIT。
- 本地 Git 对象位于 `../MoeKoeMusic-Mobile/api`，可用 `git show <commit>:<file>` 读取，未切换用户工作树。
- 文件：
  - `module/search_lyric.js`
  - `module/lyric.js`
  - `util/util.js` 的 `decodeLyrics`
  - `util/request.js`

固定实现确定了两段式流程、候选 `id/accesskey`、KRC 下载参数，以及 `krc1` 四字节头后使用 16 字节循环密钥 XOR、再执行 zlib inflate 的解包算法。KRC 解包属于酷狗协议，保留在 `:kugou-api`，不得移动到 Compose、ViewModel、Repository 或 Service。

固定 `search_lyric.js` 使用 `/v1/search`，而两个成熟 Android 固定版本使用 `/search`；`lyric.js` 又会注入设备默认参数并签名，而成熟实现使用公开无签名下载。由于固定来源相互冲突且线上行为已经漂移，本审计按门禁执行了最小匿名探针，结论见“服务漂移验证”。

### MoeKoeMusic PC

- 本地提交：`52c9833afe2e7fedcba8d5b23ff8d1f9731af73a`；许可证：GPL-2.0-only。
- 文件：`src/components/player/LyricsHandler.js`、`src/components/PlayerControl.vue`。

采用：按当前歌曲 hash 请求；后发请求通过 generation/request id 拒绝旧结果；KRC 行级和逐字段时间作为权威；`language` 元数据中的 `type=1` 翻译和 `type=0` 音译是可选附加内容；点击歌词定位播放。

不采用：Vue 可变字符高亮、DOM 查询/手写位移、Electron IPC、桌面歌词、LocalStorage 设置、Font Awesome 和桌面多模式布局。只学习产品语义与消费链，不复制 GPL 代码。

### MoeKoeMusic Mobile React Native

- 本地提交：`ab71195d4cf3297332490fd37704d1ae8973d4c5`；API submodule `283f1e97b110726b208a64b486a657c0fc0a6126`；许可证：GPL-2.0-only / API MIT。
- 文件：`src/features/player/lyrics.ts`、`store.ts`、`types.ts`、`src/components/ui/lyrics-view.tsx`。

采用：只有歌词页进入后加载；歌曲 load sequence 防止旧结果覆盖；当前行用二分查找；用户手动滚动后暂停自动跟随；点击行 seek。

不采用：RN 第一版只请求 LRC，丢失 KRC 逐字、翻译和音译，不能满足 V2 产品范围；也不采用 Zustand、Expo LinearGradient、MaskedView、Tamagui、ScrollView 或其近似图标。

## 成熟 Android 项目审计

### Kreate

- 固定提交：`f02577e862318df26b13abb02d14d8d824a1b947`；许可证：GPL-3.0。
- 文件：`extensions/kugou/.../KuGou.kt`、`SearchLyricsResponse.kt`、`DownloadLyricsResponse.kt`、`composeApp/.../SynchronizedLyrics.kt`、`LyricsTable.kt`。

采用：Ktor 协程请求、候选与下载 DTO 分离、成功歌词按歌曲缓存、当前行状态与高频播放位置分离。

不采用：关键字清洗和按时长模糊匹配作为酷狗歌曲的首选路径、只取 LRC、GPL 实现、项目偏好矩阵和数据库结构。V2 已持有精确歌曲 hash，应优先避免误匹配。

### Metrolist

- 固定提交：`289ed45d0a429e6e4f19a2a07543b243a0ab2e8b`；许可证：GPL-3.0。
- 文件：`kugou/.../KuGou.kt`、`app/.../lyrics/KuGouLyricsProvider.kt`、`LyricsHelper.kt`、`LyricsEntry.kt`、`viewmodels/LyricsViewModel.kt`。

采用：Provider/Repository 与 UI model 分层；协程取消旧处理任务；内存缓存和持久缓存都是歌曲键控；解析在 `Dispatchers.Default`；无网络时先使用已有缓存。

不采用：多歌词源编排、AI 翻译、全局 `LruCache`、手建 `CoroutineScope(SupervisorJob())`、Timber/遥测、只取 LRC、单一 app 模块和 GPL 代码。

两项目仅作为结构和失败恢复证据，不迁移 GPL-3.0 源码。

## 成熟库选型

采用 Maven Central `com.mocharealm.accompanist:lyrics-core:0.4.7`，源码固定为 `6xingyv/accompanist-lyrics-core@d1bea0b27d915183d960467a9b3b65072b10ddf4`，Apache-2.0。该版本使用 Kotlin 2.3.0、kotlinx.serialization 1.10.0，并以 Java 21 classfile 发布 JVM 变体。2026-08-06 接入验证确认 Kotlin 2.3.21 / serialization 1.11.0 可以编译，AGP 9.3/D8 可以将其转换进 Java 17 目标的 Debug APK；JVM 单元测试必须使用 Java 21 toolchain，不能继续由 Java 17 Test Worker 加载该依赖。

采用理由：

- `KugouKrcParser` 已覆盖 `[start,duration]`、逐字段 `<offset,duration,...>`、翻译、音译/phonetic、对唱与背景行，并带真实格式单测。
- 返回统一 `SyncedLyrics`/`KaraokeLine`/`KaraokeSyllable`，避免项目自行维护复杂正则和语言元数据对齐。
- 纯 Kotlin/JVM、无 Android UI 依赖、无网络、无遥测，许可证与 GPL-2.0-only 项目兼容。

不采用同项目的 `lyrics-ui`：本项目已经有确认的 `07-player-lyrics.png`、Design System、交互原型和 Compose 性能约束，通用 UI 会引入第二套视觉与状态所有权。

库解析的是已解包文本，不负责酷狗二进制协议。Base64、`krc1` 校验、循环 XOR 和 zlib 解包继续用 JDK `Base64` 与 `InflaterInputStream` 独立实现；这是协议适配而不是重复造歌词解析库。不新增 pako、Apache Commons Compress 或本地 native 库。

第三方库类型不得跨出 `:data`。Repository 将其映射为本项目稳定领域类型，并过滤空文本、负时间、结束早于开始、零长异常字段和超大内容。

## 当前服务漂移验证

2026-08-06 因固定源码与两个成熟项目冲突，执行一次不含 token、Cookie、MID、dfid、手机号或设备标识的匿名 HTTPS 探针；只记录状态与结构，临时响应随后移入废纸篓：

- `GET https://lyrics.kugou.com/v1/search`：HTTP 200，但正文为空，响应头 `x-kg-errcode=20010`。
- `GET https://lyrics.kugou.com/search`：HTTP 200，服务码 200，设计稿歌曲关键字返回 20 个候选。
- `GET https://lyrics.kugou.com/download`：使用候选 `id/accesskey`，无签名与身份参数，HTTP 200，`contenttype=0`，返回非空 KRC。
- KRC 验证：头为 `krc1`，6273 字节内容解包为 15957 字节 UTF-8，包含 55 行逐字段歌词和 `language` 元数据。未记录正文、候选 id/accesskey 或歌曲响应。

因此实现采用当前 `/search` 与 `/download`，两者 `includeDefaultParams=false`、`signatureMode=None`，不发送会话 Cookie。若以后服务再次漂移，只允许在固定源码/成熟实现冲突时做同等最小探针并更新本审计；不得在普通运行时自动回退到未知 Host。

## 协议与安全边界

### 搜索

`GET https://lyrics.kugou.com/search`，参数固定为 `ver=1`、`man=yes`、`client=pc`、`hash=<songHash>`；可附加已知播放时长用于候选排序，但精确 hash 是唯一第一切片入口。返回只读取 `candidates[]` 的 `id`、`accesskey` 和可选 duration；候选为空是 `NotFound`，关键字段缺失是 `Protocol`。

### 下载

`GET https://lyrics.kugou.com/download`，参数为 `ver=1`、`client=android`、`id`、`accesskey`、`fmt=krc`、`charset=utf8`。只兼容服务已出现的数值 `contenttype=0` 与字符串 `contenttype="0"`，进入 KRC 解包；其他类型不按 LRC 猜测。搜索与下载均是显式幂等读取，可沿用执行器对 timeout/5xx 的有限重试，4xx、协议错误与无候选不重试。

### 解包限制

- Base64 编码正文最大 `2 MiB`；解包 UTF-8 最大 `8 MiB`，超过即 `Protocol`，防止压缩炸弹和异常内存占用。
- 必须精确校验四字节 `krc1`；XOR 后使用 zlib 包装的 `InflaterInputStream`，不使用 raw deflate 模式。
- UTF-8 必须严格解码；解包失败、非法 Base64、非法 UTF-8和空文本均为 `Protocol`，不得返回空字符串冒充无歌词。
- 日志只记录 endpoint、HTTP/服务状态、候选数量和字节数，不记录 hash、候选 id/accesskey、歌词正文、翻译或音译。

## 模块、缓存与数据流

```text
:app AppPlayerLyricsViewModel（组合根）
        │ LyricsRepository（只在歌词页可见时调用）
        ▼
:data KugouLyricsRepository
        ├── LyricsCacheDao (:core:database)
        ├── lyrics-core 0.4.7 KRC parser
        └── KugouOnlineClient (:kugou-api)
                    │ search → download → Base64/XOR/zlib
                    ▼
             lyrics.kugou.com
```

- `:kugou-api` 新增请求、DTO decoder 和 KRC 解包，返回解包后的协议 DTO；动态 JSON、候选和 `accesskey` 不越过模块。
- `:core:model` 新增稳定 `LyricsDocument`、`LyricsLine`、`LyricsSyllable`、`LyricsResult/Error` 与 `LyricsRepository`。领域时间使用 `Long` 毫秒，第三方 `Int` 在 data 映射时做非负和范围校验。
- `:data` 负责来源判断、Room cache-first、调用在线协议、使用 `lyrics-core` 解析和映射。相同 hash 的并发请求 single-flight；调用协程取消后不得转为普通失败。
- `:app` 的独立 `AppPlayerLyricsViewModel` 注入 `LyricsRepository`，将领域文档映射为 `:feature:player` 的纯 UI 状态，并只在 Pager 已 settled 到歌词页时调用；这是保持 `:feature:player` 仅依赖 Design System、不得引入 `:core:model`/data/Hilt 的已批准组合根落点。它不创建或控制 `PlaybackController`；播放 seek 仍由 `AppPlaybackViewModel` 处理。
- `MediaLibraryService`、`:playback`、Compose Screen 不访问歌词网络、Room 或解析器。

缓存表以 `source_key = "kugou:<lowercase hash>"` 为主键，只保存解包 KRC 文本、`parser_version` 与 `updated_at_epoch_ms`，不保存候选 id、其摘要或 accesskey。读取缓存解析失败时原子删除该行并允许一次回源；写入只发生在完整解包和解析成功后。

## 视觉门禁

已确认 [`07-player-lyrics.png`](../design/mockups/07-player-lyrics.png) 覆盖正常逐字内容态，`23a` 至 `23h` 已确认补充以下静态状态；[`player-flow`](../design/prototypes/player-flow/README.md) 只验证封面/歌词分页、点击定位、手动滚动暂停跟随与返回层级：

- 歌词区域加载；
- 无歌词/非酷狗来源；
- 离线无缓存；
- 协议/服务失败与明确重试；
- 只有原文、原文+翻译、原文+音译三种内容密度；
- `1.5×`/`2.0×` 字体下的歌词与核心控制。

`07` 与 `23a` 至 `23h` 已经完成静态设计和确认，可直接实现 `HorizontalPager` 与歌词 Compose。只有未来出现现有图片未覆盖的新状态时，才使用 `frontend-design` 以确认基座生成单状态图并等待确认。

## 实施切片与原子提交

1. `:kugou-api`：`/search`、`/download`、DTO、KRC 解包、请求快照和 Node/Kotlin 固定向量；不接 UI。
2. 依赖与领域：引入 `lyrics-core 0.4.7`，建立稳定领域模型和 parser mapping 单测；第三方类型不外泄。
3. Room cache：schema v4、DAO、`3→4` 与 `1→2→3→4` 迁移测试、Repository cache-first/single-flight。
4. 设计：`07` 主态与 `23a` 至 `23h` 补充状态已经确认，可直接作为歌词 Compose 基线；只有新发现的未覆盖状态才补图确认。
5. UI：app-side `AppPlayerLyricsViewModel`、`HorizontalPager`、歌词列表/逐字高亮/点击 seek 和截图；手动滚动暂停自动跟随保留后续切片。
6. 真机：只在用户指定且已连接的真机验证，不启动模拟器；用户手动反馈结果，除非用户明确授权，不使用 ADB 截图。

每个切片通过对应验证后立即形成只包含该切片的原子提交。

## 测试与验收矩阵

- `:kugou-api`：两 Endpoint origin/path/query/header/无身份/无签名快照；候选空、字段缺失、内容类型漂移；Base64、头、XOR、zlib、UTF-8、尺寸上限；timeout/5xx 有限重试与 4xx/协议错误不重试。
- Node/Kotlin：固定虚构 KRC 文本由 Node 基准压缩/XOR/Base64，Kotlin 解包结果逐字节一致；不得使用真实歌词作为 fixture。
- `:data`：`lyrics-core` 映射逐字段时间、翻译、音译、背景/对唱、畸形字段过滤；缓存命中、损坏删除回源、成功写入、错误不写入、并发 single-flight、取消不缓存。
- Room：schema 4 导出、`3→4` 保留已有队列/本地音乐、完整 `1→2→3→4`；真机真实 SQLite 验证，不用模拟器结果冒充。
- `:feature:player`：未请求、加载、内容、无歌词、离线、错误、重试、歌曲切换旧结果隔离；标准/`1.5×`/`2.0×` 截图；逐字高亮不导致整页或封面每帧重组。
- 真机手动：进入封面不请求、首次滑到歌词加载、再次进入缓存命中、切歌取消旧结果、点击行 seek、手动滚动暂停跟随、离线缓存/无缓存、三键导航安全区、返回保留播放。
- 真实服务：仅在离线固定测试通过后做最小状态验收；不保存正文、hash、候选、accesskey、Cookie 或设备身份。

## 门禁结论

协议、技术栈、依赖、数据流、缓存、失败恢复与测试矩阵无关键待定项，且 `07` 与 `23a` 至 `23h` 已完成视觉确认。歌词 Compose 门禁已经开放，可在首页与用户链路优先切片完成后按既定架构实施。
