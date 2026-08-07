# MoeKoe Mobile V2 UI Mockups

本目录保存移动端视觉稿。当前仓库内已有设计图均已确认；后续只有新增且尚未确认的图片才标记为候选。所有图片的准确实现仍以 Compose 设计令牌、组件规范和无障碍要求为准。

## 基准

- 内容页面：明亮、品牌化的 Material 3。
- 播放页面：参考 Kreate 的封面驱动背景、大封面和歌词氛围，但减少操作密度。
- 目标逻辑视口：约 `390 × 845dp`；原始 `852 × 1846` 画布保持宽高比，旧 `390 × 844dp` 仅是设备级近似标注。
- 正文与说明文字：原则上不低于 `14sp`；仅徽标、时间等辅助信息可使用 `12sp`。

## 核心视觉稿

1. [`01-home-material3-v2.png`](01-home-material3-v2.png)：首页、搜索入口、三栏底部导航与 MiniPlayer 基准。
2. [`02-discover-v2.png`](02-discover-v2.png)：分类 Tab 直接置顶的发现页、排行榜和分类歌单。
3. [`03-search-results-v2.png`](03-search-results-v2.png)：从首页进入、不显示一级底部导航的独立搜索结果页。
4. [`04-my-v4.png`](04-my-v4.png)：个人资料、签到、领取 VIP、完整收藏与关注入口、创建歌单、云盘和本地音乐入口。
5. [`05-playlist-detail.png`](05-playlist-detail.png)：歌单信息、播放操作和歌曲列表。
6. [`06-player-cover.png`](06-player-cover.png)：Kreate 启发的封面播放器。
7. [`07-player-lyrics.png`](07-player-lyrics.png)：逐字歌词、翻译和音译展示基准。
8. [`08-player-queue.png`](08-player-queue.png)：播放队列 Bottom Sheet。
9. [`09-settings.png`](09-settings.png)：主题、播放、歌词和存储设置。
10. [`10-user-profile.png`](10-user-profile.png)：独立用户主页、社交统计、听歌概览和公开创建歌单。
11. [`11-dialog-components.png`](11-dialog-components.png)：确认、提示、危险操作和文本输入 Dialog。
12. [`12-feedback-components-v2.png`](12-feedback-components-v2.png)：品牌化 MoeSnackbar、应用内 MoeToast 与系统 Toast 边界。
13. [`13-login-phone-immersive.png`](13-login-phone-immersive.png)：沉浸式手机号验证码登录主状态；密码、扫码和安全验证沿用同一视觉结构。
14. [`14-design-foundations.png`](14-design-foundations.png)：颜色、主题、排版、间距、圆角、图标与触控区域视觉总览。
15. [`15-toolbar-navigation.png`](15-toolbar-navigation.png)：沉浸式、标准、折叠、搜索和多选 Toolbar，以及 Tab、MiniPlayer 和底部导航。
16. [`16-actions-inputs.png`](16-actions-inputs.png)：按钮、IconButton、分段控件、Chip、输入框、选择控件和进度组件。
17. [`17-music-content-components.png`](17-music-content-components.png)：Section Header、歌曲行、封面内容、用户资产、徽标、MiniPlayer 和队列行。
18. [`18-mobile-states-overlays.png`](18-mobile-states-overlays.png)：手机视口中的页面状态、权限与账号门槛、Bottom Sheet、Dialog、反馈和临时表面。
19. [`19-login-password-states.png`](19-login-password-states.png)：旧版横向状态总览，仅保留设计演进记录。
20. [`20-login-qr-states.png`](20-login-qr-states.png)：旧版横向状态总览，仅保留设计演进记录。
21. [`21-login-risk-verification.png`](21-login-risk-verification.png)：旧版横向状态总览，仅保留设计演进记录。
22. [`22-login-multi-account.png`](22-login-multi-account.png)：旧版横向状态总览，仅保留设计演进记录。

未带 `v2` 后缀的前四张图片以及 `04-my-v2.png`、`04-my-v3.png`、`12-feedback-components.png` 保留为早期方案，仅用于设计演进对照，不再作为实现基准。

旧 `19` 至 `22` 曾于 2026-08-05 确认，但其实现基线资格已因密度、覆盖层和单图分辨率复核于 2026-08-06 撤销。新版独立单状态图位于 [`candidates/login-v2`](candidates/login-v2/README.md)，现已确认为正式实现基线，可直接用于 Compose 返工。

`06` 至 `08` 是全屏播放器封面、歌词与队列的已确认视觉基线。对应 [`player-flow`](../prototypes/player-flow/README.md) 原型建立在这些设计图之后，只验证横向分页、歌词定位、纵向退出、队列覆盖层和系统返回优先级；原型内图标、封面、文案与时序均不是 Compose 实现来源。

歌词补充状态 `23a` 至 `23h` 位于历史路径 [`candidates/player-lyrics-v1`](candidates/player-lyrics-v1/README.md)，页面结构与状态表达已于 2026-08-06 确认。播放器图片固定结构而不固定单曲色板：`06`、`07` 和 `23a` 至 `23h` 中的背景与强调色只表示示例，生产实现统一按当前歌曲封面派生并遵循动态调色门禁。

登录确认稿的 Android 生产 Hero 已派生为 [`../assets/login-hero.png`](../assets/login-hero.png)。该文件只提供无文字插画背景；返回、标题、表单和所有图标仍由 Compose 按确认设计与 Design System 原生绘制。

## 视觉门禁顺序

1. 功能规格先列出全部适用 UI 状态，并审计现有视觉与组件约束。
2. 缺少状态设计时，先在现有 MoeKoe Air 语言内生成静态候选图。
3. 用户确认后将候选图标记为“已确认”，并同步唯一页面映射与阶段计划。
4. 设计图确认后默认直接补页面适配契约并实现 Compose；不把原型作为固定中间产物。
5. 只有滚动吸附、转场、手势、响应式重排或复杂跨状态交互仍存在静态图无法回答的问题时，才基于已确认设计图制作最小原型；原型不能先于设计图、不能替代设计确认，也不能改变视觉尺寸。
6. 若制作原型，将验证结论和组件行为同步到文档后再实现对应交互；没有原型需求时直接按确认稿、适配契约和 Design System 落地。

上述候选流程只适用于未来新增且当前不存在的设计图；本目录现有图片已经确认，可直接按既定阶段顺序实现。

## 使用方式

- 已确认页面稿是结构、坐标、组件视觉尺寸比例、间距、圆角、层级和图片裁切的权威实现依据，必须一比一复刻；“不直接作为固定像素切图”只表示不能把整张 PNG 当页面，也不能把原图像素直接当 Android `dp`，不表示可以自由改尺寸。
- 每个页面以原始画布建立设计坐标空间，再根据当前 Compose 容器宽度使用同一个比例映射所有视觉尺寸。禁止按物理设备分辨率逐组件调参、横纵分别拉伸或用窗口高度稀释设计密度。
- 页面滚动属性属于确认设计的一部分。标准视口中内容完整的页面必须保持零滚动范围；只有页面契约登记的短高度、IME、大字体或真实内容溢出场景可以启用条件滚动。
- 设计系统图板中的自动生成日期、说明文字或业务数据不作为实现依据；颜色语义、交互状态、无障碍下限和共享组件行为以 [`DESIGN_SYSTEM.md`](../../DESIGN_SYSTEM.md) 为准。
- Compose 实现应将颜色、排版、形状和间距提取到 `:core:designsystem`。
- Dialog、Snackbar 与 Toast 的行为和尺寸以 [`UI_COMPONENTS.md`](../../UI_COMPONENTS.md) 为实现规范，图片只作为视觉参考。
- 播放器封面页与歌词页共享当前歌曲封面派生的语义色板，并必须提供静态深色渐变回退；不要求低性能设备实时模糊。完整规则见 [`13-player-artwork-palette.md`](../../reference-audits/13-player-artwork-palette.md)。
- 真正落地前需先通过确认稿归一化叠加，再验证约 `390 × 845dp`、短高度、大字体和不同窗口宽度；重新录制 Compose 截图不能代替设计符合度。
- 自动生成图中的示例账号、二维码、日期、响应文案与尺寸标注只用于视觉表达；实现必须使用虚构测试数据、字符串资源、固定协议审计和 Design System Token。
