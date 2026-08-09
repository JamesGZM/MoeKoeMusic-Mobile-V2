# 数据驱动公共 UI 架构审计

状态：Accepted（仅 `MoeSongRow` 生产迁移后置；第 8 至 10 项直接迁移获准公共组件）。审计日期：2026-08-09。

## 决策与当前阶段边界

MoeKoe Air 的目标架构是“稳定视觉容器 + 不可变展示数据 + 类型化事件”。Feature 只把业务结果映射为 Feature UI Model 与公共组件展示数据；公共组件不读取业务状态，也不让页面用颜色、尺寸或任意内容 Slot 重画内部。

当前阶段不变更设计稿、页面契约、截图、Golden、遮罩、模块依赖或图片加载依赖。尤其禁止引入、替换或迁移 `MoeSongRow` 生产 API：本阶段只能冻结它的 legacy 调用与增长，并完成其后续独立 UI 阶段所需的 Feature 内准备。已批准的 MiniPlayer、Snackbar/AlertDialog、Standard/Search TopBar 则在本阶段直接迁移生产接口和全部消费者，且必须保持现有视觉与行为像素不变。

## 已有依据与现状审计

- 公共组件审计已规定 Feature 使用薄适配层映射文案、状态和事件，公共 API 不暴露任意颜色、圆角、阴影、内部 Padding 或裸 `Dp`。[`17-design-system-components.md`](17-design-system-components.md)
- 本地音乐确认稿指定本地库、扫描、搜索、歌单和队列共用 `MoeSongRow`，差异只来自封面、文案、元数据、播放态、徽标与尾部操作。[`../design/evidence/local-music-2026-08-09.md`](../design/evidence/local-music-2026-08-09.md)

| 组件 | 当前公开面与消费者证据 | 当前阶段结论 |
| --- | --- | --- |
| `MoeSongRow` | `core/designsystem/src/main/kotlin/cn/james/music/core/designsystem/component/MoeSongRow.kt:58-82` 暴露 style、`Shape`、`Color`、字重和多个 Slot；首页、搜索、本地、扫描、歌单、队列分别在 `feature/home/.../HomeSections.kt:82-106`、`feature/search/.../SearchSongItem.kt:36-75`、`feature/localmusic/.../LocalMusicComponents.kt:89-118`、`DeviceScanScreen.kt:257-313`、`feature/playlist/.../PlaylistTrackItem.kt:44-98`、`feature/player/.../PlayerQueueItem.kt:49-105` 自行插入内部视觉。 | 冻结 legacy 调用、style、Slot、视觉覆写和 API 增长；最终数据 API 留给当前阶段之后的独立 UI 阶段。 |
| `MoeMiniPlayer` | `core/designsystem/src/main/kotlin/cn/james/music/core/designsystem/component/MoeMiniPlayer.kt:44-63`；应用壳在 `app/src/main/kotlin/cn/james/music/PlaybackShell.kt:32-51` 分别映射标题、进度、文案和 callback。 | 本阶段直接实现 `MoeMiniPlayerUiModel`/`MoeMiniPlayerEvent` 并迁移 app 消费者。 |
| Standard/Search TopBar | `core/designsystem/src/main/kotlin/cn/james/music/core/designsystem/component/navigation/MoeTopBars.kt:53-93,102-185`；歌单/资料传 `8.dp` action 偏移，搜索自绘 trailing Mic。 | 本阶段直接实现受控 action data/action ID 并迁移所有消费者。 |
| Snackbar / Alert Dialog | `MoeSnackbar.kt:47-55` 与 `component/overlay/MoeDialogs.kt:72-118`；app、home、settings、my、localmusic、login 均消费。 | 本阶段直接实现 `MoeSnackbarUiModel/Event`、`MoeAlertDialogUiModel/Event` 并迁移消费者；复杂 Dialog 内容继续留 Feature。 |
| Artwork / TextField / Button | `MoeMediaPrimitives.kt:27-41`、`component/input/MoeTextField.kt:48-64`、`component/action/MoeButtons.kt:40-47`。 | 不把输入和原子动作伪装成业务模型；以后只在有审计时收紧裸视觉参数。 |

## 固定数据流与模型硬边界

```text
Domain / Repository result / PlaybackController snapshot
                    │
                    ▼
Feature ViewModel ── Feature mapper ──► immutable Feature UI Model
                    │                         │
                    │ StateFlow               ▼
                    └────────────────────► Screen / frozen legacy wrapper
                                               │ typed event
                                               ▼
                              Route / ViewModel / app root
```

- Repository、DTO、Entity、Service、`PlaybackState` 与 Android 平台对象止于 mapper 之前；它们不进入 Screen 或 `:core:designsystem`。
- Feature 持有 Feature UI Model、页面事件、业务 mapper 与错误/加载/恢复策略。`:app` 继续拥有应用级导航、MiniPlayer 可见性和播放命令接线。
- Route 获得 ViewModel、收集不可变状态并转发 typed event；Screen 不访问 Repository、数据库、Service、DataStore、ExoPlayer 或 NavController。
- 新的 Feature UI Model 与后续公共展示模型必须不可变，且禁止 DTO、Entity、Repository、DAO、UseCase、ViewModel、Service、`PlaybackState`、`PlaybackController`、`Context`、`File`、`Uri`、导航对象、`Dp`、`Shape`、`Color`、`TextStyle`、内部 Padding、对齐偏移、图标尺寸、页面布局规格及任意视觉 Slot。
- 文案、可访问性描述、稳定 id、可见/禁用/加载语义和已确认的枚举状态可以属于 UI Model。模型不是 Domain Model 的别名。

### 图片唯一例外

图片来源与解码继续归应用/Feature 现有加载链路；本阶段不向 Design System 引入 Coil 或新依赖。公共媒体组件最终只能保留一个受控像素 renderer：它仅能在固定封面容器内绘制图片或占位，不能控制容器尺寸、形状、Padding、叠层、文字或操作布局。展示模型如需关联图片，只能携带非视觉、稳定 `artworkKey`，不能携带 `File`、`Uri`、图片加载器或 Composable。

## 数据化适用边界

| 类别 | 决策 | 原因与 Slot 边界 |
| --- | --- | --- |
| `MoeSongRow`、歌曲徽标、More、扫描选择、歌单/队列尾部 | 当前阶段冻结；后续独立 UI 阶段必须数据化 | 同一音乐 Item 已有多个消费者，组件应拥有完整几何与尾部操作。仅保留固定封面容器 renderer。 |
| `MoeMiniPlayer`、Snackbar、简单 Alert Dialog、Standard/Search TopBar | 本阶段直接数据化并迁移生产接口 | 稳定容器和重复事件语义适合数据化；所有消费者在同一原子切片迁移，像素保持冻结。 |
| `MoeDialog` | 保留受控内容 Slot | 登录风险验证有不同业务内容结构；遮罩、居中、Surface 与操作容器归 Dialog，状态机归 Feature。 |
| `MoeTextField`、`MoeSearchField` | 不数据化为页面模型 | 受控输入的值、IME、密码变换、焦点和有限内容有平台语义。 |
| Button、Switch、Divider、Outline、NavigateBackIcon | 不数据化 | 原子控件或页面几何工具，不应成为万能展示模型。 |
| `MoeArtwork` | 后续只收紧为具名容器规格 | 它不是业务 Item；保留像素 renderer。 |
| 登录 Hero/Card、二维码、分段控件、发现页专属组合 | 留在 Feature | 尚无第二消费者或全局约束。 |

## MoeSongRow legacy 冻结与后续目标

### 当前阶段冻结

当前阶段和下列十个原子切片内，冻结所有现有 `MoeSongRow` 调用点、`MoeSongRowStyle`、Slot、视觉覆写和公开 API 增长：

- 不新增 style、Boolean、裸视觉参数、Slot、播放图标、徽标或尾部动作变体；
- 不让 Feature 新建歌曲行实现，或在歌曲行内新增 `GraphicEq`/`Equalizer`/More/Checkbox/拖拽/移除视觉；
- 不以修改设计图、contract、Golden、mask、probe 或截图阈值掩盖结构问题。

### 当前阶段之后的独立 UI 阶段

只有十个原子切片完成并获得新的独立 UI 授权后，`:core:designsystem` 才可建立不可变歌曲展示模型和一次性迁移 API。该模型将表达 `id`、标题、副标题、可选元数据、零到多个语义徽标、播放/可用状态、受限呈现模式和可选稳定 `artworkKey`；事件将限于 `Activate(id)`、`More(id)`、`SelectionChanged(id, selected)`、`Remove(id)` 及已确认队列操作。

- 标准音乐 Item 固有 More；本地音乐没有徽标时，徽标自然不显示，仍透传 `More(id)`。
- 在线 HQ/MV 等由 `badges` 数据决定；Feature 不再拼歌曲标题行。
- 只读扫描不产生尾部占位，选择态才显示 Checkbox；歌单序号、当前项、队列移除和不可用拖拽也由受限数据表达。

这些语义是未来目标，不是本阶段生产 API 或迁移授权。

## 当前阶段的十个原子切片

第 4 至 7 项是 Feature 架构改造；第 8 至 10 项直接迁移已批准公共组件的生产 API 与消费者。十项都不创建或迁移 `MoeSongRow` 生产 API，也不改变视觉、行为、组件 contract 或 Golden。

| # | 切片与所有权 | 当前阶段目标 |
| --- | --- | --- |
| 1 | 父子执行权治理 incident/eval；质量治理与父审查 | 已批准且已完成：`parent-mutation-boundary` incident/eval 的六个 assertion ID 是 `parent-no-implementation-mutation`、`delegated-write-format-generate`、`child-no-stage-or-commit`、`parent-validates-before-commit`、`parent-only-atomic-commit`、`identity-proof-not-overclaimed`。本阶段引用其已验证结果，不另建“Feature 控制内部视觉”incident/eval。 |
| 2 | 审计、目录、迁移清单；`docs/` | 建立本审计、计划、架构索引、组件目录和参考索引；当前文档切片到此为止。 |
| 3 | Build Logic 门禁；`build-logic` | 在独立代码授权下建立 SongRow legacy 冻结门禁和 data-component 参数门禁：阻止新增 `MoeSongRowStyle`/Slot/视觉覆写及禁止的新裸视觉参数；不重写 legacy，不宣称最终 SongRow API 已完成。 |
| 4 | `feature:search`、`feature:home` | 建立原子 `SearchSongUiModel` 与 Home Feature UI Model、mapper、typed action；Search 移除 `songs + decorator Maps`，Home 从生产模型移除 `preview*` 字段并将 fixture 留在 screenshot/debug source set。Screen 继续调用冻结 legacy wrapper，Compact 等既有像素不变。 |
| 5 | `feature:localmusic`（本地库与 DeviceScan） | 建立本地/扫描 UI Model 与 mapper；排序、过滤、格式化、候选映射移出 Composable。只读/多选继续调用冻结 legacy wrapper，现有 Checkbox 与像素不变。 |
| 6 | `:app` 与 `feature:player` 队列边界 | `:app` 映射 `PlaybackState`；Feature 只接收自己的 Queue UI Model 与 typed action，不接收 `PlaybackItem`、文件路径或播放运行态。队列继续调用冻结 legacy wrapper。 |
| 7 | `feature:settings`、`feature:my`、`feature:discover` | 建立页面私有 sealed `Row`/`Entry`/`Group` UI Model、稳定 id 与 typed action；消除平行 nullable 计数、selected index 与重复业务列表。保留 Feature 布局，不提前抽公共 Item。 |
| 8 | `:app` MiniPlayer | `:app` 把 `PlaybackState` 映射为 `MoeMiniPlayerUiModel`，直接实现 `MoeMiniPlayerEvent` 并一次迁移组件与 app 消费者；播放状态所有权仍归 `:playback`，像素不变。 |
| 9 | Snackbar / AlertDialog | 直接实现 `MoeSnackbarUiModel/Event`、`MoeAlertDialogUiModel/Event` 并一次迁移 app/Feature 消费者；复杂风险 Dialog 仍由 Feature 组合，像素不变。 |
| 10 | Standard/Search TopBar | 直接实现受控 action data/action ID 并一次迁移 Standard/Search TopBar 消费者；保留 Tencent 自定义 Close 图形和既有像素。 |

`MoeSongRow` 最终数据模型、一次性生产 API 替换与全部 legacy consumer 原子迁移属于十项完成后的独立 UI 阶段，不计入以上十项。

## 像素冻结与验证

每个准备或后续生产切片都必须维持已确认页面和组件的像素、可访问性语义、触控区、状态、导航与播放行为。迁移期间：

- 禁止修改或新增设计稿、设计契约、mask、截图 reference/Golden、布局 probe 或视觉阈值；禁止录制新截图期望值；
- 任何像素差异先按回归处理，除非用户另行批准独立视觉切片；
- 先运行受影响的 mapper/事件测试，再运行 `verifyArchitecture`、`verifyUiContracts`、`verifyUiImpact`、相应 `verifyUiFidelity` 与 `verifyUiGoldenChange`、受影响截图测试、Lint 和 Debug 构建；
- 冻结期 Build Logic 只检测新增 legacy 增长；现有调用只在后续独立 UI 阶段一次性迁移。

## 实施来源 trailer 的边界

未来由 Agent 协作完成的实现提交使用用户批准的 trailer：`Implemented-By-Agent: /root/<child-task>` 与 `Committed-By-Agent: /root`，并在提交说明列出实际验证命令。trailer 只提供工作流可追溯性；它、作者字段和 Agent 文本都可被复制或伪造，不能证明密码学身份、代码权属、人工复核或验收通过。可信身份必须依赖已批准的签名、受控 CI 身份和人工审查证据。

## 准入结论

此项工作不新增依赖、权限、持久化、后台任务、网络调用或模块边界。当前阶段允许十项原子切片中的文档、治理、Feature 架构改造及已批准的第 8 至 10 项公共组件生产迁移，并且每项须获得相应文件授权；禁止开始或暗示开始 `MoeSongRow` 生产迁移。UI 设计本轮保持不动。
