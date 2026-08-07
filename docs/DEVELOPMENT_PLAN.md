# 开发计划

状态：阶段 0、阶段 1、阶段 2、阶段 3 已完成，阶段 4 与阶段 5A/5B 进行中。更新日期：2026-08-07。

本文是 MoeKoeMusic Mobile V2 的总开发路线。仓库已具备 Android 工程基础、原生播放内核和阶段 3 的本地音乐纵向切片；阶段 4 已完成酷狗协议基础、匿名加密会话、正式搜索页面与在线播放纵向闭环。阶段 5 后续执行顺序固定为“品牌与启动基建 → 首页真实闭环 → 我的与登录用户链路 → 播放器完整链路 → 发现页”，不得因某个后续页面已有局部代码而越过前置切片。

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
- 启动遵循 Android 系统 SplashScreen：首帧可绘制后立即进入应用壳，不增加业务启动页、人工延时或跨业务初始化门禁。首页等内容页优先展示上次完整成功缓存并自动后台刷新，刷新失败保留旧内容；只有没有任何缓存时才显示首次加载状态。网络、匿名会话、数据库与缓存状态由实际消费它们的页面管理。
- 页面实现以已批准设计稿和 [`DESIGN_SYSTEM.md`](DESIGN_SYSTEM.md) 为共同验收依据。仓库现有设计图均已确认并可直接实现；只有尚不存在的页面或关键状态才执行“状态清单 → 静态设计图生成 → 用户确认 → 必要时原型 → 文档同步 → Token/组件代码 → 业务页面”。原型不能先于缺失设计图的确认，也不能替代设计图。
- 新行为与对应测试在同一变更中提交；签名、导入、队列和播放模式是强制单测区域。
- 阶段未满足退出条件时，不开始依赖它的下一阶段。
- 协议、模块边界和产品语义变化必须同步更新文档与 ADR。

## 阶段路线

| 阶段 | 目标 | 主要交付物 | 退出条件 | 状态 |
| --- | --- | --- | --- | --- |
| 0 | 文档与决策 | 本计划、测试策略、本地音乐规格、UI 映射、ADR | 文档互相一致且无关键待定项 | 已完成 |
| 1 | 工程基础 | Gradle 多模块、依赖注入、Design System、CI | Debug App 可构建，基础检查通过 | 已完成 |
| 2 | 播放内核 | MediaLibraryService、队列、通知栏、系统控制 | App 管理的测试音频可稳定后台播放 | 已完成 |
| 3 | 本地音乐 | 选择、扫描、外部导入、去重、列表、删除、播放 | “打开方式 = 导入 + 播放”端到端通过 | 已完成 |
| 4 | 酷狗在线闭环 | 匿名注册、搜索、歌曲地址、在线播放 | Node/Kotlin 对照测试与在线播放通过 | 进行中 |
| 5 | 内容与账号 | 首页、发现、详情、播放器、登录、“我的” | 核心 PC 功能在移动信息架构中闭环 | 进行中（5A/5B） |
| 6 | 完整能力与发布 | 歌词、音质、云盘、MV、识曲、自适应与性能 | 发布检查、无障碍和兼容测试通过 | 未开始 |

详细执行说明：

1. [`plans/00-project-foundation.md`](plans/00-project-foundation.md)
2. [`plans/01-playback-core.md`](plans/01-playback-core.md)
3. [`plans/02-local-music.md`](plans/02-local-music.md)
4. [`plans/03-kugou-online-slice.md`](plans/03-kugou-online-slice.md)
5. [`plans/04-content-and-player-ui.md`](plans/04-content-and-player-ui.md)
6. [`plans/05-account-and-my.md`](plans/05-account-and-my.md)
7. [`plans/06-release-readiness.md`](plans/06-release-readiness.md)
8. [`plans/07-login-flow.md`](plans/07-login-flow.md)

以上编号保留历史阶段文档索引，不代表当前实施先后。当前切片顺序固定为：品牌与启动基建、首页真实数据与 UI、“我的”与登录、播放器动态色/歌词/队列、发现与其余详情。存量播放器封面只允许先补真机验收和缺陷修正，不在首页与用户链路完成前继续扩展新页面。

首页真实内容与缓存门禁已经通过，见 [`reference-audits/15-home-content-and-cache.md`](reference-audits/15-home-content-and-cache.md)。首批接入每日推荐、推荐歌单和可选轮播，按协议与 Decoder、Room v5 完整快照、cache-first Repository、ViewModel、Compose 页面拆成原子切片；排行榜与新歌仍归发现页。协议、Room v5、Repository、ViewModel 与 Compose 五个原子切片已完成，缓存按匿名/用户身份分区自动切换，15 分钟内不重复自动刷新，部分或失败响应不会覆盖最后完整快照；页面覆盖首次加载、缓存刷新、部分结果、空内容、错误与身份切换，并已建立浅色、深色、`1.5×`、`2.0×` 字体截图基线。2026-08-06 自动真实测试确认每日推荐与推荐歌单当前可用；轮播返回 `31136`，恢复前不作为完整快照必需区块。

## UI 实现基线

以下设计稿是第一版实现基线，旧稿仅保留讨论记录；`09` 与 `14` 至 `18` 的修订稿已于 2026-08-07 确认并替换正式文件：

| 页面 | 验收设计稿 | 关键约束 |
| --- | --- | --- |
| 首页 | [`01-home-material3-v2.png`](design/mockups/01-home-material3-v2.png) | 浅色蓝色体系；搜索从首页进入 |
| 发现 | [`02-discover-v2.png`](design/mockups/02-discover-v2.png) | 不显示重复标题行；分类 Tab 置顶 |
| 搜索 | [`03-search-results-v2.png`](design/mockups/03-search-results-v2.png) | 独立子页面，不属于底部导航 |
| 我的 | [`04-my-v4.png`](design/mockups/04-my-v4.png)、[`04-my-anonymous.png`](design/mockups/04-my-anonymous.png) | 已登录与匿名主态；账号资产统一门禁，匿名态不展示假计数或重复提示 |
| 歌单详情 | [`05-playlist-detail.png`](design/mockups/05-playlist-detail.png) | Material 3 详情结构 |
| 播放封面 | [`06-player-cover.png`](design/mockups/06-player-cover.png) | 参考 Kreate 的沉浸氛围，不照搬其品牌元素 |
| 播放歌词 | [`07-player-lyrics.png`](design/mockups/07-player-lyrics.png) | 与封面共享手势和播放控制 |
| 播放队列 | [`08-player-queue.png`](design/mockups/08-player-queue.png) | 手机使用 Material 3 Bottom Sheet |
| 设置 | [`09-settings.png`](design/mockups/09-settings.png) | 内容沿用分组式可滚动设置页；Toolbar 以 `10-user-profile` 为基准居中；长画布不压缩下方 Item |
| 用户主页 | [`10-user-profile.png`](design/mockups/10-user-profile.png) | 与“我的”分离，不重复工具入口 |
| Dialog | [`11-dialog-components.png`](design/mockups/11-dialog-components.png) | 统一形状、操作顺序和遮罩 |
| 反馈组件 | [`12-feedback-components-v2.png`](design/mockups/12-feedback-components-v2.png) | 自定义 Snackbar、受限 MoeToast |
| 登录主状态 | [`13-login-phone-immersive.png`](design/mockups/13-login-phone-immersive.png) | 沉浸式顶部；无重复标题；三种登录方式共享表单结构 |
| 基础 Token | [`14-design-foundations.png`](design/mockups/14-design-foundations.png) | 数值仍以 `DESIGN_SYSTEM.md` 为准 |
| Toolbar 与导航 | [`15-toolbar-navigation.png`](design/mockups/15-toolbar-navigation.png) | 标准态页面中心标题、可替换导航图标；沉浸式仅作 Hero/封面例外 |
| 操作与输入 | [`16-actions-inputs.png`](design/mockups/16-actions-inputs.png) | Material Icons 优先；Filled 禁用态与登录确认稿一致；输入图标可见方形背景不填满输入框高度 |
| 音乐内容组件 | [`17-music-content-components.png`](design/mockups/17-music-content-components.png) | 保持紧凑列表与连续 MiniPlayer，不将所有内容卡片化 |
| 页面状态与覆盖层 | [`18-mobile-states-overlays.png`](design/mockups/18-mobile-states-overlays.png) | 系统居中白色 Dialog、Tonal 取消与贴底 Bottom Sheet |
| 密码登录与风控 | [`login-v2` 已确认稿](design/mockups/candidates/login-v2/README.md#19--密码登录) | 凭据错误内联，风险确认使用居中 Dialog |
| 扫码登录状态 | [`login-v2` 已确认稿](design/mockups/candidates/login-v2/README.md#20--扫码登录) | 生成、等待、已扫码、过期和失败分别建模 |
| 登录安全验证 | [`login-v2` 已确认稿](design/mockups/candidates/login-v2/README.md#21--风险验证) | 短信 Dialog 与隔离腾讯验证保持安全边界 |
| 手机号多账号 | [`login-v2` 已确认稿](design/mockups/candidates/login-v2/README.md#22--多账号) | 只在多账号响应后展示，不自动选择 |

旧登录流程 `19` 至 `22` 号横向设计稿的实现基线资格已撤销；[`login-flow`](design/prototypes/login-flow/README.md) 仅保留为历史交互参考。新版独立单状态图已经确认，可直接用于 Compose 视觉返工；只有发现现有图片未覆盖的新状态时才补图并等待确认。

登录纵向闭环的协议、会话、安全和成熟库选型审计已经通过，见 [`reference-audits/09-login-session-and-risk.md`](reference-audits/09-login-session-and-risk.md)。实施按协议基础、短信/多账号、密码/安全验证、扫码和整体验收拆为原子提交；当前验收只使用已连接的 API 29 真机，不创建或启动模拟器。
短信/多账号、密码、安全验证与扫码五态已经进入 `:feature:login` Compose 功能实现；协议、状态机、安全边界和既有真机自动结果继续作为行为证据。2026-08-07 复核确认，旧截图只验证实现自身稳定，尚未证明与 `852 × 1846` 确认稿的尺寸和锚点一致，因此登录视觉完成结论撤回。后续必须先按 [`LOGIN_LAYOUT_SPEC.md`](design/LOGIN_LAYOUT_SPEC.md) 从手机号验证码主状态建立统一比例映射与设计稿叠加门禁，再逐状态重新验收；在此之前不更新截图基准、不宣称视觉完成。真实验证码、密码/风控和另一台设备扫码仍属于用户主动验收项。

“我的”首个账户纵向切片已经贯通 `:kugou-api`、`:data` 与 `:feature:my`：按 `04-my-v4.png` 展示真实用户资料和 VIP 摘要，覆盖匿名、加载、部分失败、刷新与退出确认，不用假资产计数填补尚未迁移的接口；匿名、已认证和 `1.5×` 字体截图基准及 API 29 真机导航验证已经通过。密码与安全验证已完成协议、领域、一次重试状态机和隔离腾讯验证容器；二维码已完成协议、领域、2 秒生命周期轮询、120 秒本地过期截止、连续三次失败恢复和五态功能行为，并通过 API 29 真机入口、真实 key、离页和截图解码门禁。腾讯 Activity 非导出门禁也已通过；这些结果不替代登录页面的新设计符合度门禁。真实扫码状态 `2→4`、会话恢复和真实风控域名/票据兼容仍需用户主动验收。签到、VIP 领取和音乐库资产属于后续切片。

底部导航固定为“首页、发现、我的”。搜索和用户主页是子页面；播放器由歌曲、MiniPlayer 或系统恢复入口进入。

`:core:designsystem` 已完成 Primary、Typography、Spacing、Shapes、Dimensions、TopBar、`MoeSnackbar` 和第一批音乐内容组件校准；Section Header、封面、徽标、歌曲行、MiniPlayer 与队列行已接入搜索、本地音乐和应用播放壳，并通过浅色、深色、`1.5×` 字体截图及 API 29 真机回归。按钮、输入、通用 Bottom Sheet、MoeToast 和页面状态组件仍需按 `DESIGN_SYSTEM.md` 与确认设计稿继续实现。无版权、VIP、网络、会话与协议错误已接入根层类型化 Snackbar，播放地址失效最多刷新一次；真实 CDN 过期与稀有服务错误样本仍需在后续兼容性验收中补证。

全屏播放器首个封面切片已按 `06-player-cover.png` 接入独立 `:feature:player`：MiniPlayer 进入后隐藏一级导航与自身，退出恢复来源页面；封面失败、未知时长、暂停、缓冲、控制器未连接、空播放项和 `1.5×`/`2.0×` 字体均有截图基准。该切片不提前实现歌词、收藏、下载或分享；播放命令、返回优先级和小可用高度溢出滚动仍需按真机清单手动验收。

本地音乐列表、设备扫描、多选导入、外部打开进度、批量结果及异常状态已建立基础 Compose 实现，并沿用现有 Material 3 token。本地音乐空状态、内容状态和 `1.5×` 字体导入状态已建立稳定截图基准；API 26、29、32、33、36 当前代码设备矩阵已通过。五种目标格式、损坏输入、重复内容、部分成功、取消清理和中断遗留 `.partial` 恢复已通过真实 ContentResolver、WorkManager 与 Room 管线测试，阶段 3 退出条件已满足。

## 阶段质量门槛

- 运行受影响模块的最快单元测试，再运行对应完整检查。
- UI 变化至少提供 Preview、Compose UI 测试或截图测试之一；核心页面必须有截图基准。
- API 迁移必须有固定输入的 Node/Kotlin 对照结果，不以线上返回作为普通单测依赖。
- 本地音乐必须覆盖 API 26、32、33、36 的权限与外部 Intent 场景。
- 每个阶段提交说明包含目标、变更、验证命令、风险和未执行项。

完整测试要求见 [`TESTING_STRATEGY.md`](TESTING_STRATEGY.md)，本地音乐的产品与技术细节见 [`LOCAL_MUSIC.md`](LOCAL_MUSIC.md)。
