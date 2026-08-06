# 阶段 5A：内容与播放器 UI

状态：进行中。音乐内容组件、搜索/本地列表、MiniPlayer、基础队列与全屏封面页首个切片已经完成；歌词页和其余内容详情仍待实现。

## 目标

按照已批准视觉稿实现主要内容浏览和播放体验，不重新解释信息架构。

当前执行顺序固定为首页真实闭环优先；“我的”与登录随后完成，存量全屏封面只先补真机验收。动态色、歌词与完整队列在用户链路之后实施，发现及其余内容详情最后实施。

首页真实内容门禁见 [`../reference-audits/15-home-content-and-cache.md`](../reference-audits/15-home-content-and-cache.md)，状态已 Accepted。实现顺序固定为三个首页 Endpoint 与 Decoder、Room v5 完整快照、cache-first Repository、ViewModel、确认稿 Compose；前四个切片使用固定测试、迁移测试和自动截图/真机测试验收，不要求用户手动操作。排行榜与新歌不进入首页首批切片。

## 实现范围

- 底部一级导航只有首页、发现、我的；搜索是从首页进入的独立子页面。
- 实现首页、发现、搜索、歌单/专辑/歌手/排行榜详情。
- 实现 MiniPlayer、沉浸式封面页、歌词页和队列 Bottom Sheet。
- 播放页参考 Kreate 的大封面、封面驱动背景和封面/歌词切换，不复制其 Logo、双层底栏和过密操作。
- 接入 `MoeSnackbar`、`MoeToast` 和 Dialog；持续状态使用页面内组件。
- 页面覆盖加载、内容、空数据、错误、离线和无版权状态。

## 视觉验收

页面与设计稿的唯一映射见 [`../DEVELOPMENT_PLAN.md`](../DEVELOPMENT_PLAN.md)。颜色、排版、间距、圆角和组件尺寸以 [`../DESIGN_SYSTEM.md`](../DESIGN_SYSTEM.md) 为准确实现依据，视觉形态参考 `14` 至 `18` 号通用组件图板。文字必须支持 `1.0×`、`1.3×`、`1.5×`、`2.0×` 字体，交互目标不小于 `48dp`。

`:core:designsystem` 的 Primary、Typography、Spacing、Shapes、Toolbar 和音乐内容组件已经完成第一批校准；后续页面必须继续消费这些 Token 和组件，不得在业务页面用局部常量绕过校准。

## 当前结果（2026-08-06）

- `:core:designsystem` 已实现 Section Header、封面容器、媒体徽标、歌曲行、MiniPlayer 和队列行，并建立浅色、深色与 `1.5×` 字体截图基准。
- Search 与 LocalMusic 已移除各自的重复歌曲行布局，统一使用 `MoeSongRow`；在线封面和 App 专属目录本地封面继续由 Coil 3 加载。
- `PlaybackItem` 使用 `Remote(HTTPS)` 与 `AppFile(相对路径)` 两类稳定封面引用；Media3 metadata、MiniPlayer、队列和 Room v4 中的播放快照共享该字段，不持久化短期音频地址。
- Room `2→3` 与完整 `1→2→3` 迁移已在 ELE-AL00 / API 29 的真实 SQLite 上 2/2 通过；既有队列迁移后封面字段保持可空。
- 应用壳 MiniPlayer 已展示真实播放进度、封面及明确播放/暂停和队列语义；子页面隐藏一级导航时会单独处理系统导航栏安全区。
- Search/LocalMusic 截图验证、相关 JVM 单测、Debug 构建与 `MainActivityTest` 真机回归 6/6 通过；真机手动搜索并播放在线歌曲后，MiniPlayer、恢复为暂停状态和队列 Bottom Sheet 均已检查。
- 已在 `06` 至 `08` 号确认设计图之后建立 [`player-flow`](../design/prototypes/player-flow/README.md) 本地交互原型，并验证封面/歌词双页、点击歌词定位、暂无歌词、纵向退出、队列覆盖层和“先关队列、再退出播放器”的返回优先级；原型不定义 Compose 视觉或协议行为。
- 已新增独立 `:feature:player` 全屏封面目的地，按 `06-player-cover.png` 实现封面/占位、标题歌手、进度和核心控制；MiniPlayer 可进入，进入后隐藏一级导航与 MiniPlayer，队列继续复用同层 Bottom Sheet，队列最后一项移除后自动退出空播放器。
- 播放页仅消费 `:app` 映射的不可变 UI 状态和事件，不直接依赖 `:playback`；高频进度由独立 `State` 交给进度子组合读取。标准视口不依赖尺寸分档，只有内容实际溢出时允许纵向滚动。
- 全屏封面已覆盖正常、暂停、缓冲、控制器未连接、封面失败、未知时长、空播放项及 `1.5×`/`2.0×` 字体截图基准；真机导航、手势、队列返回优先级和播放命令仍待用户手动验收。
- 歌词协议、KRC 解包、成熟解析库、成功缓存、取消和失败恢复已完成独立 [`12-kugou-lyrics`](../reference-audits/12-kugou-lyrics.md) 审计；`07-player-lyrics.png` 与 `23a` 至 `23h` 加载、空、离线、错误和大字体状态均已确认，允许在首页与用户链路优先切片完成后实现歌词 Compose。
- Room v4 已新增脱敏键控的 KRC 成功缓存表；`3→4`、完整 `1→4` 与 DAO 覆盖已随本轮数据库回归在用户指定真机通过。
- 首页缓存已将 Room 升至 v5，新增按 `home:v1:anonymous` / `home:v1:user:<userid>` 分区的可观察完整快照 DAO；`4→5`、完整 `1→5`、DAO 覆盖与既有数据库回归已在 ELE-AL00 / API 29 真机 13/13 通过。
- 首页 Repository 已完成 Room stale-while-revalidate、15 分钟 TTL、匿名/用户分区自动切换与刷新、完整快照提交、部分失败保护、single-flight、强制刷新和会话双代际隔离；15 项 JVM 测试覆盖旧缓存首发、自动刷新问题、取消、缓存损坏和并发竞态。
- Home ViewModel 已完成领域模型到 UI Model 的隔离、首次/缓存/刷新/部分/空/错误状态、弱提示消耗、显式刷新代际取消和身份分区切换；9 项 JVM 测试覆盖首次会话失败顺序及账号切换与手动刷新并发，下一切片进入确认稿 Compose 页面。
- 歌词 Repository 已完成仅支持酷狗来源的缓存优先读取、损坏缓存删除后单次回源、同 Hash 并发单飞、旧请求取消透传和成功解析后缓存；匿名歌词客户端不再接受账号请求上下文，JVM 行为测试与 App Hilt 装配已通过。
- `23a` 至 `23h` 歌词状态稿的页面结构和状态表达已确认；稿件颜色只作示例，封面页和歌词页必须共享当前歌曲封面派生的语义色板。动态取色已经完成独立 [`13-player-artwork-palette`](../reference-audits/13-player-artwork-palette.md) 审计，允许先实现调色基础与现有封面页接入。

## 当前剩余

- 首页真实数据与 UI；完成后进入“我的”与登录用户链路。
- 播放器动态色、歌词页、完整队列，以及歌单/专辑/歌手/排行榜详情和对应完整状态矩阵。
- 发现页最后实施，并复用首页已经稳定的数据与组件能力。
- 队列拖拽、封面/歌词切换、AMOLED 内容页面与 `2.0×` 字体真机关键控制验收。
- 真实在线封面缺失/失败占位视觉、TalkBack 顺序、预测返回和复杂队列设备测试。

## 测试

- Route/ViewModel 状态与旧请求取消单测。
- 导航、MiniPlayer 与全屏播放器联动测试。
- 核心页面浅色、深色、纯黑和大字体截图。
- TalkBack 语义、滚动、返回和预测返回设备测试。

## 完成标准

- 主要页面在标准手机上与批准稿的结构、层级和视觉语言一致。
- 搜索不出现在底部导航，播放器不占一级 Tab。
- 大字体不裁切关键控制，错误与加载不只依赖 Toast。
