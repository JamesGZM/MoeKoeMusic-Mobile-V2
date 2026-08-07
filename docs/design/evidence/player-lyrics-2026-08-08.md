# 全屏播放器歌词页设计符合度证据

- 主确认稿：`docs/design/mockups/07-player-lyrics.png`
- 状态确认稿：`docs/design/mockups/candidates/player-lyrics-v1/23a` 至 `23h`
- 实现基线：`feature/player/src/screenshotTestDebug/reference/cn/james/music/feature/player/PlayerScreenshotTestKt/PlayerLyrics*`
- 比较范围：确认稿 `853 × 1844px` 与实现截图 `1024 × 2216px` 归一化到同一画布；状态对照图中每一行左侧为确认稿、右侧为实现。

## 证据文件

- `player-lyrics-2026-08-08-side-by-side.png`：原文 + 翻译主态完整页面并排。
- `player-lyrics-2026-08-08-overlay.png`：主态同画布平均叠加。
- `player-lyrics-2026-08-08-diff.png`：主态像素差异辅助图。
- `player-lyrics-2026-08-08-status-states.png`：加载、空、离线、错误四组逐状态并排。
- `player-lyrics-2026-08-08-content-states.png`：翻译、原文、音译、150% 和 200% 五组逐状态并排。

## 结论

- 歌词页与封面页使用真实 `HorizontalPager`，顶部栏和底部播放状态仍来自同一播放器页面；歌词页第二枚分页点高亮。
- 歌词调节入口、歌词视窗、歌曲信息、品质、收藏、进度与五项核心控制保持确认稿层级；底部安全留白经同画布对照校准。
- 加载、空、离线和错误只替换歌词视窗；重试按钮使用已确认的全局 Filled 主按钮视觉，不用系统默认色或假成功反馈。
- 原文、翻译、音译及当前行分段强调已覆盖；150% / 200% 是歌词显示档位，只改变歌词视窗字号和密度，不错误放大顶部栏与播放控制。
- 当前切片只交付可复用的纯 UI 状态和事件端口。歌词仓库加载、缓存、逐行同步、手动滚动暂停跟随和点击 seek 由后续独立业务切片接入。
- 指定 ELE-AL00 / API 29 的 App instrumentation 回归 24/24 通过，其中包含 MiniPlayer 进入全屏播放器、左滑到歌词页、加载态与歌词设置入口验证；未创建或启动模拟器。
