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

未带 `v2` 后缀的前四张图片以及 `04-my-v2.png`、`04-my-v3.png`、`12-feedback-components.png` 保留为早期方案，仅用于设计演进对照，不再作为实现基准。

## 使用方式

- 视觉稿用于确认信息层级、视觉气质和主要组件关系，不直接作为固定像素切图实现。
- Compose 实现应将颜色、排版、形状和间距提取到 `:core:designsystem`。
- Dialog、Snackbar 与 Toast 的行为和尺寸以 [`UI_COMPONENTS.md`](../../UI_COMPONENTS.md) 为实现规范，图片只作为视觉参考。
- 播放器背景必须提供静态渐变回退，不要求低性能设备实时模糊。
- 真正落地前需在 `390 × 844 dp`、大字体和不同屏幕宽度下重新验证。
