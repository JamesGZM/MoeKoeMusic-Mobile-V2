# UI 公共组件目录

状态：进行中。更新日期：2026-08-07。

本目录记录 `:core:designsystem` 公共组件的复用证据、所有权和成熟度。准入规则与完整结论见 [`reference-audits/17-design-system-components.md`](reference-audits/17-design-system-components.md)。设计稿中出现一个独立视觉块，不等于它应当成为公共组件。

## 状态定义

- `Stable`：API、组件测试和至少两个页面消费者已验证。
- `Adopt`：复用证据成立，正在建立或迁移公共 API。
- `Candidate`：设计语义明确，但尚缺第二个消费者或实现证据。
- `Feature`：页面或业务专属，明确不进入公共库。

## 目录

| 组件族 | 包 | 状态 | 已确认消费者/证据 | 下一步 |
| --- | --- | --- | --- | --- |
| Theme / Token | 根包 | Stable | 全部页面 | 保持语义化，禁止页面覆盖全局 Density |
| Navigation back icon | `component.navigation` | Stable | 标准 Toolbar、沉浸式 Toolbar、登录 Hero | 统一 SVG/Vector 路径和 RTL；外围按钮组合归各 Toolbar/页面 |
| Standard TopBar | `component.navigation` | Stable | `10-user-profile`、`15-toolbar-navigation`、搜索和本地音乐 | 页面中心锚定标题；调用方只替换返回/关闭等可见导航图标，公共组件持有点击、`48dp` 触控区、颜色和 Insets |
| Immersive TopBar | `component.navigation` | Adopt | 登录、播放器、Hero 详情确认稿 | 页面接入时验证前景对比和 Insets |
| Search / Selection TopBar | `component.navigation` | Adopt | 搜索、本地音乐多选 | 按各自语义建立窄 API |
| Button | `component.action` | Adopt | 登录、搜索、本地音乐、我的、播放器 | 建立五类操作与 48/56dp 具名尺寸 |
| IconButton | `component.action` | Adopt | Toolbar、播放器、卡片操作 | 统一 plain/tonal/immersive 语义 |
| TextField | `component.input` | Adopt | 登录、搜索、导入、输入 Dialog | 建立 Default/Compact 与错误/密码状态 |
| Alert Dialog / Dialog shell | `component.overlay` | Stable | 退出、删除、登录风控及组件矩阵 | 保持业务状态机在 Feature |
| Input Dialog | `component.overlay` | Candidate | 新建/重命名与登录结构化输入设计 | 首个正式文本输入消费者落地时实现 |
| Bottom Sheet | `component.overlay` | Candidate | 队列、音质/排序选择 | 随首个正式页面切片实现 |
| Snackbar Host | `component.feedback` | Adopt | 应用壳与可恢复操作反馈 | 增加队列、去重和 MiniPlayer 避让 |
| Toast Host | `component.feedback` | Candidate | 仅低优先级无操作反馈 | 第二个明确消费者出现后实现 |
| Page State | `component.state` | Candidate | 首页、搜索、本地音乐的加载/空/错误 | 随页面状态统一切片实现 |
| Music content | `component.media` | Stable | 首页、搜索、本地音乐、队列 | 保持现有语义，后续只整理包路径 |
| Login layout / Hero / Card | `feature.login` | Feature | 仅登录确认稿 | 保持页面设计坐标和组合所有权 |
| Login mode selector | `feature.login` | Feature | 仅登录 | 第二个相同语义消费者出现前不抽取 |
| QR / risk / account states | `feature.login` | Feature | 登录业务状态 | 仅组合公共操作、输入和模态外壳 |

## 变更规则

- 新增公共组件时，必须在表中补充至少两个消费者，或记录必须全局统一的行为约束。
- 新增变体时，必须有已确认设计或既有消费者；禁止为未来可能需求预留布尔参数。
- 公共组件发生视觉或行为变化时，先更新组件测试，再更新消费页面证据。
- 页面局部适配不得反向修改全局 Token；冲突应记录在页面适配契约中。
