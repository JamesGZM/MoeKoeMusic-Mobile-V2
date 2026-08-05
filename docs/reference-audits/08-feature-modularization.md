# Feature 模块化与文件职责参考审计

状态：Accepted。审计日期：2026-08-05。

## 决策范围

本审计决定业务 Feature 的 Gradle 模块粒度、导航所有权、Route/Screen/ViewModel 分工、应用壳状态和 Kotlin 可见性。它不决定页面视觉、酷狗协议或数据层拆分。

## Android 官方基线

- 按高内聚、低耦合组织模块；模块边界服务于所有权、可见性、构建和测试，不能只按技术层机械切分。
- `app` 是组合根，依赖 Feature；Feature 不反向依赖 `app`，Core 不依赖 Feature。
- 导航目的地由所属 Feature 注册，跨 Feature 只传稳定导航键、参数和事件，不传 ViewModel 或可变 UI 状态。
- Route 获取 ViewModel、收集生命周期感知状态并连接导航事件；Screen 接收不可变值和事件，保持无状态。

官方依据：

- [Guide to Android app modularization](https://developer.android.com/topic/modularization)
- [UI layer architecture](https://developer.android.com/topic/architecture/ui-layer)
- [Navigation type safety](https://developer.android.com/guide/navigation/design/type-safety)

## Now in Android

- 仓库：`android/nowinandroid`
- 固定提交：`7d45eae4f8720a0c77f507712ba2437ff974b6ed`
- 许可证：Apache-2.0。
- 文件：
  - `docs/ModularizationLearningJourney.md`
  - `app/src/main/kotlin/com/google/samples/apps/nowinandroid/ui/NiaApp.kt`
  - `app/src/main/kotlin/com/google/samples/apps/nowinandroid/ui/NiaAppState.kt`
  - `feature/search/api/src/main/kotlin/.../SearchNavKey.kt`
  - `feature/search/impl/src/main/kotlin/.../navigation/SearchEntryProvider.kt`
  - `feature/search/impl/src/main/kotlin/.../SearchScreen.kt`
  - `build-logic/convention/src/main/kotlin/AndroidFeatureApiConventionPlugin.kt`
  - `build-logic/convention/src/main/kotlin/AndroidFeatureImplConventionPlugin.kt`

采用：`app` 只组合功能入口；Feature 拥有导航注册、Route、Screen 和 ViewModel；默认收紧实现可见性；应用壳状态与业务页面状态分离；模块粒度由真实职责决定。

暂不采用：Navigation 3（本项目基线仍是稳定的 Navigation Compose 2.9.8）；每个 Feature 立即拆为 `api/impl`；网络监控、时区、JankStats 等当前没有产品需求的基础设施。

拒绝机械复制 `api/impl` 的理由：当前跨 Feature API 只有导航键和事件，立即把五个小功能翻倍为十个模块会增加构建与维护成本，却没有形成需要隔离的实现依赖。出现跨 Feature 编译依赖、替换实现或多 App 复用时再拆分。

## Kreate

- 固定提交：`f02577e862318df26b13abb02d14d8d824a1b947`
- 文件：`composeApp/src/androidMain/kotlin/it/fast4x/rimusic/ui/screens/AppNavigation.kt`
- 许可证：GPL-3.0，仅学习结构。

采用：播放器壳位于页面内容之外；播放队列属于壳层临时 UI。

拒绝：单一巨大导航文件、字符串路由以及把页面偏好和导航装配混在一起。

## Metrolist

- 固定提交：`289ed45d0a429e6e4f19a2a07543b243a0ab2e8b`
- 文件：`app/src/main/kotlin/com/metrolist/music/MainActivity.kt`、`ui/screens/NavigationBuilder.kt`、`ui/screens/Screens.kt`
- 许可证：GPL-3.0，仅学习结构。

采用：应用壳统一承载 MiniPlayer 与一级导航，页面通过事件请求导航。

拒绝：Activity 和共享导航文件持续增长、用全局共享状态驱动所有业务页面。

## 本项目决策

```text
:app                       组合根、NavHost、App State、MiniPlayer/Queue
  ├── :feature:home        首页与首页导航图
  ├── :feature:discover    发现页与导航图
  ├── :feature:my          “我的”入口与导航图
  ├── :feature:search      搜索 Route/Screen/ViewModel/导航
  ├── :feature:localmusic  本地音乐 Route/Screen/ViewModel/导航
  └── :feature:foundation  Debug Design System/播放实验台
```

- 删除综合 `:features` 模块；不允许重新建立跨业务的 `MoeKoeAppViewModel`。
- `AppPlaybackViewModel` 只负责应用壳播放器状态和来自在线页面的播放命令。
- `LocalMusicViewModel` 独立拥有本地列表、导入进度、扫描候选和本地播放协调。
- 每个 Feature 的路由键与 `NavGraphBuilder` 扩展是少量公共 API；Screen、UI 辅助组件和具体状态转换默认 `internal/private`。
- 一个文件只承担一种主要角色。Route 与 Screen 在简单功能中可同文件；当文件同时出现导航注册、ViewModel、复杂 Screen 或多个独立组件时必须拆分。
- 暂不增加空的 `api/impl` 模块；触发条件写入工程规范。

## 验收矩阵

- Gradle 依赖图不存在 Feature → App 或 Feature → 其他 Feature 实现的依赖。
- `app` 不获取本地音乐 Repository，不持有本地音乐页面状态。
- 搜索和本地音乐 ViewModel 可分别单测，不需要根 ViewModel。
- 每个 Feature 的导航入口可以在 `app` 独立注册。
- Release 不注册 Foundation 实验台。
- `:app` 只通过 `debugImplementation` 依赖 `:feature:foundation`；Release Source Set 提供空入口并在编译期隔离实验台。
- 系统返回、一级 Tab 状态恢复、MiniPlayer 和本地播放行为保持不变。
- 删除 `:features` 后格式、单测、Lint、截图、APK 和真机导航测试通过。

## 实施验证

2026-08-05 已完成：

- `spotlessCheck`、全量 `testDebugUnitTest`、`lintDebug` 和 `validateDebugScreenshotTest` 通过。
- `assembleDebug` 与串行 `assembleDebugAndroidTest` 通过；并行执行时曾在
  `:feature:discover:packageDebugAndroidTest` 出现一次无诊断信息的瞬时失败，单任务复跑及关闭并行后的完整打包均通过，未因此全局关闭并行构建。
- `:app:compileDebugKotlin` 与 `:app:compileReleaseKotlin` 通过。
- `releaseRuntimeClasspath` 不包含 `:feature:foundation`；Release 在编译期使用空入口，不注册也不打包实验台。
- Huawei ELE-AL00（API 29）执行 `:app:connectedDebugAndroidTest`，13/13 通过。
- 搜索与本地音乐 ViewModel 分模块测试通过；本地播放不再依赖根级业务 ViewModel。
