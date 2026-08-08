# 个人主页页面结构漏检证据（2026-08-08）

## 原始切片

- 修复前提交：`6d31ef76ea80e0ab7804ebac9e268d969a5925d6`。
- 源文件：`feature/profile/src/main/kotlin/cn/james/music/feature/profile/UserProfileScreen.kt`。
- `git show 6d31ef7:feature/profile/src/main/kotlin/cn/james/music/feature/profile/UserProfileScreen.kt | wc -l` 输出 `672`。
- 当时页面已经具有 UI contract、截图 reference、局部 fidelity region 和布局 probe；相关视觉、截图、`verifyArchitecture` 与 APK 验证均已通过。
- 用户随后要求复核代码是否符合组件化与通用组件封装规范。

## 可复现源码事实

修复前的同一个文件包含页面 Scaffold、内容列表、Hero、头像与身份、关系统计、编辑按钮、听歌概览、歌单分区、歌单行、加载态、错误态、离线态、test tag 和 Preview。`UserProfileScreen` 的参数包含 `onBack` 以及以下十个页面事件：

```text
onShare, onMore, onEdit, onFollowing, onFollowers,
onFriends, onViewAll, onPlaylist, onPlaylistMore, onRetry
```

源码还包含以下可直接检索的实现：

```text
import androidx.compose.material3.Button
Button(onClick = onRetry)
painterResource(playlist.artworkRes)
Modifier.size(52.dp).clip(RoundedCornerShape(12.dp))
```

同期 `docs/UI_COMPONENT_CATALOG.md` 已将 Button 标记为 `Adopt`，音乐内容组件标记为 `Stable`。`core/designsystem` 已存在 `MoeButton` 与 `MoeArtwork`。

## 已存在的边界事实

- `docs/ENGINEERING_STANDARDS.md` 已规定一个 Kotlin 文件只承担一种主要角色；超过 300 行不是唯一失败条件，但需要解释职责，存在两个以上独立变化原因时必须拆分。
- `.agents/skills/moekoe-architecture/SKILL.md` 已要求最小可见性和单一文件职责，同时禁止为形式机械拆分。
- `docs/reference-audits/20-user-profile-ui.md` 明确该阶段是纯 UI 切片，首个切片不新增 Repository、ViewModel、Hilt Module、数据库表或网络调用。
- 公共组件目录明确指出：设计稿中的独立视觉块不等于公共组件；进入公共库需要跨页面消费者或必须全局统一的行为证据。

## 后续修复事实

提交 `44fe95ddf10a54298d501a670fe53100bada7054` 将页面壳保留在 81 行的 `UserProfileScreen.kt`，并按内容、Hero、概览、歌单、页面状态和事件分别建立 Feature 内文件；页面事件收敛为 `UserProfileAction`。错误态改用 `MoeButton`，歌单封面改用 `MoeArtwork`。

修复没有新增 ViewModel、Repository、网络或公共 Design System API。原有视觉 fidelity 指标保持通过，Profile 单测、AndroidTest 编译、Lint、13 张截图、架构校验、UI contract、fidelity、golden 变更校验和 Debug APK 均通过。

## 隔离输入边界

本文件只保存历史源码、既有规范和前后提交的可复现事实。候选流程与预期判断位于独立 candidate 和 eval 文件；评测执行时不得向受测 agent 额外提供修复结论。
