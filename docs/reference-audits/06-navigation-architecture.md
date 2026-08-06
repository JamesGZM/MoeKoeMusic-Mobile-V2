# 应用导航架构参考审计

状态：Accepted。审计日期：2026-08-05。

## 决策范围

本审计只决定应用级路由、底部一级导航、子页面返回栈、参数传递、状态恢复和 Debug 页面隔离。页面视觉与业务数据不属于本次决策。

## Android 官方基线

- 使用 Navigation Compose `2.9.8`，不再以 Compose `remember` 枚举模拟路由。
- 无参数目的地使用 `@Serializable data object`，带参数目的地使用 `@Serializable data class`。
- 参数由 `NavBackStackEntry.toRoute()` 或 `SavedStateHandle.toRoute()` 读取，不传递完整业务对象。
- 一级目的地使用嵌套 Graph；切换底部 Tab 时使用 `popUpTo(graph.findStartDestination().id) + saveState + restoreState + launchSingleTop`。`popUpTo` 必须指向最终起始 Destination，而不是外层嵌套 Graph。
- 返回动作优先 `NavController.popBackStack()`，不把目标页写死为某个 Tab。
- `:app` 创建并持有应用级 `NavController`、`NavHost` 与 Scaffold；各 `:feature:*` 模块只向 `NavGraphBuilder` 暴露自己的注册入口。

官方依据：

- [Type safety in Kotlin DSL and Navigation Compose](https://developer.android.com/guide/navigation/design/type-safety)
- [Design your navigation graph](https://developer.android.com/guide/navigation/design)
- [Navigation release notes](https://developer.android.com/jetpack/androidx/releases/navigation)

## Kreate

- 仓库：`knighthat/Kreate`
- 固定提交：`f02577e862318df26b13abb02d14d8d824a1b947`
- 文件：`composeApp/src/androidMain/kotlin/it/fast4x/rimusic/ui/screens/AppNavigation.kt`
- 许可证：GPL-3.0；仅学习结构，不复制代码。

采用：真实 `NavHost`、队列等临时界面作为导航目的地、返回时弹出 back stack、MiniPlayer 位于页面内容之外。

拒绝：字符串路由、手写路径参数、`arguments!!`、单一巨大导航文件和把偏好设置直接混入导航构建。

## Metrolist

- 仓库：`MetrolistGroup/Metrolist`
- 固定提交：`289ed45d0a429e6e4f19a2a07543b243a0ab2e8b`
- 文件：`app/src/main/kotlin/com/metrolist/music/MainActivity.kt`、`ui/screens/NavigationBuilder.kt`、`ui/screens/Screens.kt`
- 许可证：GPL-3.0；仅学习结构，不复制代码。

采用：底部导航切换时保存和恢复目的地状态、重复点击当前 Tab 的独立语义、NavHost 与 MiniPlayer/NavigationBar 的层级关系。

拒绝：字符串 Route、Activity 内超大导航装配、页面通过共享字符串识别当前目的地。

## 本项目决策

Feature 模块粒度、文件职责与 Now in Android 对照见 [`08-feature-modularization.md`](08-feature-modularization.md)。本文件只决定导航行为；不再以综合 `:features` 模块作为最终组织方式。

```text
:app
  MoeKoeApp + rememberNavController + NavHost + BottomBar + MiniPlayer
      ↓ NavGraphBuilder 扩展
:feature:home / discover / my
  各自拥有一级 Graph
:feature:search / localmusic / foundation
  各自拥有子页面目的地与注册函数
```

- 底部只显示首页、发现、我的；搜索属于首页子路径，本地音乐和设备扫描属于“我的”子路径。
- Debug 实验台只在 Debug 构建注册，不进入 Release 导航图。
- Bottom Sheet 队列当前是应用级临时 UI 状态，不伪装为一级页面；正式播放器阶段再决定是否建模为 destination。
- 导航 Route 不携带显示文案；文案始终来自资源。
- 一级 Tab 根页面不形成按点击先后排列的历史；`首页 → 发现 → 我的 → Back` 返回首页。发现、我的各自的子页面栈仍独立保存并在重新选择该 Tab 时恢复。
- Tab 恢复不得触发页面首次加载闪烁。ViewModel 可在生命周期恢复时校验会话或刷新，但必须保留已经得到的正常或匿名内容，把刷新建模为后台刷新而不是把状态重置为 Loading。

## 验收矩阵

- 搜索页系统返回键回到实际来源页。
- 本地音乐 → 设备扫描 → 返回，仍保留本地音乐页面状态。
- 三个底部 Tab 往返后分别恢复栈和页面状态。
- 首页 → 发现 → 我的后按系统 Back 直接返回首页，不回放 Tab 点击历史。
- 已确认匿名的“我的”在离开并返回时持续显示登录卡片，不闪回骨架或全屏 Loading。
- 重复点击当前 Tab 不堆叠重复目的地。
- Activity 重建后恢复当前目的地。
- Release 导航图不存在播放实验台。
- UI 测试使用 `hasRoute<T>()` 断言，不比较 Route 字符串。

## 实施验证

2026-08-05 已在 Huawei ELE-AL00（API 29）执行 `:app:connectedDebugAndroidTest`，13 项全部通过。新增测试验证首页进入搜索后由系统返回栈回到真实来源；正式底部导航只保留首页、发现、我的，Debug 实验台仅在 Debug 图中可达。
