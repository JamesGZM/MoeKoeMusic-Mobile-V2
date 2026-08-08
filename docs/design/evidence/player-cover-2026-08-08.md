# 全屏播放器封面页设计符合度证据

- 确认稿：`docs/design/mockups/06-player-cover.png`
- 实现截图：`feature/player/src/screenshotTestDebug/reference/cn/james/music/feature/player/PlayerScreenshotTestKt/PlayerCoverScreenshot_Cover_fbd8d4e9_0.png`
- 比较范围：确认稿 `853 × 1844px` 归一化为实现截图 `1024 × 2216px`，完整页面同画布比较。

## 证据文件

- `player-cover-2026-08-08-side-by-side.png`
- `player-cover-2026-08-08-overlay.png`
- `player-cover-2026-08-08-diff.png`

## 结论

- 第二轮同画布审查否决了旧结论：旧实现虽然元素齐全，但封面锚点、歌曲信息内边距、收藏位置、进度细节、五项核心控制节奏和次级动作密度仍有肉眼可见偏差，不能以“截图已存在”视为通过。
- 本轮只修正 Compose 视觉：封面恢复 `333dp` 基准边长和确认稿纵向锚点；歌曲信息/进度恢复 `30dp` 视觉边距；核心控制改为更宽的五点节奏，主按钮收敛至 `68dp`；进度轨道、拇指、时间标签和 `46dp` 次级动作均按确认稿重新校准。
- 顶部收起/标题/更多、方形封面、分页点、歌曲信息/品质/收藏、进度、五项核心控制和四项次级动作已重新与确认稿同画布并排、叠加和差异复核。不同真实封面内容、静态深色回退色板和 Material 图标笔画不作为结构偏差。
- 播放、暂停、缓冲、连接中、未知时长、封面失败、空播放项、`1.5×`、`2.0×` 共 9 组截图基线通过；大字体保持标题、进度和核心控制可见，次级动作可通过页面滚动到达。
- 下载、加入歌单、分享、收藏和更多当前是确认稿要求的视觉语义，不伪装业务已完成；队列、播放暂停、上下首、seek 与模式继续使用既有端口。

## 验证

- 本轮只修改 UI 几何与视觉，不新增或改变播放业务行为。
- `spotlessCheck`、`:feature:player:testDebugUnitTest`、`:feature:player:validateDebugScreenshotTest`、`:feature:player:lintDebug`、`:app:assembleDebug`、`:app:compileDebugAndroidTestKotlin` 均通过。
- 指定 ELE-AL00 / API 29 的 App instrumentation 25/25 通过；未创建或启动模拟器。
