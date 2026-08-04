# 阶段 1：工程基础

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
