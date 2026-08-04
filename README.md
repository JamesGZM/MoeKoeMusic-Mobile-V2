# MoeKoeMusic Mobile V2

MoeKoeMusic Mobile V2 是 MoeKoeMusic 的 Android 原生实现，计划使用 Kotlin、Jetpack Compose 与 Media3 构建。

项目以桌面端 MoeKoeMusic 的功能和产品体验为主要参考，以现有 React Native Mobile 的移动端数据流程为验证参考，并将 KuGouMusicApi 逐步迁移为 Kotlin 原生实现。正式 App 不依赖 Node、Express、WebView 或自建 API 服务。

## 当前阶段

项目处于设计与技术验证阶段，尚未创建 Android 工程骨架。当前已确定：

- Android 原生 Kotlin + Jetpack Compose。
- 直接访问酷狗官方接口，不依赖自建服务器。
- 以 `KuGouMusicApi@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb` 为协议迁移基准。
- 使用 Media3 `MediaLibraryService` 管理播放生命周期与系统媒体集成。
- 第一版以 PC 端功能对齐为目标，但交互和布局遵循 Android 平台习惯。
- Application ID 为 `cn.james.music`，最低支持 API 26，compile/target SDK 为 API 36。
- 第一版 UI 视觉语言与核心页面设计稿已经确认。
- 本地音乐统一复制导入；外部“打开方式”在导入成功后立即播放。

## 文档索引

- [产品范围](docs/PRODUCT_SCOPE.md)
- [总开发计划](docs/DEVELOPMENT_PLAN.md)
- [分阶段开发文档](docs/plans/00-project-foundation.md)
- [测试策略](docs/TESTING_STRATEGY.md)
- [本地音乐规格](docs/LOCAL_MUSIC.md)
- [软件架构](docs/ARCHITECTURE.md)
- [KuGouMusicApi 迁移](docs/API_MIGRATION.md)
- [工程规范](docs/ENGINEERING_STANDARDS.md)
- [UI 设计讨论稿](docs/UI_DESIGN_BRIEF.md)
- [UI 反馈与 Dialog 组件规范](docs/UI_COMPONENTS.md)
- [参考项目与来源](docs/REFERENCES.md)
- [ADR-0001：原生直连酷狗接口](docs/decisions/0001-native-direct-api.md)
- [ADR-0002：轻量模块化架构](docs/decisions/0002-lightweight-modular-architecture.md)
- [ADR-0003：复制导入本地音乐](docs/decisions/0003-copy-imported-local-music.md)

## 文档维护原则

- 已确定的决定写入对应主文档和 ADR。
- 尚未确定的内容标记为“待讨论”，不能作为实现依据。
- 架构或公共约定发生变化时，同一提交内更新相关文档。
- 外部参考用于学习设计与实现方式，不代表整体照搬。

## 许可证

本项目采用 [GNU General Public License v2.0](LICENSE)，SPDX 标识为 `GPL-2.0-only`。迁移自 KuGouMusicApi 的部分继续保留其 MIT 许可证和来源声明。
