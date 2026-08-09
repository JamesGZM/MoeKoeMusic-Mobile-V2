# 播放队列 Sheet 设计符合度证据

- 确认稿：`docs/design/mockups/08-player-queue.png`
- 实现截图：`feature/player/src/screenshotTestDebug/reference/cn/james/music/feature/player/PlayerScreenshotTestKt/PlayerQueueScreenshot_Queue_11b2e5c9_0.png`
- 比较范围：从确认稿 `853 × 1844px` 的 `y=818` 起裁出队列 Sheet，并归一化为实现截图 `1024 × 1234px`；两侧使用相同可见范围比较。

## 证据文件

- `player-queue-2026-08-08-side-by-side.png`
- `player-queue-2026-08-08-overlay.png`
- `player-queue-2026-08-08-diff.png`

## 结论

- 第二轮同画布复核发现旧实现虽然几何锚点基本一致，但大面积 Sheet 容器与当前项 Surface 明显偏蓝；旧截图基线没有拦住这项可见差异。本轮按确认稿重新校准为 `#222538` 与 `#2E304F`，并重新生成并排、叠加和差异证据。
- Sheet 顶部圆角、拖拽柄、标题/数量/模式/清空、来源行、当前项、六行可见密度与关闭提示均按确认稿恢复；运行时使用 Material 3 `ModalBottomSheet` 的遮罩、系统返回和 Insets。
- 标准队列、空队列和 `1.5×` 字体共 3 组截图基线通过；大字体下标题与操作区分行，列表仍保持完整行，不暴露被裁切的下一行。
- `git diff --check`、`spotlessCheck`、播放器 JVM 单测、全部播放器截图校验、播放器 lint、Debug App 构建与 AndroidTest 编译均通过；指定 ELE-AL00 / API 29 的 App instrumentation 回归 25/25 通过，未创建或启动模拟器。
- 截图 fixture 的 12 项内容与封面只用于复现确认稿的信息密度；生产界面展示真实队列字段，不伪造业务数据。
- 022 将拖拽点阵接为真实重排入口：可见 `22dp` 点阵与移除圆按钮的绝对中心保持，点阵由独立且不与移除重叠的 `48dp` 长按命中节点承载；为满足两个独立动作，时长/文字可用尾宽必要收窄约 `17dp`，因此仅 Queue、Probe、LargeText 三张受影响 golden 更新。旧 golden 对新 rendered 的局部量化为：duration bbox `676,290,76,840` mean `0.024524219` / changed `0.054432957`；handle icon bbox `814,260,48,900` 和 remove button bbox `890,242,78,930` 均为 `0`。最新同画布复核通过：mean `0.0317157`、changed `0.0666075`、累计漂移 `0.54457`，Sheet/handle/header/source/firstItem/dismiss 锚点误差分别为 `0.1665/0.0835/0.3369/0.5806/0.3549/0.1896`。非 Shuffle 的 stable-id 临时顺序只在 drop 一次提交，App 以最新权威队列解析最终 Media3 index；Rejected/stale、权威变更、Sheet 关闭和 2 秒未对齐回滚。随机、空/单项提供逐行禁用原因，TalkBack 上移/下移复用同一请求。截图验证通过；未启动设备，真实拖动、惯性、边缘自动滚动与横纵手势仲裁仍待真机验证。
- 保留差异仅为生产可用测试封面内容、跨平台字体字形与 Material 图标笔画；槽位、尺寸、层级和交互语义一致。
