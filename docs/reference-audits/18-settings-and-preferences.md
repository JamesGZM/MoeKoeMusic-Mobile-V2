# 设置与应用偏好审计

状态：Accepted。审计日期：2026-08-07。

## 产品定义

- 入口位于“我的”账户卡片右上角，匿名和已登录用户均可进入；设置是应用级能力，不经过账号门禁。
- 首批成功语义是：用户可在“跟随系统 / 浅色 / 深色 / 纯黑”中选择主题，选择立即作用于整个应用，并在 Activity 重建、进程重启后恢复。
- 首批同时提供只读的“关于 MoeKoe Air”应用信息。确认稿中的主题色、封面动态取色、播放与音质、歌词、缓存和语言分组应先完整保留视觉结构，再随各自消费者按纵向切片接入功能。
- 明确非目标：视觉先行不等于提前写入无消费者的 DataStore 值，也不伪造缓存容量或执行清理；未接能力使用明确的静态/不可提交状态。不重启进程，不新增网络、权限、后台任务、分析或遥测。
- 首次读取期间使用稳定默认值“跟随系统”，不以全屏 Loading 阻塞应用壳；读取失败继续使用该默认值并在设置页显示可恢复错误。写入失败保持最后持久化值，页面给出重试反馈。

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
- 依赖方向：`:feature:settings -> :core:model + :core:designsystem`；`:data -> :core:model`；`:app -> :feature:settings + :data`。`:feature:settings` 不依赖 `:feature:my` 或 `:app`。
- 独立 Preferences DataStore 文件仅保存非敏感应用偏好，不与加密酷狗会话 DataStore 共用文件或 qualifier。
- 主数据流：DataStore `Flow<Preferences>` → data Repository → app Theme ViewModel / Settings ViewModel → 不可变 StateFlow → Compose。事件反向调用 ViewModel，再由 Repository 单次更新。
- 读取错误映射为带默认设置的 `AppSettingsSnapshot` 和类型化 Storage 问题；写入返回成功/失败。UI 写入期间禁用当前提交，失败后保留旧值并提供重试；不做无条件自动重试。
- 多次快速选择按 ViewModel 代际串行，旧写入结果不能覆盖较新的 UI 状态；DataStore 中最终值是唯一事实来源。

## 原子实施顺序

1. 领域端口、`:data` Preferences DataStore、错误与 Repository JVM 测试。已完成。
2. app-level 主题消费，替换 `MainActivity` 临时状态，验证重建恢复。已完成。
3. `:feature:settings` 主题/关于页面、截图和导航目的地。已完成确认稿五个分组、14 个 Item、完整长页面与六组截图；未接能力保持静态事件边界。
4. “我的”匿名/登录态齿轮接入 Settings，退出动作改为明确账号菜单入口。已完成。
5. 后续播放、歌词、缓存能力分别在真实消费者完成时增加对应设置 Item。

每个切片独立提交、推送并恢复干净工作区。

## 测试与验收

- JVM：默认值、四种主题映射、持久化、读取 IOException、写入失败、快速连续写入代际、ViewModel 不乐观覆盖。
- 集成：Activity 重建与进程重启恢复；Settings → Back 返回 My；匿名与登录态均可进入；退出确认仍可达。匿名态 My → Settings → 主题切换 → Back 已在指定 ELE-AL00 / API 29 真机通过。
- Compose：整行选择语义、选中状态、写入中禁用、错误与重试、Back content description。
- 截图：390 × 844 浅色/深色/AMOLED、`1.5×`、`2.0×`；另做确认稿锚点对比，不以重录回归图替代设计符合度。
- 真机：仅使用用户指定且已连接的 ELE-AL00 / API 29，验证四种主题即时切换、Activity 重建、返回栈、列表滚动和大字体；不创建模拟器。
- 工程：受影响模块 compile、unit test、lint、screenshot validation、`spotlessCheck` 与 `:app:assembleDebug` 全部通过。

以上产品、平台、所有权、失败恢复、视觉适配和测试决策均已确定，首批状态由已确认设计覆盖，可开始实现。
