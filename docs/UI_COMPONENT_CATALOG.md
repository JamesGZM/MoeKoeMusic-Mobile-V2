# UI 公共组件目录

状态：进行中。更新日期：2026-08-09。

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
| Standard TopBar | `component.navigation` | Stable（本阶段生产接口迁移） | `10-user-profile`、`15-toolbar-navigation`、设置、歌单详情和本地音乐 | 本阶段迁移受控 title/navigation/action data/action ID 与全部消费者；确认的自定义 Close 导航图形保留受控 Slot，像素不变。 |
| Immersive TopBar | `component.navigation` | Adopt | 登录、播放器、Hero 详情确认稿 | 页面接入时验证前景对比和 Insets |
| Search / Selection TopBar | `component.navigation` | Adopt（本阶段生产接口迁移） | 搜索、本地音乐多选 | 本阶段让 Search 接收受控 Voice 等 action data/action ID 并迁移消费者；不保留任意 trailing Slot，像素不变。 |
| Button | `component.action` | Adopt | 登录、搜索、本地音乐、我的、播放器 | 建立五类操作与 48/56dp 具名尺寸 |
| IconButton | `component.action` | Adopt | Toolbar、播放器、卡片操作 | 统一 plain/tonal/immersive 语义 |
| TextField | `component.input` | Adopt | 登录、搜索、导入、输入 Dialog | 建立 Default/Compact 与错误/密码状态 |
| Alert Dialog / Dialog shell | `component.overlay` | Stable（本阶段生产接口迁移） | 退出、删除、登录风控及组件矩阵 | 本阶段迁移简单 Alert 的 `MoeAlertDialogUiModel/Event` 与消费者；复杂 Dialog 保持受控内容 Slot，业务状态机留 Feature。 |
| Input Dialog | `component.overlay` | Candidate | 新建/重命名与登录结构化输入设计 | 首个正式文本输入消费者落地时实现 |
| Bottom Sheet | `component.overlay` | Candidate | 队列、音质/排序选择 | 随首个正式页面切片实现 |
| Snackbar Host | `component.feedback` | Adopt（本阶段生产接口迁移） | 应用壳与可恢复操作反馈 | 本阶段迁移 `MoeSnackbarUiModel/Event` 与消费者，收敛 message/tone/可选 action 数据。 |
| Toast Host | `component.feedback` | Candidate | 仅低优先级无操作反馈 | 第二个明确消费者出现后实现 |
| Page State | `component.state` | Candidate | 首页、搜索、本地音乐的加载/空/错误 | 随页面状态统一切片实现 |
| MoeSongRow / media badge / More | `component.media` | Stable（legacy 调用和 API 冻结） | 首页、搜索、本地音乐、扫描、歌单、队列 | 当前只建立 Feature 内 mapper/typed action 并继续调用 legacy wrapper；最终不可变歌曲展示数据和 `More(id)` 迁移属于当前十项后的独立 UI 阶段。图片只保留固定容器 renderer。 |
| MiniPlayer | `component.media` | Stable（本阶段生产接口迁移） | 应用播放壳、首页/子页面避让 | 本阶段迁移 `MoeMiniPlayerUiModel`/`MoeMiniPlayerEvent` 与 app 消费者；播放状态仍由 `:playback`/`:app` 所有。 |
| Login layout / Hero / Card | `feature.login` | Feature | 仅登录确认稿 | 保持页面设计坐标和组合所有权 |
| Login mode selector | `feature.login` | Feature | 仅登录 | 第二个相同语义消费者出现前不抽取 |
| QR / risk / account states | `feature.login` | Feature | 登录业务状态 | 仅组合公共操作、输入和模态外壳 |

## 变更规则

- 新增公共组件时，必须在表中补充至少两个消费者，或记录必须全局统一的行为约束。
- 新增变体时，必须有已确认设计或既有消费者；禁止为未来可能需求预留布尔参数。
- 公共组件发生视觉或行为变化时，先更新组件测试，再更新消费页面证据。
- 页面局部适配不得反向修改全局 Token；冲突应记录在页面适配契约中。
- 数据接口迁移遵循 [`reference-audits/23-data-driven-ui-architecture.md`](reference-audits/23-data-driven-ui-architecture.md)：当前阶段只冻结 SongRow legacy API 与全部已确认视觉；MiniPlayer、Snackbar/AlertDialog、Standard/Search TopBar 按批准的原子切片迁移，禁止用 Golden、contract、mask 或截图更新掩盖回归。
