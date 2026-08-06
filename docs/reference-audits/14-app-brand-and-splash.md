# 应用品牌与系统启动页技术参考审计

状态：Accepted。审计日期：2026-08-06。

## 范围与产品决策

本审计只开放正式产品名、Launcher Icon 和 Android 系统 SplashScreen 基建：

- 正式名称统一为 **MoeKoe Air**；
- Launcher 与 Splash 沿用 MoeKoeMusic PC、旧版 Mobile 同源的女孩耳机 Logo；
- 提供普通、圆形、Adaptive 和 Android 13 Monochrome 资源；
- 使用系统启动窗口，不增加独立 Splash Activity，不人为延时，也不等待网络、会话、数据库或缓存。

系统 Splash 结束后直接进入应用壳，不再增加条件启动页面，也不建立 `AppStartupRepository`。匿名会话由发起真实业务请求的 Repository 按需初始化；首页等内容页优先展示上次完整成功缓存并自动后台刷新，刷新失败仍保留旧内容。网络、数据库与缓存的加载、错误、离线和恢复状态由对应 Feature 页面承接。这样允许首屏尽快绘制，并避免一个业务数据源阻断整个应用。

## Android 官方约束与依赖结论

- [Android SplashScreen 迁移指南](https://developer.android.com/develop/ui/views/launch/splash-screen/migrate)要求启动 Activity 在 `super.onCreate()` 前调用 `installSplashScreen()`，并通过 `Theme.SplashScreen` 与 `postSplashScreenTheme` 回到应用主题。额外 Splash Activity 会造成重复启动页，并可能增加启动延迟，因此拒绝采用。
- [AndroidX Core SplashScreen 发布页](https://developer.android.com/jetpack/androidx/releases/core#core-splashscreen-1.2.0)的生产稳定版为 `1.2.0`。本项目 minSdk 26，需要同一套 API 兼容 Android 12 之前与之后的系统行为，因此固定 `androidx.core:core-splashscreen:1.2.0`。
- AndroidX Core SplashScreen 为 Apache-2.0，维护主体为 AndroidX，功能边界明确；不引入第三方启动框架、动画库或网络探针。

`MainActivity` 是唯一 Launcher Activity，独占 Starting Theme。外部音频入口和风险验证码 Activity 保持普通应用主题，避免从业务内跳转时重复显示启动页。系统 Splash 不持有产品初始化状态；首帧可绘制后立刻进入 Compose。`setKeepOnScreenCondition` 不用于等待业务数据。

文件管理器“打开方式”是独立系统入口，不先绕回 Launcher：首页缓存与自动刷新不能延迟或替换 `AudioImportActivity`。冷启动直接显示导入进度；应用已运行时按 `singleTop`/`onNewIntent` 语义复用顶部导入页或建立新的导入页。两种路径都必须在复制和 Room 提交后才通过 `PlaybackController` 播放 App 管理的副本。

## 固定品牌来源

### MoeKoeMusic PC

- 本地固定提交：`52c9833afe2e7fedcba8d5b23ff8d1f9731af73a`；许可证：GPL-2.0-only。
- 来源文件：`build/icons/logo.png`。
- 采用：女孩、耳机、音符及蓝绿渐变的品牌图形；由本项目生成各 Android 密度资源。
- 不采用：Electron 构建、托盘图标与安装器结构。

### MoeKoeMusic Mobile

- 本地固定提交：`ab71195d4cf3297332490fd37704d1ae8973d4c5`；许可证：GPL-2.0-only。
- 来源文件：`assets/images/icon.png`、`assets/images/android-icon-foreground.png`、`assets/images/splash-icon.png` 与 `app.json`。
- `assets/images/icon.png` 与 PC `build/icons/logo.png` 的 SHA-256 均为 `abe17837bbcc725dea0b8e5735c3a640db44237796125dea319c4ded18f678f5`，确认两端使用同一品牌源图。
- 采用：同源 Logo 与浅色启动背景语义。
- 不采用：Expo Splash 插件、React Native Runtime、远程更新与旧包名配置。

品牌资产来自同一 MoeKoeMusic 项目族并按用户决策延续，不从 GPL 项目复制实现代码。Monochrome 层使用品牌音符的单色矢量轮廓，以满足主题图标只能表达透明度遮罩的系统约束。

## 资源与分层

```text
MoeKoe 同源 Logo
  ├─ 各密度 legacy / round PNG
  ├─ Adaptive foreground + 品牌背景色
  ├─ Android 13 monochrome mask
  └─ Theme.SplashScreen animatedIcon
                       ↓
                 MainActivity
        installSplashScreen() → Compose App
```

- `:app` 只声明品牌资源、Starting Theme 和启动 Activity，不访问协议、Room 或 DataStore。
- 系统 Splash 不调用匿名会话初始化器，不通过 keep-on-screen 条件等待远端或本地结果。
- `MainActivity` 仅以 Hilt `Lazy` 持有本地导入入口，避免 Activity 创建时解析 `LocalImportGateway` 及其 Room/WorkManager 依赖；用户实际发起导入时才实例化。
- 不新增跨业务启动 Repository 或启动 ViewModel。匿名会话初始化继续由搜索、登录、资料和在线播放等 Repository 按需触发。
- 页面 ViewModel 分别暴露首次加载、已有内容刷新、离线、错误与缓存内容。已有缓存时先显示内容并自动刷新，刷新错误不清空页面；只有无缓存时才显示首次加载或全屏错误。缓存实现只调整所属 Repository 的数据策略，不改变启动流程。
- Room、DataStore 或缓存异常由所属功能做类型化恢复和反馈，不清空数据，也不把应用壳替换为全局阻断页。

## 验证矩阵

- 静态：Manifest 合并结果必须解析为 `@mipmap/ic_launcher`、`@mipmap/ic_launcher_round`、`@string/app_name` 和 `Theme.MoeKoe.Starting`；所有 Adaptive 资源通过 AAPT 编译。
- 构建：`spotlessCheck`、`:app:lintDebug`、`:app:assembleDebug`。
- API 29 真机：安装、桌面名称与图标、冷启动、热启动、无崩溃；系统 Splash 为瞬时系统窗口，若设备无法录屏且连续截图未捕获，必须记录为未留视觉证据。
- 当前 Debug 实测：ELE-AL00 / API 29 连续 5 次强制停止后的 Launcher 冷启动 `TotalTime` 为 `1927–1990 ms`，平均约 `1961 ms`；首页真实缓存完成后仍需用 Macrobenchmark/Release 构建重新建立启动基线。
- 静态边界：`MainActivity` 不调用 `setKeepOnScreenCondition`，代码库不存在独立 Splash Activity、`AppStartupRepository` 或启动门禁页面；本地导入入口保持惰性解析。
- 外部入口：`AudioImportActivityTest` 在同一真机 5/5 通过，分别覆盖冷启动导入后播放、顶部热启动 `onNewIntent` 同实例复用与非法来源拒绝。
- 后续设备矩阵：Android 12+ 系统 Splash、Android 13+ 主题图标、圆形图标裁切仍待具备对应真机时复验，不使用模拟器替代。
- 当前不执行锁屏验收。

## 门禁结论

品牌来源、稳定依赖、Activity 边界、启动性能和兼容策略无关键待定项。正式启动路径固定为“系统 SplashScreen → 应用壳”；不设计或实现条件启动门禁，业务数据状态继续归对应页面。
