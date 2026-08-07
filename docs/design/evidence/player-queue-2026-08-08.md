# 播放队列 Sheet 设计符合度证据

- 确认稿：`docs/design/mockups/08-player-queue.png`
- 实现截图：`feature/player/src/screenshotTestDebug/reference/cn/james/music/feature/player/PlayerScreenshotTestKt/PlayerQueueScreenshot_Queue_11b2e5c9_0.png`
- 比较范围：从确认稿 `853 × 1844px` 的 `y=818` 起裁出队列 Sheet，并归一化为实现截图 `1024 × 1234px`；两侧使用相同可见范围比较。

## 证据文件

- `player-queue-2026-08-08-side-by-side.png`
- `player-queue-2026-08-08-overlay.png`
- `player-queue-2026-08-08-diff.png`

## 结论

- Sheet 顶部圆角、拖拽柄、标题/数量/模式/清空、来源行、当前项、六行可见密度与关闭提示均按确认稿恢复；运行时使用 Material 3 `ModalBottomSheet` 的遮罩、系统返回和 Insets。
- 标准队列、空队列和 `1.5×` 字体共 3 组截图基线通过；大字体下标题与操作区分行，列表仍保持完整行，不暴露被裁切的下一行。
- 指定 ELE-AL00 / API 29 已从 MiniPlayer 和全屏播放器分别打开真实 3 项调试队列，验证系统 Sheet、遮罩、系统返回优先级和真实播放模式；未创建或启动模拟器。
- 截图 fixture 的 12 项内容与封面只用于复现确认稿的信息密度；生产界面展示真实队列字段，不伪造业务数据。
- 拖拽点阵当前只保留确认稿视觉与“暂不可用”的可访问性说明；没有伪装队列重排已经实现。选择、移除、清空和模式切换继续复用现有事件。
- 保留的 P3 差异为 fixture 封面内容、跨平台字体字形与 Material 图标笔画；槽位、尺寸、层级和交互语义一致。
