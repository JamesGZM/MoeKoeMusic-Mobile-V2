# 阶段 1：工程基础

状态：已完成。完成日期：2026-08-04。

## 目标

建立可安装、可测试、边界清晰的 Android 原生工程，为后续播放和本地音乐闭环提供基础设施。

## 实现范围

- 使用 `cn.james.music`、minSdk 26、compile/targetSdk 36 初始化 Kotlin DSL 工程。
- 建立 `:app`、`:core:model`、`:core:common`、`:core:designsystem`、`:core:database`、`:kugou-api`、`:data`、`:playback`、`:features`。
- 集中管理插件和依赖版本，使用 Hilt 作为组合根。
- 建立浅色、深色、纯黑 ColorScheme，以及排版、间距、圆角、图标、阴影和动效 token。
- 配置单元测试、Compose UI 测试、截图测试、Lint、格式检查与 CI。
- Debug App 只展示 Design System 示例，不创建业务空页面。

## 测试

- 模块依赖方向检查。
- Design System 组件 Preview/截图基线。
- 主题、字体缩放和语义 token 的基础 UI 测试。
- CI 从干净环境完成 Debug 构建和单元测试。

## 完成标准

- Debug App 可在 API 26 和 API 36 安装启动。
- 所有模块可独立编译且无反向依赖。
- 颜色和尺寸只由 `:core:designsystem` 提供。
- CI 检查稳定通过，未包含真实凭据或网络业务。

## 实现结果

- 已创建九个规划模块和三个 `build-logic` Convention Plugin，Android 模块使用 AGP 9 内置 Kotlin。
- `:app` 已接入 Hilt、edge-to-edge 和 Design System Showcase；Manifest 未暴露媒体、分享、外部打开或后台服务能力。
- `:core:designsystem` 已提供浅色、深色、AMOLED 主题、排版、形状、间距和扩展语义色。
- 已生成三套主题截图参考图，并加入 Compose UI smoke test 与 GitHub Actions。
- 正式产品名已统一为 MoeKoe Air，并接入 PC/Mobile 同源女孩耳机 Logo 的 legacy、round、Adaptive、monochrome Launcher 资源与 AndroidX 系统 SplashScreen；系统 Splash 后直接进入应用壳，不再规划条件启动门禁。
- 已通过格式检查、单元测试任务、Lint、截图验证、Debug APK 和测试 APK 编译。

## 兼容性说明

- Hilt 计划版本 `2.57.1` 不支持 AGP 9，工程使用首个明确支持 AGP 9 的兼容线 `2.59.2`。
- Core KTX `1.19.0` 与 Lifecycle `2.11.0` 要求 compileSdk 37；为坚持 API 36，分别使用 `1.18.0` 与 `2.10.0`。
- Compose Preview Screenshot Testing `0.0.1-alpha15` 的截图任务暂不兼容 Configuration Cache，仅截图更新和验证命令使用 `--no-configuration-cache`。
- API 26 与 API 36 的安装启动仍由设备矩阵执行；当前提交已完成 APK 与 instrumentation test APK 编译。2026-08-05 已在一台解锁的 API 29 真机上，以 1.0×、1.5× 和 2.0× 字体缩放通过 MainActivity Compose UI smoke test。
- 2026-08-06 已在指定 ELE-AL00 / API 29 真机完成 MoeKoe Air APK 安装、桌面名称与正式图标、冷/热启动及无崩溃复验；该设备不提供可用录屏命令，连续截图未留住瞬时系统 Splash 画面。Android 12+ Splash、Android 13+ 主题图标与锁屏场景仍未验证，且未使用模拟器替代。
- 直接启动策略校准后，同一真机 5 次 `am force-stop` + `am start -W` 的 Launcher 冷启动 `TotalTime` 为 `1927–1990 ms`，平均约 `1961 ms`，未发现 `AndroidRuntime`/`FATAL EXCEPTION`。该结果是当前 Debug 构建实测，不替代首页真实缓存落地后的 Macrobenchmark 与 Release 性能验收。
