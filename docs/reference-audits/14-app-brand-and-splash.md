# 应用品牌与系统启动页技术参考审计

状态：Accepted。审计日期：2026-08-06。

## 范围与产品决策

本审计只开放正式产品名、Launcher Icon 和 Android 系统 SplashScreen 基建：

- 正式名称统一为 **MoeKoe Air**；
- Launcher 与 Splash 沿用 MoeKoeMusic PC、旧版 Mobile 同源的女孩耳机 Logo；
- 提供普通、圆形、Adaptive 和 Android 13 Monochrome 资源；
- 使用系统启动窗口，不增加独立 Splash Activity，不人为延时，也不等待网络。

初始化、可恢复失败与存储阻断属于系统 Splash 之后的条件启动门禁。仓库目前没有这些状态的已确认设计图，因此本切片不实现；必须先产出静态设计并由用户确认，再接入既有匿名会话初始化器与后续 `AppStartupRepository`。

## Android 官方约束与依赖结论

- [Android SplashScreen 迁移指南](https://developer.android.com/develop/ui/views/launch/splash-screen/migrate)要求启动 Activity 在 `super.onCreate()` 前调用 `installSplashScreen()`，并通过 `Theme.SplashScreen` 与 `postSplashScreenTheme` 回到应用主题。额外 Splash Activity 会造成重复启动页，并可能增加启动延迟，因此拒绝采用。
- [AndroidX Core SplashScreen 发布页](https://developer.android.com/jetpack/androidx/releases/core#core-splashscreen-1.2.0)的生产稳定版为 `1.2.0`。本项目 minSdk 26，需要同一套 API 兼容 Android 12 之前与之后的系统行为，因此固定 `androidx.core:core-splashscreen:1.2.0`。
- AndroidX Core SplashScreen 为 Apache-2.0，维护主体为 AndroidX，功能边界明确；不引入第三方启动框架、动画库或网络探针。

`MainActivity` 是唯一 Launcher Activity，独占 Starting Theme。外部音频入口和风险验证码 Activity 保持普通应用主题，避免从业务内跳转时重复显示启动页。系统 Splash 不持有产品初始化状态；首帧可绘制后立刻进入 Compose。

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
- 系统 Splash 不调用匿名会话初始化器，不通过 keep-on-screen 条件等待远端结果。
- 后续条件门禁的网络失败允许重试或离线进入；存储/迁移失败不得绕过。该状态机必须由 Repository/ViewModel 驱动，不能塞入 `MainActivity`。

## 验证矩阵

- 静态：Manifest 合并结果必须解析为 `@mipmap/ic_launcher`、`@mipmap/ic_launcher_round`、`@string/app_name` 和 `Theme.MoeKoe.Starting`；所有 Adaptive 资源通过 AAPT 编译。
- 构建：`spotlessCheck`、`:app:lintDebug`、`:app:assembleDebug`。
- API 29 真机：安装、桌面名称与图标、冷启动、热启动、无崩溃；系统 Splash 为瞬时系统窗口，若设备无法录屏且连续截图未捕获，必须记录为未留视觉证据。
- 后续设备矩阵：Android 12+ 系统 Splash、Android 13+ 主题图标、圆形图标裁切仍待具备对应真机时复验，不使用模拟器替代。
- 当前不执行锁屏验收。

## 门禁结论

品牌来源、稳定依赖、Activity 边界、启动性能和兼容策略无关键待定项，允许提交正式名称、Launcher Icon 和系统 SplashScreen。条件启动门禁仍关闭实现门禁，直到新增静态设计获得用户确认。
