# 阶段 5A：内容与播放器 UI

状态：进行中。音乐内容组件、搜索/本地列表、MiniPlayer、全屏封面/歌词和队列纯 UI 已完成；动态色、歌词业务和其余内容详情仍待实现。

## 目标

按照已批准视觉稿实现主要内容浏览和播放体验，不重新解释信息架构。

当前界面开发顺序按已确认稿固定为“我的首页 → 通用播放组件 → 首页 → 发现页 → 播放页”；页面视觉先完整落地，对应业务能力再按纵向切片接入，不能因为功能尚未完成而删减已确认视觉结构。

首页真实内容门禁见 [`../reference-audits/15-home-content-and-cache.md`](../reference-audits/15-home-content-and-cache.md)，状态已 Accepted。实现顺序固定为三个首页 Endpoint 与 Decoder、Room v5 完整快照、cache-first Repository、ViewModel、确认稿 Compose；前四个切片使用固定测试、迁移测试和自动截图/真机测试验收，不要求用户手动操作。排行榜与新歌不进入首页首批切片。发现页视觉按 [`../design/DISCOVER_LAYOUT_SPEC.md`](../design/DISCOVER_LAYOUT_SPEC.md) 先完整落地，业务能力随后按纵向切片接入。

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

## 当前结果（2026-08-08）

- `:core:designsystem` 已实现 Section Header、封面容器、媒体徽标、歌曲行、MiniPlayer 和队列行，并建立浅色、深色与 `1.5×` 字体截图基准。
- Search 与 LocalMusic 已移除各自的重复歌曲行布局，统一使用 `MoeSongRow`；在线封面和 App 专属目录本地封面继续由 Coil 3 加载。
- `PlaybackItem` 使用 `Remote(HTTPS)` 与 `AppFile(相对路径)` 两类稳定封面引用；Media3 metadata、MiniPlayer、队列和 Room v4 中的播放快照共享该字段，不持久化短期音频地址。
- Room `2→3` 与完整 `1→2→3` 迁移已在 ELE-AL00 / API 29 的真实 SQLite 上 2/2 通过；既有队列迁移后封面字段保持可空。
- 应用壳 MiniPlayer 已展示真实播放进度、封面及明确播放/暂停和队列语义；子页面隐藏一级导航时会单独处理系统导航栏安全区。
- 通用 MiniPlayer 已按 `17-music-content-components.png` 与 `01-home-material3-v2.png` 的页面壳层完成同视口视觉校准：保留连续底部 Surface，校正封面、紧凑字级、质量徽标、上一首、主色圆形播放暂停、下一首、队列、起止时间与独立进度条；上一首/下一首继续接入既有播放命令，本次不改 `:playback` 状态所有权。浅色、深色、`1.5×` 聚焦截图与设计并排/叠加/差异证据见 [`mini-player-2026-08-08.md`](../design/evidence/mini-player-2026-08-08.md)，四项操作语义区域继续保持不小于 `48dp`。
- Search/LocalMusic 截图验证、相关 JVM 单测、Debug 构建与 `MainActivityTest` 真机回归 6/6 通过；真机手动搜索并播放在线歌曲后，MiniPlayer、恢复为暂停状态和队列 Bottom Sheet 均已检查。
- 已在 `06` 至 `08` 号确认设计图之后建立 [`player-flow`](../design/prototypes/player-flow/README.md) 本地交互原型，并验证封面/歌词双页、点击歌词定位、暂无歌词、纵向退出、队列覆盖层和“先关队列、再退出播放器”的返回优先级；原型不定义 Compose 视觉或协议行为。
- 独立 `:feature:player` 全屏封面目的地已按 [`PLAYER_LAYOUT_SPEC.md`](../design/PLAYER_LAYOUT_SPEC.md) 和 `06-player-cover.png` 完成视觉复刻：补齐更多、分页点、品质、收藏、下载、加歌单、分享及独立队列动作，封面改为真实本地预览位图，播放/暂停、进度、上下首、模式和队列继续复用既有接线；其余次级动作只保留视觉语义。9 组截图、设计对照证据及 ELE-AL00 / API 29 App instrumentation 24/24 通过。
- 播放页仅消费 `:app` 映射的不可变 UI 状态和事件，不直接依赖 `:playback`；高频进度由独立 `State` 交给进度子组合读取。标准视口不依赖尺寸分档，只有内容实际溢出时允许纵向滚动。
- 全屏封面已覆盖正常、暂停、缓冲、控制器未连接、封面失败、未知时长、空播放项及 `1.5×`/`2.0×` 字体截图基准；真机导航、手势、队列返回优先级和播放命令仍待用户手动验收。
- 歌词协议、KRC 解包、成熟解析库、成功缓存、取消和失败恢复已完成独立 [`12-kugou-lyrics`](../reference-audits/12-kugou-lyrics.md) 审计；`07-player-lyrics.png` 与 `23a` 至 `23h` 加载、空、离线、错误和大字体状态均已确认，并已形成独立 [`PLAYER_LYRICS_LAYOUT_SPEC.md`](../design/PLAYER_LYRICS_LAYOUT_SPEC.md) 视觉合同。歌词 Compose 先按全部确认稿完成纯 UI 和截图，再单独接入歌词状态与交互。
- 全屏歌词页纯 UI 已按 [`PLAYER_LYRICS_LAYOUT_SPEC.md`](../design/PLAYER_LYRICS_LAYOUT_SPEC.md)、`07-player-lyrics.png` 与 `23a` 至 `23h` 完成复刻：封面/歌词使用真实双页 Pager，歌词页复用顶部栏、歌曲信息、进度和核心播放控制；加载、空、离线、错误、原文、翻译、音译及歌词 `150%` / `200%` 显示档位均有独立截图和同画布对照，指定 ELE-AL00 / API 29 App instrumentation 24/24 通过。真实歌词加载、缓存、同步与点击 seek 保持为后续业务切片。
- Room v4 已新增脱敏键控的 KRC 成功缓存表；`3→4`、完整 `1→4` 与 DAO 覆盖已随本轮数据库回归在用户指定真机通过。
- 首页缓存已将 Room 升至 v5，新增按 `home:v1:anonymous` / `home:v1:user:<userid>` 分区的可观察完整快照 DAO；`4→5`、完整 `1→5`、DAO 覆盖与既有数据库回归已在 ELE-AL00 / API 29 真机 13/13 通过。
- 首页 Repository 已完成 Room stale-while-revalidate、15 分钟 TTL、匿名/用户分区自动切换与刷新、完整快照提交、部分失败保护、single-flight、强制刷新和会话双代际隔离；15 项 JVM 测试覆盖旧缓存首发、自动刷新问题、取消、缓存损坏和并发竞态。
- Home ViewModel 已完成领域模型到 UI Model 的隔离、首次/缓存/刷新/部分/空/错误状态、弱提示消耗、显式刷新代际取消和身份分区切换；9 项 JVM 测试覆盖首次会话失败顺序及账号切换与手动刷新并发。
- 首页 Compose 已按 [`HOME_LAYOUT_SPEC.md`](../design/HOME_LAYOUT_SPEC.md) 和 `01-home-material3-v2.png` 完整恢复横向顶部工具区、Radio Hero、三快捷入口、每日推荐四行与四列推荐歌单；2026-08-08 的二次同画布复核进一步修正了旧证据未拦住的歌曲行、封面、字级与推荐歌单纵向漂移。真实推荐/歌单、搜索、下拉刷新、播放事件和缓存弱提示继续复用既有链路。Light、Dark、AMOLED、加载、空、错误、缓存刷新弱提示、`1.5×`、`2.0×` 共 9 组截图基线及归一化对照证据已建立；设计示例徽标和副标题只进入截图 fixture，不扩充协议或伪造运行时能力。
- 发现页已按 [`DISCOVER_LAYOUT_SPEC.md`](../design/DISCOVER_LAYOUT_SPEC.md) 和 `02-discover-v2.png` 完整恢复五段 Tab、本周新声 Hero、三列热门榜单、分类胶囊与三列封面；2026-08-08 的二次同画布复核进一步修正了 Tab 基线、Hero 内容、排行榜起点、胶囊宽度与分类封面的局部漂移。当前内容是 UI 层设计预览，不声明榜单、详情或播放业务已接入。Light、Dark、AMOLED、加载、空、错误、`1.5×`、`2.0×` 共 8 组截图基线和归一化设计对照证据已建立，大字体排行榜按纵向行重排。
- 播放队列已按 [`PLAYER_QUEUE_LAYOUT_SPEC.md`](../design/PLAYER_QUEUE_LAYOUT_SPEC.md) 和 `08-player-queue.png` 完成纯 UI 复刻：系统 `ModalBottomSheet` 内恢复标题、真实数量、播放模式、清空、来源、当前项、六行密度和关闭提示；标准、空队列、`1.5×` 共 3 组截图及归一化对照证据通过。指定 ELE-AL00 / API 29 已从 MiniPlayer 与全屏播放器分别打开真实调试队列并验证系统返回；拖拽仅表达确认稿视觉，不伪装重排业务已完成。
- 歌单详情已经完成 [`19-playlist-detail-ui`](../reference-audits/19-playlist-detail-ui.md) 门禁，并按 [`PLAYLIST_DETAIL_LAYOUT_SPEC.md`](../design/PLAYLIST_DETAIL_LAYOUT_SPEC.md) 与 `05-playlist-detail.png` 落入独立 `:feature:playlist`：从发现页使用类型安全子页面导航进入，恢复标准 Toolbar、资料 Hero、五项操作、紧凑歌曲列表与当前项 Surface；子页面隐藏一级底栏并保留应用级 MiniPlayer。Light、Dark、AMOLED、加载、空、错误、`1.5×`、`2.0×` 共 8 组截图基线及归一化设计对照证据已建立，指定 ELE-AL00 / API 29 已验证入口、返回与窄屏操作区无裁切。真实协议、收藏、下载、排序和播放行为继续拆分为后续纵向切片。
- 歌词 Repository 已完成仅支持酷狗来源的缓存优先读取、损坏缓存删除后单次回源、同 Hash 并发单飞、旧请求取消透传和成功解析后缓存；匿名歌词客户端不再接受账号请求上下文，JVM 行为测试与 App Hilt 装配已通过。
- `23a` 至 `23h` 歌词状态稿的页面结构和状态表达已确认；稿件颜色只作示例，封面页和歌词页必须共享当前歌曲封面派生的语义色板。动态取色已经完成独立 [`13-player-artwork-palette`](../reference-audits/13-player-artwork-palette.md) 审计，允许先实现调色基础与现有封面页接入。

## 当前剩余

- 播放器动态色、歌词逐行交互、队列重排业务，以及歌单/专辑/歌手/排行榜详情和对应完整状态矩阵。
- 发现页后续真实榜单、歌单与播放纵向切片复用首页已经稳定的数据与组件能力。
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
