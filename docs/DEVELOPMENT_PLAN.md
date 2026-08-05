# 开发计划

状态：阶段 0、阶段 1、阶段 2 已完成，阶段 3 进行中。更新日期：2026-08-05。

本文是 MoeKoeMusic Mobile V2 的总开发路线。仓库已具备 Android 工程基础、原生播放内核和阶段 3 的本地音乐纵向切片；酷狗 API 与正式在线业务页面尚未实现。

## 技术基线

- Application ID：`cn.james.music`。
- 最低版本：Android 8.0（API 26）。
- 编译与目标版本：API 36。
- Kotlin、Jetpack Compose、Material 3、Media3、Room、Hilt、Coroutines 与 Flow。
- Android 客户端原生直连酷狗官方接口，不依赖 Node、内嵌 HTTP 服务、WebView API 桥或 JavaScript Runtime。
- 酷狗协议以 `KuGouMusicApi@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb` 为固定迁移基准。
- 本地音乐导入统一复制到 App 专属目录；外部“打开方式”严格执行“导入成功后立即播放”。

## 开发原则

- 每个阶段形成可运行、可测试的纵向闭环，不先铺满空页面。
- 页面实现以已批准设计稿和 Design System token 为共同验收依据。
- 新行为与对应测试在同一变更中提交；签名、导入、队列和播放模式是强制单测区域。
- 阶段未满足退出条件时，不开始依赖它的下一阶段。
- 协议、模块边界和产品语义变化必须同步更新文档与 ADR。

## 阶段路线

| 阶段 | 目标 | 主要交付物 | 退出条件 | 状态 |
| --- | --- | --- | --- | --- |
| 0 | 文档与决策 | 本计划、测试策略、本地音乐规格、UI 映射、ADR | 文档互相一致且无关键待定项 | 已完成 |
| 1 | 工程基础 | Gradle 多模块、依赖注入、Design System、CI | Debug App 可构建，基础检查通过 | 已完成 |
| 2 | 播放内核 | MediaLibraryService、队列、通知栏、系统控制 | App 管理的测试音频可稳定后台播放 | 已完成 |
| 3 | 本地音乐 | 选择、扫描、外部导入、去重、列表、删除、播放 | “打开方式 = 导入 + 播放”端到端通过 | 进行中 |
| 4 | 酷狗在线闭环 | 匿名注册、搜索、歌曲地址、在线播放 | Node/Kotlin 对照测试与在线播放通过 | 未开始 |
| 5 | 内容与账号 | 首页、发现、详情、播放器、登录、“我的” | 核心 PC 功能在移动信息架构中闭环 | 未开始 |
| 6 | 完整能力与发布 | 歌词、音质、云盘、MV、识曲、自适应与性能 | 发布检查、无障碍和兼容测试通过 | 未开始 |

详细执行说明：

1. [`plans/00-project-foundation.md`](plans/00-project-foundation.md)
2. [`plans/01-playback-core.md`](plans/01-playback-core.md)
3. [`plans/02-local-music.md`](plans/02-local-music.md)
4. [`plans/03-kugou-online-slice.md`](plans/03-kugou-online-slice.md)
5. [`plans/04-content-and-player-ui.md`](plans/04-content-and-player-ui.md)
6. [`plans/05-account-and-my.md`](plans/05-account-and-my.md)
7. [`plans/06-release-readiness.md`](plans/06-release-readiness.md)

## UI 实现基线

以下设计稿是第一版实现基线，旧稿仅保留讨论记录：

| 页面 | 验收设计稿 | 关键约束 |
| --- | --- | --- |
| 首页 | [`01-home-material3-v2.png`](design/mockups/01-home-material3-v2.png) | 浅色蓝色体系；搜索从首页进入 |
| 发现 | [`02-discover-v2.png`](design/mockups/02-discover-v2.png) | 不显示重复标题行；分类 Tab 置顶 |
| 搜索 | [`03-search-results-v2.png`](design/mockups/03-search-results-v2.png) | 独立子页面，不属于底部导航 |
| 我的 | [`04-my-v4.png`](design/mockups/04-my-v4.png) | 用户卡片、签到、VIP、收藏与关注入口完整 |
| 歌单详情 | [`05-playlist-detail.png`](design/mockups/05-playlist-detail.png) | Material 3 详情结构 |
| 播放封面 | [`06-player-cover.png`](design/mockups/06-player-cover.png) | 参考 Kreate 的沉浸氛围，不照搬其品牌元素 |
| 播放歌词 | [`07-player-lyrics.png`](design/mockups/07-player-lyrics.png) | 与封面共享手势和播放控制 |
| 播放队列 | [`08-player-queue.png`](design/mockups/08-player-queue.png) | 手机使用 Material 3 Bottom Sheet |
| 设置 | [`09-settings.png`](design/mockups/09-settings.png) | 分组式设置页 |
| 用户主页 | [`10-user-profile.png`](design/mockups/10-user-profile.png) | 与“我的”分离，不重复工具入口 |
| Dialog | [`11-dialog-components.png`](design/mockups/11-dialog-components.png) | 统一形状、操作顺序和遮罩 |
| 反馈组件 | [`12-feedback-components-v2.png`](design/mockups/12-feedback-components-v2.png) | 自定义 Snackbar、受限 MoeToast |

底部导航固定为“首页、发现、我的”。搜索和用户主页是子页面；播放器由歌曲、MiniPlayer 或系统恢复入口进入。

本地音乐列表、设备扫描、多选导入、外部打开进度、批量结果及异常状态已建立基础 Compose 实现，并沿用现有 Material 3 token。本地音乐空状态、内容状态和 `1.5×` 字体导入状态已建立稳定截图基准；完整异常态视觉验收、多格式逐项导入和 API 26 最终复测仍属于阶段 3 退出条件。

## 阶段质量门槛

- 运行受影响模块的最快单元测试，再运行对应完整检查。
- UI 变化至少提供 Preview、Compose UI 测试或截图测试之一；核心页面必须有截图基准。
- API 迁移必须有固定输入的 Node/Kotlin 对照结果，不以线上返回作为普通单测依赖。
- 本地音乐必须覆盖 API 26、32、33、36 的权限与外部 Intent 场景。
- 每个阶段提交说明包含目标、变更、验证命令、风险和未执行项。

完整测试要求见 [`TESTING_STRATEGY.md`](TESTING_STRATEGY.md)，本地音乐的产品与技术细节见 [`LOCAL_MUSIC.md`](LOCAL_MUSIC.md)。
