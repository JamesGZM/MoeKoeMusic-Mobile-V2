# 阶段 2：播放内核补充参考审计

状态：Accepted。审计日期：2026-08-05。

## 权威基线

- AndroidX Media3 后台播放：`MediaLibraryService` 独占 Player，Activity 使用 Controller 连接。
- Playback resumption：冷启动恢复媒体与位置但不自动出声，由用户或系统媒体入口主动恢复。
- MediaSession：只接受本 App、系统和可信控制器。

现有 `MoeKoePlaybackService`、`Media3PlaybackController` 和 Room Snapshot 符合上述责任边界。本次补审未发现需要在阶段 3 前进行架构重写的问题。

## Kreate

- 固定提交：`f02577e862318df26b13abb02d14d8d824a1b947`。
- 许可证：GPL-3.0；仅学习思想。
- 文件：`composeApp/src/androidMain/kotlin/it/fast4x/rimusic/service/modern/MediaLibrarySessionCallback.kt`。
- 采用：Session callback 集中处理媒体请求、领域媒体与 Media3 对象分离。
- 不采用：历史 RiMusic 兼容分支、KMP/扩展体系和超出第一版范围的自定义命令。

## Metrolist

- 固定提交：`289ed45d0a429e6e4f19a2a07543b243a0ab2e8b`。
- 许可证：GPL-3.0；仅学习思想。
- 文件：`app/src/main/kotlin/com/metrolist/music/playback/MediaLibrarySessionCallback.kt`。
- 采用：系统媒体控制与应用队列保持单一 Player 状态源。
- 不采用：大型单模块 Service 职责和在线业务耦合。

## 结论

第三方项目证明了 MediaLibrarySession 集中管理的可行性，但系统生命周期以 Media3 官方文档为准。阶段 3 只扩展已导入本地源解析与显式队列命令，不把文件扫描或导入逻辑放入 Service。
