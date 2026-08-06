# MoeKoe Air Design System

状态：第一版视觉方向已确认，Compose Token 与 TopBar 第一批校准已完成，其余通用组件继续按本文实现。确认日期：2026-08-05。

本文是颜色、排版、间距、形状和通用组件的数值与行为真值。视觉稿用于确认形态、密度和气质；如果视觉稿中的自动生成文字、日期或标注数值与本文冲突，以本文为准。

视觉参考：

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

### 层级

- 常规列表和页面容器以分隔线和色调差建立层级，默认无阴影。
- Toolbar、Dropdown 和 Snackbar 只使用轻量阴影；Dialog 与 Bottom Sheet 主要依靠遮罩分层。
- 禁止在业务页面自定义任意阴影参数；最终 Elevation Token 在 Compose 截图和真机校准时固化。

## Toolbar 与导航

### Toolbar 变体

1. 沉浸式：内容绘制到状态栏后方，无独立标题行；返回按钮悬浮在安全区。登录、播放器和 Hero 详情优先使用。
2. 标准：高度 `64dp`，不含系统状态栏；水平边距 `16dp`，标题使用 `titleLarge`，尾部最多两个主要操作。
3. 滚动折叠：展开时透明且不重复 Hero 标题；折叠后变为实体 Surface，显示标题和轻分隔线。
4. 搜索：返回按钮加 `56dp` 高搜索框；搜索是子页面，不进入底部导航。
5. 多选：关闭、已选数量、全选和删除；只有删除使用 Error 色。
6. 内容 Tab：直接位于页面顶部或 Toolbar 下方，不再增加重复页面标题。

默认启用 edge-to-edge。透明 Toolbar 必须根据背景对比度选择前景色；复杂背景优先使用半透明圆形按钮，不给整个顶部增加磨砂层。

### 底部结构

- 一级底部导航固定为“首页 / 发现 / 我的”。
- MiniPlayer 位于底部导航正上方，与导航组成连续 Surface，不使用悬浮玻璃胶囊。
- Snackbar 位于 MiniPlayer 上方；任何反馈不得遮挡播放控制。
- 子页面、搜索、登录和全屏播放器不重复显示一级底部导航。

## 操作与输入

- 普通按钮最小高度 `48dp`，页面主操作可使用 `56dp`；加载状态不能改变按钮宽度。
- 按钮类型限制为 Filled、Filled Tonal、Outlined、Text 和 Destructive，避免业务页面创建新形态。
- 输入框建议高度 `56dp`，圆角 `16–20dp`；聚焦使用 Primary 描边，错误使用 Error 描边和内联错误。
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
- Bottom Sheet 必须贴住手机视口底部，占满可用宽度，仅顶部使用 `28dp` 圆角，并处理底部安全区。
- 手机 Dialog 宽度为 `min(320.dp, windowWidth - 48.dp)`，默认相对完整可用视口水平、垂直居中；不得相对底部表单或剩余内容区居中。遮罩约为 32% 黑色，确认型操作沿用 [`11-dialog-components.png`](design/mockups/11-dialog-components.png) 的左侧文字取消与右侧主按钮结构。
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
- 已建立标准 Toolbar、沉浸式 Toolbar 和沉浸式 IconButton；搜索、折叠和多选 Toolbar 在对应页面接入时继续完成。
- 已建立 `MoeSectionHeader`、`MoeArtwork`、`MoeMediaBadge`、`MoeSongRow`、`MoeMiniPlayer` 与 `MoeQueueRow`；搜索、本地音乐和应用播放壳已经消费同一套组件。
- 歌曲行在 `1.5×` 字体下增加行高并将时长并入副标题行，避免标题、时长和尾部操作互相覆盖；浅色、深色与大字体截图基准已通过。
- 按钮、输入、通用 Bottom Sheet、MoeToast 和页面状态组件仍待实现；队列拖拽属于播放器后续切片。

后续组件修正继续通过浅色、深色、AMOLED、大字体和截图测试验证。
