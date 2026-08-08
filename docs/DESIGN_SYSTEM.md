# MoeKoe Air Design System

状态：第一版视觉方向，以及 `09`、`14` 至 `18` 的修订稿均已确认。初次确认日期：2026-08-05；本轮修订确认日期：2026-08-07。

本文是颜色语义、交互状态、无障碍下限和通用组件行为的真值。已确认页面设计图是该页面结构、坐标、视觉尺寸比例、间距、圆角、层级和裁切的真值；页面专属几何必须按页面适配契约测量和映射，不能用通用 Token 或实现者经验覆盖。设计稿中的自动生成文字、日期和业务数据仍不是真值。

公共组件的当前清单见 [`UI_COMPONENT_CATALOG.md`](UI_COMPONENT_CATALOG.md)，准入、所有权与迁移依据见 [`reference-audits/17-design-system-components.md`](reference-audits/17-design-system-components.md)。

当前已确认的标准 Toolbar 视觉基准为 [`10-user-profile.png`](design/mockups/10-user-profile.png)，设置与通用组件正式基线为 `09`、`14` 至 `18`：

- [`09-settings.png`](design/mockups/09-settings.png)
- [`14-design-foundations.png`](design/mockups/14-design-foundations.png)
- [`15-toolbar-navigation.png`](design/mockups/15-toolbar-navigation.png)
- [`16-actions-inputs.png`](design/mockups/16-actions-inputs.png)
- [`17-music-content-components.png`](design/mockups/17-music-content-components.png)
- [`18-mobile-states-overlays.png`](design/mockups/18-mobile-states-overlays.png)

## 设计原则

- Material 3 提供交互语义、状态和无障碍基础，MoeKoe Air 提供颜色、插画、信息密度与音乐产品识别。
- 内容页面以暖白、天空蓝和清透表面为主；蓝色是操作与品牌强调色，不大面积堆叠淡蓝卡片。
- 插画、封面和头像承担主要视觉色彩，界面容器保持克制。
- 歌曲和资产列表主要依靠排版、留白与分隔线建立层级，不将每一项包装成卡片。
- 所有用户可见文案必须资源化；布局为后续国际化至少预留约 40% 的横向扩展空间。
- 所有组件必须验证浅色、深色、AMOLED、`1.0×`、`1.5×` 和 `2.0×` 字体。
- Filled 主按钮禁用态沿用登录 `19a` 的低饱和浅蓝层级：使用约 `35%` 主色容器与弱化 `onPrimary` 内容，不回退为中性灰块；加载态仍保持完整主色，并同时通过进度指示表达不可交互。
- 公共组件按真实复用判断：至少两个页面具有相同语义、结构和交互，或存在必须全局统一的行为约束；视觉相似、代码相似和一次性页面组合都不是抽取理由。
- `:core:designsystem` 按 `action`、`input`、`navigation`、`overlay`、`feedback`、`state` 和 `media` 职责组织；公共 API 不接收业务状态或页面布局规格。

## 页面设计稿与 Token 的关系

- 已确认页面先建立设计坐标空间，再把坐标、间距、圆角、图标和组件视觉尺寸用一个统一系数映射到当前容器；禁止逐组件挑选近似 Token 后改变整页比例。
- 通用 Token 继续约束颜色语义、控件状态、最小触控区和没有页面专属设计尺寸的共享组件。页面设计值与共享 Token 不一致时，先在页面规格记录差异，不得静默修改设计或全局 Token。
- 设计单位不是 Android 原始像素。Compose 中使用当前约束转换为 `Dp`/`Sp`；不得按物理分辨率或设备型号选择尺寸。
- `fontScale = 1.0` 时文字几何匹配设计；系统大字体使用 `sp` 并允许增高、换行和滚动。可见组件按设计缩放，语义点击区仍满足最小 `48dp`。
- 登录页的完整规则见 [`design/LOGIN_LAYOUT_SPEC.md`](design/LOGIN_LAYOUT_SPEC.md)；其他核心页面编码前按同一模板补齐页面级契约。

## 颜色

### 浅色主题

| Token | 色值 | 用途 |
| --- | --- | --- |
| `primary` | `#1677F2` | 品牌、主要操作、选中状态 |
| `onPrimary` | `#FFFFFF` | 主色容器上的内容 |
| `primaryContainer` | `#E8F2FF` | 选中项、柔和操作容器 |
| `onPrimaryContainer` | `#0B3B79` | 主色浅容器上的内容 |
| `background` | `#FBFCFE` | 页面背景 |
| `onBackground` | `#1A1C20` | 页面主要文字 |
| `surface` | `#FFFFFF` | 标准表面 |
| `onSurface` | `#1A1C20` | 表面主要文字 |
| `surfaceContainer` | `#F5F8FC` | 分组容器、导航和弱层级 |
| `onSurfaceVariant` | `#6E7580` | 次要文字与图标 |
| `outline` | `#BFC7D2` | 强调边框 |
| `outlineVariant` | `#E1E7EF` | 分隔线和轻描边 |

扩展语义色：

| 语义 | 主色 | 容器色 |
| --- | --- | --- |
| Success | `#0B9B66` | `#DDF8EB` |
| Warning | `#B77800` | `#FFF0CD` |
| Error | `#D9304F` | `#FFE8ED` |
| Accent Purple | `#7557E8` | 只用于明确的辅助分类 |
| Accent Mint | `#10AE74` | 下载、本地和成功类入口 |
| Accent Pink | `#F14465` | 喜欢、收藏和 MV 类入口 |
| VIP Gold | `#F2B52A` | VIP 权益，禁止作为普通强调色 |

### 深色主题

| Token | 色值 |
| --- | --- |
| `primary` | `#8EC4FF` |
| `onPrimary` | `#003258` |
| `primaryContainer` | `#154F8E` |
| `onPrimaryContainer` | `#D6E8FF` |
| `background` | `#0F1218` |
| `onBackground` | `#E6E9EF` |
| `surface` | `#1A1F29` |
| `onSurface` | `#E6E9EF` |
| `surfaceContainer` | `#212836` |
| `onSurfaceVariant` | `#AAB2BE` |
| `outline` | `#707987` |
| `outlineVariant` | `#39414D` |

### AMOLED 主题

- `background` 固定为 `#000000`，`surface` 为 `#0A0A0A`，`surfaceContainer` 为 `#111111`。
- 主要文字使用 `#FFFFFF`，次要文字使用 `#A3A3A3`。
- 主色沿用深色主题的 `#8EC4FF`，避免在纯黑背景上直接使用过饱和蓝色。
- 表面仍通过相邻深色和细描边建立层级，不能把所有容器都压成同一块纯黑。

主题色不得成为唯一状态信号；成功、警告和错误必须同时具有图标或明确文案。

### 播放器封面动态色

- 播放器设计图固定布局、层级、密度和控件形态，不固定每首歌曲的实际背景色。生产色板必须由当前歌曲已经加载成功的封面派生；设计稿中的蓝紫色只是一组示例。
- 封面页和歌词页共享一个不可变 `PlayerPalette`。背景渐变、进度、核心播放按钮和逐字歌词高亮使用该语义色板，不得各自取色或在页面内硬编码近似色。
- 大面积背景必须压暗并与深色基底混合；核心非文本强调相对背景至少 `3:1`，按钮文字/图标至少 `4.5:1`。主要和次要文字仍使用经校验的浅色语义角色，不能直接采用封面原始像素。
- 切歌后立即回到静态深色渐变，再等待新封面取色；上一首的异步结果不得提交到下一首。封面缺失、失败、无合格色样或取色异常时整套回退，不保留半套动态色。
- 取色只由媒体标识和一次封面成功事件触发，不得跟随播放进度、歌词滚动、字体倍率或重组重复计算；不额外请求封面，不使用实时模糊。

实现和依赖边界见 [`reference-audits/13-player-artwork-palette.md`](reference-audits/13-player-artwork-palette.md)。

## 字体

首版跟随系统中文字体；品牌字标是独立品牌资产，不用业务文本字体模拟。

| Token | 字号 / 行高 | 字重 | 主要用途 |
| --- | --- | --- | --- |
| `headlineLarge` | `28sp / 36sp` | SemiBold | 页面主标题、登录主标题 |
| `headlineMedium` | `24sp / 32sp` | SemiBold | 模块主标题 |
| `titleLarge` | `22sp / 28sp` | SemiBold | 标准 Toolbar 标题、重要区块 |
| `titleMedium` | `18sp / 26sp` | SemiBold | 卡片标题、Dialog 标题 |
| `titleSmall` | `16sp / 24sp` | SemiBold | 歌曲标题、主要列表标题 |
| `bodyLarge` | `16sp / 24sp` | Regular | 输入内容、重要正文 |
| `bodyMedium` | `14sp / 22sp` | Regular | 正文、歌手和说明文字 |
| `bodySmall` | `12sp / 18sp` | Regular | 时长、徽标和非关键辅助信息 |
| `labelLarge` | `14sp / 20sp` | Medium | 按钮和操作标签 |
| `labelMedium` | `12sp / 16sp` | Medium | 小型徽标 |

- 正文和歌手信息原则上不得低于 `14sp`。
- `12sp` 只能承载时长、徽标、播放量等可丢失的辅助信息。
- 大字体下列表和按钮允许增高，不缩小字号、不裁切主要文案。
- 长标题最多两行；紧凑歌曲行默认一行省略，大字体模式允许增长到两行。

## 间距、形状和层级

### 间距

统一提供 `4 / 8 / 12 / 16 / 20 / 24 / 32 / 40dp`。页面常规水平边距为 `24dp`，紧凑列表和 Toolbar 可使用 `16dp`。

### 圆角

| 圆角 | 用途 |
| --- | --- |
| `8dp` | 小徽标、紧凑封面 |
| `12dp` | 列表封面、普通小容器 |
| `16dp` | Snackbar、按钮、普通卡片 |
| `24dp` | 大卡片、输入组和主要 Surface |
| `28dp` | Dialog、Bottom Sheet 顶部 |
| `36dp` | 沉浸式 Hero 和大型视觉容器 |

### 图标与触控

- 标准操作图标为 `24dp`，辅助图标为 `20dp`，小徽标允许 `16dp`，展示型图标允许 `32dp`。
- 所有交互区域最小为 `48 × 48dp`；图标视觉尺寸不等于触控尺寸。
- 沉浸式返回按钮使用 `48dp` 触控区域和约 `40dp` 半透明圆形 Surface。
- 优先使用 Compose Material Icons；只有 Material 图标无法表达品牌或专有业务语义时才设计 SVG 母版。
- 返回、前进等方向性图标在 RTL 下镜像；播放、暂停和媒体跳转控制不镜像。
- 已确认的返回图形是无横杆、圆端细线 Chevron，由 `docs/design/assets/icons/navigation-back.svg` 作为源母版、`:core:designsystem` 的 `MoeNavigateBackIcon` 作为唯一 Compose 入口。资源按产品与语义命名为 `ic_moe_navigation_back`，禁止按 `login`、`search` 等页面名复制；标准、沉浸式和 Hero 页面只调整外围按钮组合与图形尺寸，不能另画返回路径。VectorDrawable 使用自动 RTL 镜像，调用方不得再次手动翻转。

### 层级

- 常规列表和页面容器以分隔线和色调差建立层级，默认无阴影。
- 同一设置分组内的单行 Item 必须使用一致的行高、上下内边距和内容基线；组尾、页面尾部或为了塞进截图都不得压缩单个 Item。图标、主文案、尾部值、Chevron 与 Switch 分别在该行的可见高度内垂直居中，分隔线只改变边界，不占用或缩短相邻行。
- 标准 Toolbar 与页面使用同一 `surface`，默认无卡片外框、圆角容器或整体阴影；滚动后最多增加底部分隔线。
- Dropdown 和 Snackbar 只使用轻量阴影；Dialog 与 Bottom Sheet 主要依靠遮罩分层。
- 禁止在业务页面自定义任意阴影参数；最终 Elevation Token 在 Compose 截图和真机校准时固化。

## Toolbar 与导航

### Toolbar 变体

1. 标准：以 `10-user-profile.png` 为基准。高度 `64dp`，不含系统状态栏；水平内容边距 `16dp`；标题使用单行 `titleLarge`，相对完整页面宽度几何居中，而不是跟随左侧内容起排。左侧与尾部操作均为 `24dp` 图标置于至少 `48dp` 语义触控区，不显示圆形底板、卡片外框或 Toolbar 整体阴影。
2. 滚动标准态：保持标准态几何与居中标题；内容滚动后切换为实体 `surface`，最多增加一条轻量底部分隔线，不改变图标容器、标题位置或 Toolbar 高度。
3. 搜索：使用独立 `56dp` Toolbar，内部搜索框视觉高度 `40dp`、圆角约 `20dp`；左侧导航与右侧语音操作各占至少 `48dp` 触控区。搜索、清除图标使用独立紧凑槽位，图标背景不得填满输入框高度。搜索属于子页面，不进入底部导航，也不叠加第二个页面标题。
4. 多选：左侧关闭，中间显示已选数量，尾部只保留当前流程需要的少量操作；只有删除等破坏性操作使用 Error 色。
5. 沉浸式例外：仅用于登录 Hero、播放器封面和其他确有复杂图片背景的页面。内容可绘制到状态栏后方，单个操作允许使用半透明圆形 Surface 保证对比度；该圆形外观不得回流到设置、资料、安全验证等标准页面。
6. 内容 Tab：直接位于页面顶部或标准 Toolbar 下方，不再增加重复页面标题。

标准 Toolbar 使用对称槽位保证标题真正居中：无尾部操作时仍保留与左侧等宽的布局占位；存在一至两个尾部操作时，标题继续以页面中心为锚点并在与操作区冲突前省略。导航区必须支持调用方提供语义图标与点击事件，至少覆盖返回和关闭；共享组件负责尺寸、触控、颜色和无障碍，Feature 不得复制 Toolbar 绘制逻辑。

默认启用 edge-to-edge，系统状态栏 Insets 由页面/Toolbar 统一处理且不计入 `64dp` 内容高度。透明 Toolbar 必须根据背景对比度选择前景色；复杂背景只给独立操作增加必要的对比 Surface，不给整个顶部增加磨砂层。

`10-user-profile.png` 是标准 Toolbar 的坐标与视觉母版：设计图保留顶部系统安全区留白，但不自行绘制时间、信号、Wi-Fi 或电池图形；运行时系统栏仍由 Android 绘制。无尾部操作的设置页沿用同一标题基线、左侧 Chevron 锚点和内容起点，只把尾部操作替换为对称空槽。返回图形必须使用共享 `MoeNavigateBackIcon` 的无横杆圆端 Chevron，不得换成带水平箭杆的 Material ArrowBack。

设置页正式设计稿采用固定宽度、内容驱动高度的完整展开画布：宽度用于建立横向设计坐标，高度随全部设置分组自然增长，用来表达页面可纵向滚动，不代表某台设备的固定视口。不得为了把全部内容塞进一屏而压缩下方分组或单个 Item。运行时标准 Toolbar 处理自身 Insets，设置内容由一个纵向滚动容器承载；短视口只改变可见范围，不改变 Item 密度。

### 底部结构

- 一级底部导航固定为“首页 / 发现 / 我的”。
- MiniPlayer 位于底部导航正上方，与导航组成连续 Surface，不使用悬浮玻璃胶囊。
- Snackbar 位于 MiniPlayer 上方；任何反馈不得遮挡播放控制。
- 子页面、搜索、登录和全屏播放器不重复显示一级底部导航。

## 操作与输入

- 普通按钮最小高度 `48dp`，页面主操作可使用 `56dp`；加载状态不能改变按钮宽度。
- 按钮类型限制为 Filled、Filled Tonal、Outlined、Text 和 Destructive，避免业务页面创建新形态。启用的 Outlined 使用 `primary` 描边与 `primary` 内容色；禁用时才回落到 `outlineVariant` 与 `onSurfaceVariant`，业务页面不得覆盖成中性灰启用态。
- 输入框建议高度 `56dp`，圆角 `16–20dp`；聚焦使用 Primary 描边，错误使用 Error 描边和内联错误。
- 带语义前导图标的输入框以已确认登录输入框为母版：图标不是裸放，也不使用圆形底板，而是居中放入低强调的圆角方形 `primaryContainer`。Compact 输入使用约 `30dp` 容器、`8dp` 圆角和 `18dp` 图标；Default 输入使用约 `32dp` 容器、`8dp` 圆角和 `20dp` 图标。可见背景方块与输入框高度分别测量：以 `56dp` Default 输入为例，`32dp` 方块上下各保留约 `12dp` 空间；禁止使用 `fillMaxHeight`、纵向拉伸或把不可见的 `48dp` 触控区画成背景。浅色主题容器使用 `primaryContainer` 约 56% 强度，图标使用 `primary`，并可保留极弱的 Primary 描边。
- 手机、验证码、账号和密码分别使用 Phone、VerifiedUser、AccountCircle 与 Lock 的实心语义图标。聚焦和错误只改变输入框描边与内联反馈，默认不把前导语义图标改成 Error 色；禁用时保留容器形状并整体降低强调。
- 密码可见性、清除和验证码操作属于尾部交互：可见图形不增加 Tonal 底板，但语义触控区不得小于 `48dp`。国家码、下拉图标和固定分隔线属于输入内容结构，不能挤压占位文本或在状态切换时跳动。
- 验证错误不得只使用 Toast；验证码倒计时不能导致尾部操作宽度跳变。
- Segmented Button 用于同级模式切换，例如“验证码 / 密码 / 扫码”；页面层级切换继续使用 Navigation 或 Tab。
- HQ、MV、VIP、已下载等状态使用统一徽标，不与歌曲标题争夺视觉层级。

## 音乐内容组件

- Section Header 由标题、可选说明和单个尾部操作组成；蓝色竖线是内容区标题的可选品牌强调。
- 歌曲行高度默认为 `64–72dp`，封面为 `48–56dp`；大字体时高度随内容增长。
- 歌单和专辑使用方形封面，歌手和用户使用圆形头像；封面本身提供颜色，外层不额外堆叠彩色卡片。
- MiniPlayer 至少包含封面、标题、歌手、播放/暂停和队列；进度状态必须与普通页面状态隔离。
- 队列行提供当前播放指示、选歌、移除和拖拽；所有操作保持 `48dp` 触控区域。

## 页面状态与覆盖层

- 加载、空数据、持续错误和离线属于页面内状态。
- 权限解释优先使用全页面状态；登录与 VIP 限制优先使用内容区内联门槛，只有必须确认时才打开 Dialog。
- 不可在同一页面中用风险、确认或提示卡片替换用户正在操作的主表单。登录额外风险验证属于临时阻断确认：原密码表单保持可辨识但不可操作，叠加遮罩与 Dialog；Dialog 关闭后恢复原草稿和焦点语义。只有确认后的独立验证流程可以成为新页面或隔离 Activity。
- 覆盖层状态设计图统一采用“原页面画布 → 规范遮罩 → 独立 Dialog 或 Bottom Sheet 前景层”的确定性合成方式；不得重新生成遮罩后的底层页面，底层文案、输入草稿、控件尺寸与坐标必须保持不变。
- Bottom Sheet 必须贴住手机视口底部，占满可用宽度，仅顶部使用 `28dp` 圆角，并处理底部安全区；使用遮罩与统一轻量顶缘投影分层，默认 `24dp` 水平内边距、一个 `48dp` Filled 主操作和一个 TextButton 取消，不堆叠两个同等级全宽按钮。
- 手机普通确认 Dialog 在 `390dp` 登录参考宽度上为 `304dp`，六位验证码等结构化输入 Dialog 为 `320dp`；它们分别对应约 `664` 与 `699` 个登录设计单位，Compact 窗口按页面统一系数缩放，不写成脱离设计画布的固定运行宽度。参考画布的单侧边距约为 `43dp` 与 `35dp`。高度由内容决定；`fontScale = 1.0` 时字号、验证码格和操作文案按设计比例，系统大字体时只做可访问性增高/换行。Dialog 使用系统 `Dialog` 的完整可用视口居中行为，不增加业务自定义纵向偏移，也不根据顶部/底部剩余间距重新计算位置。遮罩约为 32% 黑色。双操作区在参考画布使用两个视觉高度 `48dp`、间距 `12dp` 的按钮，随页面统一比例映射且语义触控区始终不小于 `48dp`：左侧取消为低强调 Tonal 容器，右侧主操作为 Filled 容器；不得再把取消画成与主按钮错位的裸文字。Filled 禁用态遵循全局约 `35%` 主色容器与约 `82%` `onPrimary` 内容规则，不回退成与白色表面融合的中性灰块。
- 已完成的登录风险确认与短信验证 Dialog 是全局 Dialog 视觉母版。Dialog 前景使用 `surface` 背景，标题沿用 `titleMedium` 尺寸并提升为 Bold；启用的 Tonal 取消按钮使用 `primaryContainer` 容器与 `primary` 文案，加载锁定时才回落为中性灰禁用态。Filled 确认按钮继续遵循全局启用、加载和禁用规则。其他业务只替换标题、正文、图标或结构化内容，不得重画另一套容器、标题字重、操作区或居中算法。
- Snackbar 左右边距 `16dp`，最多两行和一个操作，位于 MiniPlayer 上方。
- DropdownMenu 必须锚定触发控件；音质、排序等多项单选优先使用 Bottom Sheet。
- 同一时间只展示一个模态覆盖层；系统返回、焦点恢复和遮罩关闭规则必须明确。

Dialog、Snackbar 和 Toast 的完整语义规则见 [`UI_COMPONENTS.md`](UI_COMPONENTS.md)。

## 实现状态

当前 `:core:designsystem` 已完成第一批基础校准：

- Primary 已校准为 `#1677F2`，浅色、深色和 AMOLED Surface 层级已更新。
- Typography 已补齐 `24sp`、`18sp`、`titleSmall`、`bodySmall` 等层级。
- Spacing 已补齐 `12dp`、`20dp` 和 `40dp`，并保留旧属性兼容现有页面。
- Shapes 已增加 `36dp` Hero 语义，图标、触控、输入、Toolbar、MiniPlayer 和底部导航尺寸已建立 Token。
- 标准 Toolbar 已按 `10-user-profile.png` 与 `15-toolbar-navigation.png` 改为页面中心锚定标题，并允许调用方替换返回/关闭等可见导航图标；公共组件继续统一点击事件、`48dp` 触控区、颜色和 Insets。`MoeSearchTopBar` 已由搜索页真实消费，拥有返回、输入、清除、IME Search 和可选尾部事件槽，不持有业务状态。沉浸式 Toolbar 与沉浸式 IconButton 保持 Hero/封面例外；折叠和多选 Toolbar 随真实消费者继续完成。
- 已建立 `MoeSectionHeader`、`MoeArtwork`、`MoeMediaBadge`、`MoeSongRow`、`MoeMiniPlayer` 与 `MoeQueueRow`；搜索、本地音乐和应用播放壳已经消费同一套组件。MiniPlayer 已按 `17-music-content-components.png` 补齐封面、标题/歌手/质量徽标、上一首、主色播放暂停、下一首、队列以及带起止时间的独立进度区，并保留主区域进入全屏播放器的行为。
- 歌曲行在 `1.5×` 字体下增加行高并将时长并入副标题行，避免标题、时长和尾部操作互相覆盖；浅色、深色与大字体截图基准已通过。
- Button、TextField、标准/沉浸式 Toolbar、标准 Dialog 外壳与双操作区已进入 `:core:designsystem`；标准 Toolbar 和 Dialog 已有多个真实页面消费者。Snackbar Host、Input Dialog、Bottom Sheet、MoeToast 和页面状态组件继续随真实消费者落地，不预先建立万能 API。队列拖拽属于播放器后续切片。

后续组件修正继续通过浅色、深色、AMOLED、大字体和截图测试验证。
