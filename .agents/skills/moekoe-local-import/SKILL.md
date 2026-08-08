---
name: moekoe-local-import
description: 实现、调试或复核 MoeKoeMusic 本地音乐接入。涉及 feature/localmusic、SAF、内容 URI、ACTION_VIEW、外部音频 Intent、文件复制、哈希、去重、Room 导入事务、清理、回滚、取消、格式 fixture 或导入后播放时使用。
---

# MoeKoe 本地音乐导入

## 读取路由

1. 读取 `docs/LOCAL_MUSIC.md`、`docs/decisions/0003-copy-imported-local-music.md`、`docs/ARCHITECTURE.md` 的本地 Intent 边界、`docs/plans/02-local-music.md`、`docs/reference-audits/03-local-music.md` 和 `docs/TESTING_STRATEGY.md` 的导入矩阵。
2. 数据或模块边界变化同时读取 `docs/ARCHITECTURE.md` 并调用 `$moekoe-architecture`。
3. 导入后播放或外部“打开方式”同时调用 `$moekoe-playback`；用户界面变化调用 `$moekoe-ui-compose`。

## 不变量

- 音乐必须复制到 App 专属音乐目录后才能进入本地库；领域层不暴露绝对路径或依赖外部 URI。
- `ACTION_VIEW` 表示“导入 + 播放”，只有文件与数据库事务提交成功后才能发送播放命令。
- 文件复制使用临时文件、流式哈希和原子提交；失败、取消和重复项必须按规范清理或复用，不留下半成品。
- 不申请 `MANAGE_EXTERNAL_STORAGE`，不为第一版导入申请公共目录写权限，不把 WorkManager 作为默认前台导入方案。
- 明确 URI 权限、格式检测、去重、数据库事务、进程恢复和错误映射边界。

## 验证

- 覆盖复制、哈希、去重、取消、回滚、外部 Intent、格式失败以及成功后播放顺序。
- 使用仓库合成 fixture，不引入第三方音乐或真实用户文件。
- 完成前调用 `$moekoe-validate-change`，设备验收只使用用户指定真机。
