# 设置与应用偏好审计

状态：Accepted。审计日期：2026-08-07。

## 产品定义

- 入口位于“我的”账户卡片右上角，匿名和已登录用户均可进入；设置是应用级能力，不经过账号门禁。
- 首批成功语义是：用户可在“跟随系统 / 浅色 / 深色 / 纯黑”中选择主题，选择立即作用于整个应用，并在 Activity 重建、进程重启后恢复。
- 首批同时提供只读的“关于 MoeKoe Air”应用信息。确认稿中的主题色、封面动态取色、播放与音质、歌词、缓存和语言分组应先完整保留视觉结构，再随各自消费者按纵向切片接入功能。
- 明确非目标：视觉先行不等于提前写入无消费者的 DataStore 值，也不伪造缓存容量或执行清理；未接能力使用明确的静态/不可提交状态。不重启进程，不新增网络、权限、后台任务、分析或遥测。
- 首次读取期间使用稳定默认值“跟随系统”，不以全屏 Loading 阻塞应用壳；读取失败继续使用该默认值并在设置页显示可恢复错误。写入失败保持最后持久化值，页面给出重试反馈。
- `播放失败时自动跳过` 是首个随既有消费者接入的播放偏好：默认开启以保持当前产品行为。关闭只影响下一次不可恢复播放错误；已经开始的地址刷新和已排队的恢复不取消。开启时继续保留单曲不跳过、最多连续六次失败自动跳过以及成功进入 `READY` 后重置计数的既有规则。
- `封面动态取色` 是第二个已有真实消费者的应用偏好：默认开启。它仅控制全屏播放器是否从**已经成功解码**的当前封面派生 `PlayerPalette`，不改变全局主题、Android Monet、封面请求、数据库或任何其他页面；关闭立即恢复固定色板并取消旧任务，重新开启只使用本次组合中已解码的当前封面。缺失/读取失败安全回退开启，写失败回滚最后持久化值并提供精确重试。
- `翻译与音译` 是第三个已有真实消费者的应用偏好：`showLyricsSupplementalText` 默认开启，旧 PC 的 `lyricsTranslation` 同时控制翻译与音译。V2 只在播放器渲染层隐藏 secondary，保留当前已加载的歌词内容，重新开启立即恢复；不影响歌词请求、解析、缓存、逐字 timing 或点击 seek。读取失败回退开启，写失败回滚最后持久值并精确重试。
- `歌词字体大小` 是第四个已有真实消费者的应用偏好：`lyricsTextSize` 为独立 v1 枚举，默认“标准”；“150%”与“200%”分别复用已确认 `23g` / `23h` 的 Large / Largest 几何。旧 PC 的 `20 / 24 / 32` 只证明小/中/大三档语义，不复制为 Android px。切换只改变播放器既有歌词视窗的字号、行高和内容 offset，不改变已加载文档、请求、解析、缓存、逐字 timing、点击 seek 或翻译与音译的显隐。缺失、读取失败和未知值安全回退“标准”并报告既有读取问题；写失败回滚最后持久值并精确重试。
- `默认音质` 的协议、回退、显示真值与实施边界已由 [`24-default-playback-quality`](24-default-playback-quality.md) Accepted：默认 `128`，匿名固定 `free_part=1` 且不查询候选；登录态只在下一次在线地址解析中使用已保存偏好并向低档回退。设置行显示简短当前值，Dialog 以“选择默认音质”列出 `标准音质 · 128 Kbps`、`高品音质 · 320 Kbps`、`FLAC 无损`、`Hi-Res 无损`、`蝰蛇全景`、`蝰蛇超清`、`蝰蛇母带`。它只写偏好上限，绝不把偏好伪装为当前实际质量。
- `主题色` 的产品真值、六档预设、模块边界与失败恢复已由 [`25-brand-theme-color`](25-brand-theme-color.md) Accepted：默认“天空蓝”精确保留当前 `#1677F2 / #8EC4FF / AMOLED` 基线，且只改变 Material `primary` 角色组（含 on/container/inverse）；secondary / tertiary、Surface、error 和 MoeKoe extra semantic colors 保持固定。它独立于 ThemeMode、系统 Monet 和播放器封面动态色；缺失/未知/读取失败回退天空蓝，写失败回滚最后持久值并精确 Retry，快速选择使用独立 generation，不取消其余设置写入。
- `清理缓存` 的 UI 契约已于 036 建立：它是无容量/历史值的 Action 行，保留 Chevron；点击以同一 Settings 页面为底层、约 32% 黑色遮罩打开 `MoeAlertDialog`。Dialog 明确只清首页/歌词/图片缓存，不删本地音乐、账号或播放记录，并说明后续内容会重新下载；左侧“取消”为 Tonal、右侧“清理”为 Destructive。提交前 Back/遮罩可取消，提交中按钮禁用、主按钮加载且 Dialog 不可关闭；成功只关闭 Dialog 并显示无 action、受控约 4 秒后自动消费的成功 Snackbar，partial/failed 显示带 Retry 的错误 Snackbar，Retry 重跑整次幂等清理，取消不显示失败。任一其他设置写入开始时清除旧缓存反馈，保证可见 Retry 始终对应最后产生该反馈的请求。`CacheLimit` 继续是 Deferred / Unavailable。
- `淡入淡出` 由 [`28-playback-fade`](28-playback-fade.md) 明确为 Deferred：固定 PC/Mobile 没有音频消费者，且曲目开始/结束淡化、暂停/恢复音量 ramp、相邻曲目 crossfade 是不同产品能力。保留现有不可提交行；在用户确认语义、时长、转换/失败边界、gapless、音频焦点和 offload 策略前，不新增偏好或假开关。
- `语言` 由 [`29-app-language`](29-app-language.md) 明确为 Deferred：PC 的六种 locale 概念不等于 Android 已有翻译；V2 没有 `values-xx`、`LocaleConfig` 或 platform locale 所有权，且仍有生产硬编码中文。`09` 的“简体中文”是未来视觉目标，当前 `Unavailable` 是诚实状态；在资源、跨 API lifecycle、系统/应用单一权威和 RTL/翻译 QA 完成前，不新增假选择。

## 平台与系统约束

- minSdk 26、targetSdk 36；主题偏好没有 API 分支、运行时权限、Intent 或后台执行要求。
- [Android Data layer](https://developer.android.com/topic/architecture/data-layer) 明确将 DataStore 作为用户设置的数据源，并要求 UI 通过 Repository 消费而不是直接访问数据源。本项目因此由 `:data` 独占 DataStore，Feature 和应用壳只依赖 `:core:model` 端口。
- [Save UI states](https://developer.android.com/topic/libraries/architecture/saving-states) 区分可保存 UI 状态与长期持久数据；主题属于安装周期内偏好，不使用 `rememberSaveable` 作为权威来源。
- [Compose App bars](https://developer.android.com/develop/ui/compose/components/app-bars) 定义标题、导航图标与 action 的职责；设置页复用 `MoeStandardTopBar` 的居中标题和返回语义。
- DataStore 读写使用协程与 Flow，写入保持串行事务语义。取消向上传播，不转换成普通失败或自动重放。

## 开源参考审计

### Now in Android

- 仓库：`android/nowinandroid`；固定提交：`7d45eae4f8720a0c77f507712ba2437ff974b6ed`；许可证：Apache-2.0。
- 文件：`core/datastore/.../NiaPreferencesDataSource.kt`、`core/datastore-proto/.../user_preferences.proto`、`feature/settings/impl/.../SettingsViewModel.kt`、`SettingsDialog.kt`、`SettingsViewModelTest.kt`。
- 采用：DataStore 是偏好单一事实来源；ViewModel 将 Repository Flow 映射为不可变 UI state；主题选择使用整行可选择语义并测试持久状态映射。
- 不采用：Proto 模块、Dialog 形态、品牌主题矩阵、动态色能力、Google 服务链接及 `api/impl` 双模块。MoeKoe 当前偏好规模适合既有 Preferences DataStore 依赖与独立全屏设置页。
- 仅学习架构和状态策略，不迁移源码。

### Metrolist

- 仓库：`MetrolistGroup/Metrolist`；固定提交：`289ed45d0a429e6e4f19a2a07543b243a0ab2e8b`；许可证：GPL-3.0。
- 文件：`app/src/main/kotlin/com/metrolist/music/ui/screens/settings/SettingsScreen.kt`、`AppearanceSettings.kt`、`ui/component/Material3SettingsGroup.kt`。
- 采用：音乐设置按能力分组、使用单一纵向滚动所有者；布尔值和枚举值采用不同交互；平台不支持的能力不应伪装成可用。
- 不采用：单一大型 `app` 模块、Composable 直接读取偏好、庞大常量矩阵、SharedPreferences 旁路、重启进程、YouTube/集成服务及项目专属组件。GPL 源码不复制。

## 决策点

### 偏好存储与错误语义

- 当前实现：`MainActivity` 以 `rememberSaveable(ThemeMode.System)` 保存临时主题，重启进程即丢失，设置页不存在。
- 候选：继续使用 Activity 状态；Feature 直连 Preferences DataStore；以 Repository 包装独立 Preferences DataStore。
- 决策：采用 Repository。`:core:model` 定义不依赖 Compose 的 `AppThemePreference`、`AppSettings`、`AppSettingsRepository` 与类型化更新结果；`:data` 使用独立文件实现。
- 理由：Activity 临时状态不满足恢复语义；Feature 直连 DataStore 违反数据层门禁；Repository 可被 app 和设置 Feature 共同消费并提供测试替身。
- 验收：进程重建恢复主题；损坏/IOException 回退 System 且可观察；写失败不乐观提交错误值；不输出偏好内容日志。

### 模块、导航与应用主题消费

- 当前实现：`:feature:my` 的齿轮实际打开退出菜单，语义错误；`:app` 直接持有临时主题状态。
- 候选：把设置塞进 `:feature:my`；在 `:app` 写页面；新增 `:feature:settings`。
- 决策：新增 `:feature:settings`，拥有 Destination、Route、Screen、ViewModel、strings 与测试。`:feature:my` 只上抛 `onSettings`；`:app` 注册目的地并以独立 app-level ViewModel 消费全局主题。
- 理由：设置有独立数据与页面变化原因；Feature 不依赖 app 或其他 Feature；组合根只负责装配。
- 验收：匿名/登录态齿轮进入唯一 Settings destination；Back 返回原“我的”栈；设置页不显示底部导航和 MiniPlayer；退出账号仍使用明确的账号菜单动作而非设置齿轮。

### 渐进功能与确认稿完整度

- 当前实现：`09-settings.png` 已确认，但绝大多数设置尚无真实消费链。
- 候选：一次性持久化全部视觉选项；全部显示为禁用；只渲染已工作的分组。
- 决策：确认稿中的全部分组与 Item 先按视觉稿实现；已工作的主题与关于保持真实交互，其余能力在消费者接入前使用一致的静态/不可提交状态，示例值不进入业务真值。
- 理由：设计确认与功能开发是两个阶段；隐藏未接能力会让实际页面偏离已确认稿，但伪造持久化或可点击成功语义同样不可接受。
- 验收：页面视觉结构完整，不显示假缓存容量，不把未消费开关写入 DataStore；后续每个功能切片再补消费者、失败恢复和测试。

### 播放失败自动跳过

- 当前实现：`:playback` 已在不可恢复的播放器错误后按连续失败策略自动跳到下一项，但用户无法控制该行为；设置页只显示不可提交的静态行。
- 候选：继续固定自动跳过；由设置 Feature 直接操作 Service；通过 `AppSettingsRepository` 让设置与 Service 消费同一个持久化偏好。
- 决策：新增非敏感 `autoSkipFailedPlayback` 布尔偏好，默认 `true`。`:feature:settings` 只经 Repository 写入并展示持久化状态；`:playback` 在 Service 生命周期内观察 Repository 的快照，并只在下一次不可恢复错误发生时读取当时值。Feature 不依赖 Service，Service 不访问 DataStore。
- 失败恢复：读取缺失、损坏或 `IOException` 时回退 `true`，避免意外关闭既有保护；写失败保留最后持久化值并在设置页提供重试。关闭时暂停当前项并保留现有错误反馈，不消耗连续失败预算；不取消已启动的一次地址刷新或已经排队的下一首恢复。
- 验收：开关状态经进程重建恢复；开启保留单曲不跳过和六次上限；关闭后下一次不可恢复错误暂停；运行中切换只作用于下一次错误；快速主题与自动跳过写入彼此不取消。

## 视觉设计门禁与适配契约

- 权威设计：[`../design/mockups/09-settings.png`](../design/mockups/09-settings.png)，已确认并于 2026-08-07 使用标准 Toolbar、登录输入框图标规范和长画布规则修订。首批仅裁取其真实能力分组，不改变 Item 视觉语言。
- 正式基准文件实测为 `853 × 2172px`、简体中文、浅色、`1.0×` 字体；设计坐标按当前 Composable 内容宽度等比归一化，触控区仍满足最小 `48dp`，文字使用主题 `sp`，不按原图 px 硬编码。
- Toolbar 固定在页面顶部并消费状态栏 Insets；使用 `MoeStandardTopBar`，高度、对称槽位、居中标题和返回图标由 Design System 所有。
- 首个设置分组紧接 Toolbar，不叠加顶部 Content Padding；列表仅保留水平边距、分组间距和底部滚动留白。
- 内容是单一 `LazyColumn`/纵向滚动所有者。分组宽度随 Compact 容器伸缩，Item 高度由内容和最小触控区决定，不固定整页高度，也不把长画布压缩到单屏。
- Compact：单列；Medium/Expanded：仍保持居中单列并限制可读宽度，不擅自改双栏。横屏/短高度允许同一列表自然滚动。
- `1.5×/2.0×` 字体时 Item 的标题与当前值允许纵向增高或换行；开关/箭头保持尾端对齐，不裁切文字。字体恢复后由布局自然复位，无独立滚动偏移规则。
- 标准视口内容超过可用高度时固有滚动；初始位置为顶部。Back 不保存业务草稿，Navigation 的页面栈恢复可保留列表位置。
- 无 IME。浅色、深色、AMOLED、`1.5×`、`2.0×` 建立实现回归截图；设计符合度以 Toolbar 中线、外边距、分组圆角、首项锚点和 Item 最小高度为锚点，网络图片/动态系统栏区域不参与差异。

## 技术设计

- 不新增第三方依赖；复用现有 AndroidX DataStore、Hilt、Coroutines、Lifecycle、Navigation Compose、Material 3 与截图插件，许可证均为 Apache-2.0。
- 依赖方向：`:feature:settings -> :core:model + :core:designsystem`；`:data -> :core:model`；`:playback -> :core:model`；`:app -> :feature:settings + :data`。`:feature:settings` 不依赖 `:feature:my`、`:app` 或 `:playback`。
- 独立 Preferences DataStore 文件仅保存非敏感应用偏好，不与加密酷狗会话 DataStore 共用文件或 qualifier。
- 主数据流：DataStore `Flow<Preferences>` → data Repository → app Theme ViewModel / Settings ViewModel / Playback Service → 不可变 StateFlow 或运行时策略。事件反向调用 ViewModel，再由 Repository 单次更新。
- 读取错误映射为带默认设置的 `AppSettingsSnapshot` 和类型化 Storage 问题；写入返回成功/失败。UI 写入期间禁用当前提交，失败后保留旧值并提供重试；不做无条件自动重试。
- 多次快速选择按 ViewModel 代际串行，旧写入结果不能覆盖较新的 UI 状态；DataStore 中最终值是唯一事实来源。

## 原子实施顺序

1. 领域端口、`:data` Preferences DataStore、错误与 Repository JVM 测试。已完成。
2. app-level 主题消费，替换 `MainActivity` 临时状态，验证重建恢复。已完成。
3. `:feature:settings` 主题/关于页面、截图和导航目的地。已完成确认稿五个分组、14 个 Item、完整长页面与六组截图；未接能力保持静态事件边界。
4. “我的”匿名/登录态齿轮接入 Settings，退出动作改为明确账号菜单入口。已完成。
5. 播放失败自动跳过：复用既有连续失败策略，把 `SkipFailed` 接入真实 Toggle、持久化和 Service 观察；不改变地址刷新或播放控制器所有权。已完成。
6. 封面动态取色：把 `DynamicColor` 接入真实 Toggle、持久化和全屏播放器派生色板；不改变全局系统动态主题、封面请求或歌词数据链路。已完成。
7. “翻译与音译”：把 `Translation` 接入真实 Toggle、独立持久化与 Player 渲染层；关闭只隐藏 secondary，不重新读取歌词。已完成。
8. “歌词字体大小”：把 `LyricsFontSize` 接入真实选择行、统一 Radio Dialog、独立枚举持久化与 Player 纯字号输入；不把字号写入歌词文档。已完成。
9. “默认音质”：core 仅含七档语义 enum，data 私有映射稳定 storage value；`AppSettings` Repository setter、独立 v1 DataStore key、登录态候选回退、`KugouPlaybackQuality` 映射与真实 resolved-quality 运行时状态已完成。设置行现已接入真实七档选择 Dialog，并拥有独立保存代际、失败回滚最后持久值和精确 Retry；Player 与 MiniPlayer 现仅从 resolved runtime quality 显示实际角标，null（本地、演示、未解析或错误）不显示。它仍只改变下一次在线地址解析的偏好上限；真实服务和真机验证继续独立。
10. “主题色”：领域/DataStore、六档 Design System primary 角色变体、app 组合根和 Settings 选择 Dialog 的代码/语义已于 030-032 完成；默认天空蓝不得改变既有 Light/Dark/AMOLED 基线，真实消费者是全局 Material primary 角色组，不把色值下沉到页面或播放器。032 已受限更新主题色行及五种非蓝/主题色 Dialog 截图基线，最终视觉与治理验收仍待完成。
11. “清理缓存”：036 已完成确认/进行中/结果反馈的设计契约；037 已接入已完成的 `CacheMaintenanceRepository`，补齐独立 saving/retry、Dialog、Snackbar、JVM/AndroidTest 编译、两张确认 Dialog reference、截图/contract/fidelity/治理验收。既有 PNG 未改动；指定真机与真实 Coil 生命周期仍未验证。`CacheLimit` 仍 Deferred，不随清理能力改为可点。
12. “淡入淡出”：Deferred；保持 `Fade` 不可提交，等待独立 A/B/C 产品选择和对应 Player/Settings 设计状态确认。
13. “语言”：Deferred；保持 `Language` 不可提交，等待完整 locale 资源、系统/API 生命周期与 Settings 状态准入。
14. 后续其余播放能力分别在真实消费者完成时增加对应设置 Item。

每个切片独立提交、推送并恢复干净工作区。

## 测试与验收

- JVM：默认值、四种主题映射、自动跳过/动态取色/翻译与音译布尔值及歌词三档字号持久化、读取 IOException/未知枚举、写入失败、取消透传、快速连续写入代际、ViewModel 不乐观覆盖和精确重试；开启/关闭、单曲、连续六次失败与下一次错误生效的播放失败策略。
- 集成：Activity 重建与进程重启恢复；Settings → Back 返回 My；匿名与登录态均可进入；退出确认仍可达。匿名态 My → Settings → 主题切换 → Back 已在指定 ELE-AL00 / API 29 真机通过。
- Compose：整行选择/Toggle 语义、选中状态、写入中禁用、错误与重试、Back content description。
- 截图：390 × 844 浅色/深色/AMOLED、`1.5×`、`2.0×`；另做确认稿锚点对比，不以重录回归图替代设计符合度。
- 真机：仅使用用户指定且已连接的 ELE-AL00 / API 29，验证四种主题即时切换、Activity 重建、返回栈、列表滚动和大字体；不创建模拟器。
- 工程：受影响模块 compile、unit test、lint、screenshot validation、`spotlessCheck` 与 `:app:assembleDebug` 全部通过。

以上产品、平台、所有权、失败恢复、视觉适配和测试决策均已确定，首批状态由已确认设计覆盖，可开始实现。
