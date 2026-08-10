# 开发计划

状态：阶段 0、阶段 1、阶段 2、阶段 3 已完成，阶段 4 与阶段 5A/5B 进行中。更新日期：2026-08-10。

本文是 MoeKoeMusic Mobile V2 的总开发路线。仓库已具备 Android 工程基础、原生播放内核和阶段 3 的本地音乐纵向切片；阶段 4 已完成酷狗协议基础、匿名加密会话、正式搜索页面与在线播放纵向闭环。阶段 5 固定的“我的首页 → 通用播放组件 → 首页 → 发现页 → 播放页”确认稿 UI 顺序已经完成，歌单详情与用户主页确认稿纯 UI 也已独立落地；页面视觉不等待对应业务能力，协议与真实行为继续按纵向切片接入。

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

以上编号保留历史阶段文档索引，不代表当前实施先后。当前界面切片顺序以“我的首页 → 通用播放组件 → 首页 → 发现页 → 播放页”为准；不能因为某个业务能力尚未接入而删减已经确认的页面结构，也不能为了视觉预览伪造协议结果。

首页真实内容与缓存门禁已经通过，见 [`reference-audits/15-home-content-and-cache.md`](reference-audits/15-home-content-and-cache.md)。首批接入每日推荐、推荐歌单和可选轮播，按协议与 Decoder、Room v5 完整快照、cache-first Repository、ViewModel、Compose 页面拆成原子切片；排行榜与新歌仍归发现页。协议、Room v5、Repository、ViewModel 与 Compose 五个原子切片已完成，缓存按匿名/用户身份分区自动切换，15 分钟内不重复自动刷新，部分或失败响应不会覆盖最后完整快照；页面覆盖首次加载、缓存刷新、部分结果、空内容、错误与身份切换，并已建立浅色、深色、`1.5×`、`2.0×` 字体截图基线。`home.content.light` 结构 contract 现绑定确认稿、独立六锚点 probe 与全部回归状态，页面编排、顶部 Hero、快捷入口、内容区和状态页已按变化原因拆分。2026-08-06 自动真实测试确认每日推荐与推荐歌单当前可用；轮播返回 `31136`，恢复前不作为完整快照必需区块。

发现页确认稿 UI 已按 [`design/DISCOVER_LAYOUT_SPEC.md`](design/DISCOVER_LAYOUT_SPEC.md) 完成五段 Tab、Hero、三列排行榜、分类胶囊和三列封面，并覆盖浅色、深色、AMOLED、加载、空、错误及两档大字体截图。`discover.content.light` 结构 contract 绑定确认稿、独立七锚点 probe 和 Tab/分类选择恢复截图；页面编排、Hero、榜单、分类、状态页与 fixture/model 已按变化原因拆分，Route 只持有可保存的纯展示选择。当前榜单与分类内容是 UI 层设计预览；真实协议、缓存、详情与播放接入留在后续纵向切片，不影响本轮视觉交付。

歌单详情确认稿 UI 已按 [`design/PLAYLIST_DETAIL_LAYOUT_SPEC.md`](design/PLAYLIST_DETAIL_LAYOUT_SPEC.md) 落入独立 `:feature:playlist`，从发现页使用类型安全子页面导航进入；标准 Toolbar、资料 Hero、五项操作、紧凑歌曲列表、当前项 Surface、应用级 MiniPlayer 避让及返回来源均已覆盖。页面数据为设计预览，真实歌单协议和各操作行为继续独立接入。

用户主页确认稿 UI 已按 [`design/USER_PROFILE_LAYOUT_SPEC.md`](design/USER_PROFILE_LAYOUT_SPEC.md) 落入独立 `:feature:profile`，由已登录“我的”资料区进入类型安全子页面；标准 Toolbar、资料 Hero、关系统计、编辑资料、听歌概览、公开歌单、空态、离线、长文本、宽屏与两档大字体重排均已覆盖，`user-profile.content.light` 结构 contract、三锚点 probe、13 组截图及归一化并排、叠加和差异证据均已通过。页面数据为设计预览，真实关系、听歌统计、编辑、分享与公开歌单协议继续独立接入。

设置页确认稿 UI 已按 `09-settings.png` 完整恢复外观、播放与音质、歌词、存储、其他五个分组及 14 个 Item，并使用统一最小触控高度和单一长页面滚动；主题、关于与默认开启的“播放失败时自动跳过”保持真实交互。`歌词显示` 的“逐字歌词”已由 [`27-lyrics-highlight-mode`](reference-audits/27-lyrics-highlight-mode.md) 准入为 Character/Line 高亮，而非 scroll/single 布局；逐行 Player 与 Radio Dialog `1.0×/2.0×` 仍待用户确认，确认前不进入 UI 实现。自动跳过在下一次不可恢复播放错误生效，关闭时暂停且不取消已开始的地址刷新或已排队恢复；“默认音质”已完成 [`24-default-playback-quality`](reference-audits/24-default-playback-quality.md) 的领域偏好、登录态候选回退、短期 resolved-quality runtime state、真实 Settings Dialog 和 Player/MiniPlayer 实际角标闭环，真实服务与真机验证仍独立。[`25-brand-theme-color`](reference-audits/25-brand-theme-color.md) 的六档全局品牌色预设已完成领域、Design System、app 消费与 Settings 的代码/语义：默认天空蓝严格保持当前 Light/Dark/AMOLED 色表，五种非蓝状态和 Dialog 基线已受限更新；最终视觉门禁和指定真机验证仍待完成。[`26-cache-management`](reference-audits/26-cache-management.md) 的“清理缓存”已完成 Settings 真实闭环与受限视觉验收：确认 Dialog、独立单飞 clear job、锁定提交、类型化 Snackbar/Retry 与 4 秒成功反馈消费均消费既有 repository，且不展示容量；只新增确认 Dialog 的 `1.0×` / `2.0×` 两张 reference，既有 PNG 不变。它仅清首页/歌词 Room 内容缓存与 Coil singleton image cache，绝不清本地音乐、播放恢复、会话、DataStore 或数据库其他表；CacheLimit 因缺少容量/淘汰产品定义而继续 Deferred。指定真机与真实 Coil 生命周期仍独立未验。[`28-playback-fade`](reference-audits/28-playback-fade.md) 已确认“淡入淡出”仍 Deferred：固定 PC/Mobile 没有音频消费者，A 曲目开始/结束淡化、B 暂停/恢复音量 ramp、C 相邻歌曲 crossfade 不可混为一个开关；在用户冻结语义、时长、转换/失败、gapless、音频焦点和 offload 边界前，`Fade` 保持静态不可提交。其余尚无消费者的能力只呈现静态视觉，不持久化假值或展示假缓存容量。

## UI 实现基线

以下设计稿是第一版实现基线，旧稿仅保留讨论记录；`09` 与 `14` 至 `18` 的修订稿已于 2026-08-07 确认并替换正式文件：

| 页面 | 验收设计稿 | 关键约束 |
| --- | --- | --- |
| 首页 | [`01-home-material3-v2.png`](design/mockups/01-home-material3-v2.png)、[`HOME_LAYOUT_SPEC.md`](design/HOME_LAYOUT_SPEC.md) | 浅色蓝色体系；搜索从首页进入；应用壳固定、页面单一纵向滚动 |
| 发现 | [`02-discover-v2.png`](design/mockups/02-discover-v2.png) | 不显示重复标题行；分类 Tab 置顶；布局契约见 [`DISCOVER_LAYOUT_SPEC.md`](design/DISCOVER_LAYOUT_SPEC.md) |
| 搜索 | [`03-search-results-v2.png`](design/mockups/03-search-results-v2.png)、[`SEARCH_LAYOUT_SPEC.md`](design/SEARCH_LAYOUT_SPEC.md) | 独立子页面；真实歌曲搜索保留，其他结果类型先交付 UI Model，不伪造生产数据 |
| 我的 | [`04-my-v4.png`](design/mockups/04-my-v4.png)、[`04-my-anonymous.png`](design/mockups/04-my-anonymous.png) | 已登录与匿名主态；账号资产统一门禁，匿名态不展示假计数或重复提示 |
| 歌单详情 | [`05-playlist-detail.png`](design/mockups/05-playlist-detail.png)、[`PLAYLIST_DETAIL_LAYOUT_SPEC.md`](design/PLAYLIST_DETAIL_LAYOUT_SPEC.md) | Material 3 详情结构；子页面保留 MiniPlayer，不显示一级底栏 |
| 播放封面 | [`06-player-cover.png`](design/mockups/06-player-cover.png)、[`PLAYER_LAYOUT_SPEC.md`](design/PLAYER_LAYOUT_SPEC.md) | 参考 Kreate 的沉浸氛围；完整保留核心控制与次级动作 |
| 播放歌词 | [`07-player-lyrics.png`](design/mockups/07-player-lyrics.png) | 与封面共享手势和播放控制 |
| 播放队列 | [`08-player-queue.png`](design/mockups/08-player-queue.png) | 手机使用 Material 3 Bottom Sheet |
| 设置 | [`09-settings.png`](design/mockups/09-settings.png) | 内容沿用分组式可滚动设置页；Toolbar 以 `10-user-profile` 为基准居中；长画布不压缩下方 Item |
| 用户主页 | [`10-user-profile.png`](design/mockups/10-user-profile.png)、[`USER_PROFILE_LAYOUT_SPEC.md`](design/USER_PROFILE_LAYOUT_SPEC.md) | 与“我的”分离，不重复工具入口；视觉不等待资料扩展接口 |
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
| 登录安全验证 | [`login-v2` 已确认稿](design/mockups/candidates/login-v2/README.md#21--风险验证) | 短信 Dialog；隔离腾讯验证以白色全尺寸 WebView 宿主承接远端滑块 |
| 手机号多账号 | [`login-v2` 已确认稿](design/mockups/candidates/login-v2/README.md#22--多账号) | 只在多账号响应后展示，不自动选择 |

旧登录流程 `19` 至 `22` 号横向设计稿的实现基线资格已撤销；[`login-flow`](design/prototypes/login-flow/README.md) 仅保留为历史交互参考。新版独立单状态图已经确认，可直接用于 Compose 视觉返工；只有发现现有图片未覆盖的新状态时才补图并等待确认。

登录纵向闭环的协议、会话、安全和成熟库选型审计已经通过，见 [`reference-audits/09-login-session-and-risk.md`](reference-audits/09-login-session-and-risk.md)。实施按协议基础、短信/多账号、密码/安全验证、扫码和整体验收拆为原子提交；当前验收只使用已连接的 API 29 真机，不创建或启动模拟器。
短信/多账号、密码、安全验证与扫码五态已经进入 `:feature:login` Compose 功能实现；协议、状态机和安全边界继续作为行为证据。2026-08-09 已按 [`LOGIN_LAYOUT_SPEC.md`](design/LOGIN_LAYOUT_SPEC.md) 重新校准腾讯加载态：标准返回 Toolbar 下只保留白色全尺寸 WebView 宿主与最小 loading fallback，远端滑块 H5/iframe 不再被伪造成原生设计。其余手机号、密码、风险 Dialog、短信风险、腾讯失败返回、扫码五态和多账号三态仍使用各自确认稿，证据索引见 [`login-v2`](design/mockups/candidates/login-v2/README.md)。真实验证码、密码/风控、扫码状态 `2→4`、会话恢复和跨设备兼容仍属于用户主动验收项，不阻塞后续页面视觉开发。

“我的”首个账户纵向切片已经贯通 `:kugou-api`、`:data` 与 `:feature:my`：按 `04-my-v4.png` 展示真实用户资料和 VIP 摘要，覆盖匿名、加载、部分失败、刷新与退出确认，不用假资产计数填补尚未迁移的接口；匿名与已登录确认稿已补齐 [`my-content-2026-08-08.md`](design/evidence/my-content-2026-08-08.md) 的归一化并排、叠加和差异证据，深色与 `1.5×` 字体截图基准及 API 29 真机导航验证继续作为回归门禁。密码与安全验证已完成协议、领域、一次重试状态机和隔离腾讯验证容器；二维码已完成协议、领域、2 秒生命周期轮询、120 秒本地过期截止、连续三次失败恢复和五态功能行为，并通过 API 29 真机入口、真实 key、离页和截图解码门禁。腾讯 Activity 非导出门禁也已通过；真实扫码状态 `2→4`、会话恢复和真实风控域名/票据兼容仍需用户主动验收。签到、VIP 领取和音乐库资产属于后续切片。

底部导航固定为“首页、发现、我的”。搜索和用户主页是子页面；播放器由歌曲、MiniPlayer 或系统恢复入口进入。

`:core:designsystem` 已完成 Primary、Typography、Spacing、Shapes、Dimensions、标准/沉浸式/搜索型 TopBar、`MoeSnackbar` 和第一批音乐内容组件校准；Section Header、封面、徽标、歌曲行、MiniPlayer 与队列行已接入搜索、本地音乐和应用播放壳。MiniPlayer 已用 `17-music-content-components.png` 与首页壳层确认稿完成同视口并排、叠加和差异复核，证据见 [`design/evidence/mini-player-2026-08-08.md`](design/evidence/mini-player-2026-08-08.md)。搜索结果页已经按 `03-search-results-v2.png` 完成搜索 Toolbar、六分类 Tab、歌手摘要、紧凑歌曲行与横向歌单/专辑的纯 UI 复刻，并覆盖浅色、深色、AMOLED、`1.5×`、`2.0×`、空、错误和分页错误截图；真实运行态仍只消费既有歌曲协议，不伪造其他结果类型。按钮、输入、通用 Bottom Sheet、MoeToast 和页面状态组件仍需按 `DESIGN_SYSTEM.md` 与确认设计稿继续实现。无版权、VIP、网络、会话与协议错误已接入根层类型化 Snackbar，播放地址失效最多刷新一次；真实 CDN 过期与稀有服务错误样本仍需在后续兼容性验收中补证。

全屏播放器封面、歌词与队列已分别建立 `player.cover.default`、`player.lyrics.translation`、`player.queue.default` 机器契约，并按 [`PLAYER_LAYOUT_SPEC.md`](design/PLAYER_LAYOUT_SPEC.md)、[`PLAYER_LYRICS_LAYOUT_SPEC.md`](design/PLAYER_LYRICS_LAYOUT_SPEC.md)、[`PLAYER_QUEUE_LAYOUT_SPEC.md`](design/PLAYER_QUEUE_LAYOUT_SPEC.md) 和 `06` 至 `08` 确认稿完成无视觉债务复验。MiniPlayer 进入后隐藏一级导航与自身，退出恢复来源页面；播放、暂停、缓冲、连接中、未知时长、封面失败、歌词多状态、队列空态和大字体均保留截图回归。`:feature:player` 内部已按入口、UI Model、封面、歌词、共享控制、背景、队列 Sheet、队列头部与通用歌曲项适配器拆分；播放状态、封面动态取色、“翻译与音译”和歌词字号偏好、真实歌词均由 `:app` 以纯 UI 输入注入，`AppPlayerLyricsViewModel` 仅在歌词页 settled 可见时读取歌词并将文档与高频 timing 分离；翻译设置关闭时只隐藏已映射 secondary，字号选择只改变歌词视窗既有三档排版，二者均不重新请求。歌词长内容自动跟随有效当前行；用户开始真实纵向拖动后暂停 `3500ms`，恢复时按届时当前行跟随，程序动画、点击 seek 和横向 Pager 不延长暂停。封面成功后只在 Feature 内派生共享色板；队列在非 Shuffle 且至少两项时允许全部稳定项（含当前项）从 `22dp` 行尾点阵的独立 `48dp` 节点长按后拖动，Feature 仅持有临时顺序并在 drop 一次提交，App 以最新权威队列换算 Media3 最终索引，Rejected/stale/权威变化/关闭/超时均回滚，随机队列不伪装为可排序。重试与空态操作统一使用 Design System 按钮；真机触控、惯性和横纵手势仍待验证。

本地音乐列表、设备扫描、多选导入、外部打开进度、批量结果及异常状态已建立基础 Compose 实现，并沿用现有 Material 3 token。2026-08-09 已确认新版本地库与独立导入页：权限成功后自动开始真实逐条扫描，扫描态只读，完成后才进入多选；两类列表统一复用 `MoeSongRow`。本地音乐空状态、内容状态和 `1.5×` 字体导入状态已建立稳定截图基准；API 26、29、32、33、36 当前代码设备矩阵已通过。五种目标格式、损坏输入、重复内容、部分成功、取消清理和中断遗留 `.partial` 恢复已通过真实 ContentResolver、WorkManager 与 Room 管线测试，阶段 3 退出条件已满足。

## 阶段质量门槛

- 运行受影响模块的最快单元测试，再运行对应完整检查。
- UI 变化至少提供 Preview、Compose UI 测试或截图测试之一；核心页面必须有截图基准。
- API 迁移必须有固定输入的 Node/Kotlin 对照结果，不以线上返回作为普通单测依赖。
- 本地音乐必须覆盖 API 26、32、33、36 的权限与外部 Intent 场景。
- 每个阶段提交说明包含目标、变更、验证命令、风险和未执行项。

完整测试要求见 [`TESTING_STRATEGY.md`](TESTING_STRATEGY.md)，本地音乐的产品与技术细节见 [`LOCAL_MUSIC.md`](LOCAL_MUSIC.md)。
