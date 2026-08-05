# MoeKoe Mobile V2 UI Mockups

本目录保存已确认或待确认的移动端视觉稿。所有图片均为设计参考，实际实现以 Compose 设计令牌、组件规范和无障碍要求为准。

## 基准

- 内容页面：明亮、品牌化的 Material 3。
- 播放页面：参考 Kreate 的封面驱动背景、大封面和歌词氛围，但减少操作密度。
- 目标逻辑视口：`390 × 844 dp`。
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
19. [`19-login-password-states.png`](19-login-password-states.png)：已确认；密码登录的默认、提交、凭据错误与安全验证触发状态。
20. [`20-login-qr-states.png`](20-login-qr-states.png)：已确认；扫码登录的生成、待扫码、已扫码、过期与获取失败状态。
21. [`21-login-risk-verification.png`](21-login-risk-verification.png)：已确认；短信二次验证、隔离腾讯图形验证、失败与成功状态。
22. [`22-login-multi-account.png`](22-login-multi-account.png)：已确认；手机号多账号选择、提交失败与账号切换 Sheet。

未带 `v2` 后缀的前四张图片以及 `04-my-v2.png`、`04-my-v3.png`、`12-feedback-components.png` 保留为早期方案，仅用于设计演进对照，不再作为实现基准。

`19` 至 `22` 已于 2026-08-05 使用 `frontend-design` 约束生成并经用户确认，现已加入 `DEVELOPMENT_PLAN.md` 的实现基线。对应 [`login-flow`](../prototypes/login-flow/README.md) 原型只验证交互关系，不替代 Android 实现和验收。

`06` 至 `08` 是全屏播放器封面、歌词与队列的已确认视觉基线。对应 [`player-flow`](../prototypes/player-flow/README.md) 原型建立在这些设计图之后，只验证横向分页、歌词定位、纵向退出、队列覆盖层和系统返回优先级；原型内图标、封面、文案与时序均不是 Compose 实现来源。

登录确认稿的 Android 生产 Hero 已派生为 [`../assets/login-hero.png`](../assets/login-hero.png)。该文件只提供无文字插画背景；返回、标题、表单和所有图标仍由 Compose 按确认设计与 Design System 原生绘制。

## 视觉门禁顺序

1. 功能规格先列出全部适用 UI 状态，并审计现有视觉与组件约束。
2. 缺少状态设计时，先在现有 MoeKoe Air 语言内生成静态候选图。
3. 用户确认后将候选图标记为“已确认”，并同步唯一页面映射与阶段计划。
4. 只有复杂交互确有必要时，才基于已确认设计图制作原型；原型不能先于设计图，也不能替代设计图确认。
5. 原型结论和组件行为同步到文档后，才开始 Design System 与 Compose 业务实现。

## 使用方式

- 视觉稿用于确认信息层级、视觉气质和主要组件关系，不直接作为固定像素切图实现。
- 设计系统图板中的自动生成日期、说明文字或标注数值不作为实现依据；准确 Token 和组件行为以 [`DESIGN_SYSTEM.md`](../../DESIGN_SYSTEM.md) 为准。
- Compose 实现应将颜色、排版、形状和间距提取到 `:core:designsystem`。
- Dialog、Snackbar 与 Toast 的行为和尺寸以 [`UI_COMPONENTS.md`](../../UI_COMPONENTS.md) 为实现规范，图片只作为视觉参考。
- 播放器背景必须提供静态渐变回退，不要求低性能设备实时模糊。
- 真正落地前需在 `390 × 844 dp`、大字体和不同屏幕宽度下重新验证。
- 自动生成图中的示例账号、二维码、日期、响应文案与尺寸标注只用于视觉表达；实现必须使用虚构测试数据、字符串资源、固定协议审计和 Design System Token。
