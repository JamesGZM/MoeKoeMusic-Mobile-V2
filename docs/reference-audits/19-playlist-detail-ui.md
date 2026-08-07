# 歌单详情纯 UI 与 Feature 所有权审计

状态：Accepted。审计日期：2026-08-08。

## 产品定义

本切片把已确认的 [`05-playlist-detail.png`](../design/mockups/05-playlist-detail.png) 落为 Android 原生歌单详情页面，并允许从当前已完成的内容页面进入、返回原来源。可观察的完成语义是：标准 Toolbar、歌单资料、播放操作、歌曲标题/排序和歌曲列表完整可见；页面滚动、系统返回、MiniPlayer 避让和大字体状态可自动验证。

本切片只交付 UI 状态和明确事件边界。歌单详情 Endpoint、Repository、收藏/下载、排序、分享、歌曲菜单和播放全部/随机播放的业务结果均为后续纵向切片；不得以设计示例 ID、歌曲 Hash、收藏状态或假网络响应冒充业务已经接入。Screen 可以发出具名事件，组合根未接入的事件不改变任何业务状态。

适用状态为正常内容、首次加载、空歌单、持续错误、离线和大字体。取消与恢复使用系统返回栈；权限、IME、账号受限在首个只读 UI 切片不适用。正常内容由截图 fixture 证明，运行时若暂时使用设计预览入口，必须明确属于当前已登记的 UI 预览阶段，不写入 Repository、数据库或播放队列。

## Android 与既有工程约束

- minSdk 26、targetSdk 36；不新增权限、Intent、后台任务、持久化、网络请求或依赖。
- 使用类型安全 Navigation Compose；详情是子页面，不进入三个一级 Tab。系统返回只 `popBackStack()`，不写死来源。
- 页面隐藏一级底部导航，但保留应用壳 MiniPlayer；MiniPlayer 与系统导航栏不进入 Feature 的页面测量。
- `:app` 只注册目的地和连接跨模块事件；页面 UI、状态模型、导航键、Screen 和截图归新的 `:feature:playlist`。
- Compose 只消费不可变 UI Model 与事件，不依赖 `:playback`、Repository、数据库或网络。

## 决策点：独立 Feature 与导航所有权

### Android 官方基线

- [Guide to Android app modularization](https://developer.android.com/topic/modularization)：按高内聚业务能力建立模块，保持 App 组合根到 Feature 的单向依赖。
- [Type-safe Navigation Compose](https://developer.android.com/guide/navigation/design/type-safety)：目的地使用可序列化类型，参数只传稳定标识，不传完整业务对象。
- [UI layer architecture](https://developer.android.com/topic/architecture/ui-layer)：Screen 以不可变状态和事件工作，不直接访问数据源。

### Now in Android

- 仓库：`android/nowinandroid`
- 固定提交：`7d45eae4f8720a0c77f507712ba2437ff974b6ed`
- 许可证：Apache-2.0。
- 文件：`docs/ModularizationLearningJourney.md`、`app/src/main/kotlin/com/google/samples/apps/nowinandroid/ui/NiaApp.kt`、`feature/search/api/.../SearchNavKey.kt`、`feature/search/impl/.../navigation/SearchEntryProvider.kt`。

采用：Feature 拥有导航键、页面和测试，App 只组合入口；实现默认收紧可见性。拒绝：当前只有一个 App，不机械拆成 `api/impl` 双模块，也不迁移到 Navigation 3。

### Compose Samples

- 仓库：`android/compose-samples`
- 固定提交：`84788c81186acd5bf0d280100992c8a9c04120ad`
- 许可证：Apache-2.0。
- 文件：`Jetsnack/app/src/main/java/com/example/jetsnack/ui/components/`、`JetNews/app/src/main/java/com/example/jetnews/ui/JetnewsApp.kt`。

采用：页面内容与应用级导航/壳层分离，列表使用稳定 key，产品组件保持窄职责。拒绝：不复制其视觉、导航结构、示例数据或页面实现；歌单详情几何只由 MoeKoe 确认稿决定。

### 决定

建立单一 `:feature:playlist` 模块，不放入 `:feature:home`、`:feature:discover` 或 `:feature:my`。歌单详情将由多个来源复用，把它归入任一来源 Feature 会制造 Feature 间反向依赖。当前不建立 Repository、ViewModel、Hilt Module 或 `api/impl`；首个纯 UI Screen 只定义 UI Model、事件与类型化目的地。真实数据接入时在本 Feature 内补 ViewModel，并通过 `:core:model` 的稳定 Repository 端口取数。

## 视觉门禁

- 正常内容：`05-playlist-detail.png`，已确认；完整适配规则见 [`../design/PLAYLIST_DETAIL_LAYOUT_SPEC.md`](../design/PLAYLIST_DETAIL_LAYOUT_SPEC.md)。
- 加载、空、错误、离线：复用已确认 `18-mobile-states-overlays.png` 的页面状态语言，不发明新插画或新色板。
- Toolbar：复用当前标准 `MoeToolbar`，视觉以 `10-user-profile.png` 和 `15-toolbar-navigation.png` 为准；`05` 中的返回、搜索、更多决定槽位和标题。
- 音乐内容：复用已确认 `17-music-content-components.png` 的封面、徽标和连续歌曲行语义；详情页专属 Hero、操作排布和设计坐标留在 Feature。
- 设计图示例账号、VIP、曲目、时长、HQ/MV 和封面只作为截图 fixture，不反推协议字段或业务常量。

## 测试矩阵

- Screenshot：Light、Dark、AMOLED、加载、空、错误、`1.5×`、`2.0×`；正常 Light 与确认稿裁去应用壳 MiniPlayer 后归一化并排、叠加和差异检查。
- Compose：返回、搜索、更多、播放全部、随机播放、收藏、下载、歌曲点击/菜单均发出正确事件；标准视口 Hero 起始锚点与列表滚动边界稳定。
- App instrumentation：从现有内容入口进入详情，一级底栏隐藏、MiniPlayer 保留，系统返回回到真实来源；只使用指定 ELE-AL00 / API 29。
- 工程：格式、`:feature:playlist` 截图验证、单测、Lint、Debug AndroidTest 编译与 APK 构建。

## 结论

产品范围、视觉基线、模块所有权、导航边界、非目标和自动验收均无关键待定项。允许先实现完整纯 UI 与事件端口，后续再单独审计并接入歌单协议和业务行为；本结论不授权新增网络、持久化、权限或伪造播放结果。
