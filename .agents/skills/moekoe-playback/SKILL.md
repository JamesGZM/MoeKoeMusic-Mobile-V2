---
name: moekoe-playback
description: Implement, debug, or review MoeKoeMusic playback behavior. Use for playback/, Media3, ExoPlayer, MediaLibraryService, MediaSession, queues, play modes, source resolution, playback restoration, notifications, progress, MiniPlayer, full-screen player state, artwork palettes, or feature/player.
---

# MoeKoe 播放内核

## 读取路由

1. 读取 `docs/ARCHITECTURE.md` 的播放器边界、`docs/plans/01-playback-core.md`、`docs/reference-audits/02-playback-core.md` 和 `docs/TESTING_STRATEGY.md` 的 `:playback` 矩阵。
2. 全屏播放器或动态色板再读取审计 `11-fullscreen-player-ui.md`、`13-player-artwork-palette.md` 及对应 UI 设计文档。
3. 本地导入后播放同时调用 `$moekoe-local-import`；播放器 UI 同时调用 `$moekoe-ui-compose`。

## 不变量

- `:playback` 是唯一创建和持有 ExoPlayer 的模块；UI、ViewModel 和 Repository 不直接持有 Player。
- `MediaLibraryService` 负责播放生命周期和系统媒体集成，不承担搜索、登录、收藏或歌词解析业务。
- 队列、播放模式和恢复建立单一事实来源；进程、Service 与 UI 的状态转换必须显式且可测试。
- 播放进度等高频状态与常规页面状态分离，避免整页重组。
- 播放 URL 是短期资源；失效刷新遵守现有一次刷新和类型化错误策略，不长期持久化为可靠地址。

## 验证

- 为队列状态机、播放模式、恢复、SourceResolver 和失败分支添加或更新单元测试。
- 涉及后台、通知、音频焦点或进程恢复时列出设备验证；只使用用户指定真机。
- 架构边界变化调用 `$moekoe-architecture`；完成前调用 `$moekoe-validate-change`。
