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

- 歌词页不单独提取封面颜色：高亮歌词、设置入口与辅助容器复用封面页已经解析的 `PlayerPalette`，以保持两页颜色角色一致；正文及次要正文仍使用固定深色主题语义色保证可读性。

- 第二轮复核否决了旧证据中“截图基线即通过”的结论：旧实现把不同歌词密度统一按容器自然居中，导致加载态以及原文、音译和大字号状态整体下沉；底部 Compact 进度轨、滑块和收藏位置也与确认稿不一致。
- 本轮逐状态按同画布重新校准：Translation、Original、Phonetic、150% 和 200% 的当前行与上下文行锚点分别对齐；Loading 单独上移，Empty、Offline、Error 保持设计稿中心，且所有调整只发生在歌词视窗内。
- 歌词页与封面页使用真实 `HorizontalPager`，顶部栏和底部播放状态仍来自同一播放器页面；歌词页第二枚分页点高亮。
- 歌词调节入口、歌词视窗、歌曲信息、品质、收藏、进度与五项核心控制保持确认稿层级；底部安全留白经同画布对照校准。
- 加载、空、离线和错误只替换歌词视窗；重试按钮使用已确认的全局 Filled 主按钮视觉，不用系统默认色或假成功反馈。
- 原文、翻译、音译及当前行分段强调已覆盖；150% / 200% 是歌词显示档位，只改变歌词视窗字号和密度，不错误放大顶部栏与播放控制。
- 2026-08-10 业务接线已由 app-side `AppPlayerLyricsViewModel` 完成：Pager settled 到歌词页才读取 Repository，离页/切歌取消并隔离迟到结果，成功文档复用；逐字强调改为从行首到当前播放位置的前缀，点击行经既有 `AppPlaybackViewModel` seek。Feature 仍只接收纯 UI 状态和 timing 输入。
- 2026-08-10 “翻译与音译”偏好仅以 app 注入的纯 Boolean 控制 secondary 渲染：关闭态复用确认的 Original-only 视觉，不改变歌词文档、Repository 请求或 timing；默认开启的 Translation/Phonetic 截图基线保持不变。
- 背景行、对唱对齐以及手动滚动暂停自动跟随没有确认稿承载或本切片交互设计，仍明确保留后续；本次未启动模拟器、ADB、真机或真实服务。
- `git diff --check`、`spotlessCheck`、播放器 JVM 单测、9 组截图校验、播放器 lint、Debug App 构建与 AndroidTest 编译均通过；指定 ELE-AL00 / API 29 的 App instrumentation 回归 25/25 通过，未创建或启动模拟器。
