# 全屏播放器封面页设计符合度证据

- 确认稿：`docs/design/mockups/06-player-cover.png`
- 实现截图：`feature/player/src/screenshotTestDebug/reference/cn/james/music/feature/player/PlayerScreenshotTestKt/PlayerCoverScreenshot_Cover_fbd8d4e9_0.png`
- 比较范围：确认稿 `853 × 1844px` 归一化为实现截图 `1024 × 2216px`，完整页面同画布比较。

## 证据文件

- `player-cover-2026-08-08-side-by-side.png`
- `player-cover-2026-08-08-overlay.png`
- `player-cover-2026-08-08-diff.png`

## 结论

- 顶部收起/标题/更多、方形封面、分页点、歌曲信息/品质/收藏、进度、五项核心控制和四项次级动作均已恢复，并与确认稿同输入复核。
- 播放、暂停、缓冲、连接中、未知时长、封面失败、空播放项、`1.5×`、`2.0×` 共 9 组截图基线通过；大字体保持标题、进度和核心控制可见，次级动作可通过页面滚动到达。
- 指定 ELE-AL00 / API 29 的 App instrumentation 回归 24/24 通过；未创建或启动模拟器。
- 下载、加入歌单、分享、收藏和更多当前是确认稿要求的视觉语义，不伪装业务已完成；队列、播放暂停、上下首、seek 与模式继续使用既有端口。
- 保留的 P3 差异为原创封面内容、静态深色调色回退与 Material 图标笔画；槽位、构图、裁切、尺寸和色彩语义一致。
