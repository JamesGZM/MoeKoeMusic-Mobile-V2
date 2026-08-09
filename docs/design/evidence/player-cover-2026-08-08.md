# 全屏播放器封面页设计符合度证据

- 确认稿：`docs/design/mockups/06-player-cover.png`
- 实现截图：`feature/player/src/screenshotTestDebug/reference/cn/james/music/feature/player/PlayerScreenshotTestKt/PlayerCoverScreenshot_Cover_fbd8d4e9_0.png`
- 比较范围：确认稿 `853 × 1844px` 归一化为实现截图 `1024 × 2216px`，完整页面同画布比较。

## 证据文件

- `player-cover-2026-08-08-side-by-side.png`
- `player-cover-2026-08-08-overlay.png`
- `player-cover-2026-08-08-diff.png`

## 结论

- 封面成功后由 `:feature:player` 内部复用 Coil 已解码图片，在后台缩至最长边 `96px`、最多 `12` 色并生成共享 `PlayerPalette`；封面、背景、控制与歌词使用同一语义角色。强调色以四层 glow 的实际 Canvas 顺序累计 alpha 合成后的最亮背景验证 `3:1`，强调容器前景验证 `4.5:1`。关闭应用级“封面动态取色”会立即回退到确认稿的固定深色基线，不发起额外请求；切歌、关闭和迟到回调均由媒体 id 代际隔离。

- 第二轮同画布审查否决了旧结论：旧实现虽然元素齐全，但封面锚点、歌曲信息内边距、收藏位置、进度细节、五项核心控制节奏和次级动作密度仍有肉眼可见偏差，不能以“截图已存在”视为通过。
- 本轮只修正 Compose 视觉：封面恢复 `333dp` 基准边长和确认稿纵向锚点；歌曲信息/进度恢复 `30dp` 视觉边距；核心控制改为更宽的五点节奏，主按钮收敛至 `68dp`；进度轨道、拇指、时间标签和 `46dp` 次级动作均按确认稿重新校准。
- 顶部收起/标题/更多、方形封面、分页点、歌曲信息/品质/收藏、进度、五项核心控制和四项次级动作已重新与确认稿同画布并排、叠加和差异复核。不同真实封面内容、静态深色回退色板和 Material 图标笔画不作为结构偏差。
- 028 将质量 badge 的真值改为当前 `PlaybackState.currentResolvedQuality`：Standard fixture 继续保持确认稿既有基线；无 runtime quality 时 badge 不渲染；最长“母带”标签在同一 badge 容器内完整显示。封面与歌词页复用同一控制区，不从设置偏好推断显示值。
- 播放、暂停、缓冲、连接中、未知时长、封面失败、空播放项、`1.5×`、`2.0×` 共 9 组截图基线通过；大字体保持标题、进度和核心控制可见，次级动作可通过页面滚动到达。
- 下载、加入歌单、分享、收藏和更多当前是确认稿要求的视觉语义，不伪装业务已完成；队列、播放暂停、上下首、seek 与模式继续使用既有端口。

## 验证

- 本轮只消费既有短期 runtime quality，不改变播放、解析、队列或持久化业务行为。
- `spotlessCheck`、`:feature:player:testDebugUnitTest`、`:feature:player:validateDebugScreenshotTest`、`:feature:player:lintDebug`、`:app:assembleDebug`、`:app:compileDebugAndroidTestKotlin` 均通过。
- 指定 ELE-AL00 / API 29 的 App instrumentation 25/25 通过；未创建或启动模拟器。
