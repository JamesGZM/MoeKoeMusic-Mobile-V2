# 搜索结果页视觉与状态边界审计

状态：Approved for implementation。审计日期：2026-08-08。

## 范围与完成语义

本切片把已确认的 [`03-search-results-v2.png`](../design/mockups/03-search-results-v2.png) 落为 Android 原生搜索结果页，并保留现有“首页 → 搜索 → 歌曲搜索 → 播放 → Back”的真实纵向闭环。视觉完成语义包括：搜索型 Toolbar、六分类 Tab、歌手 Hero、歌曲段、歌单与专辑横向内容、分页状态、应用级 MiniPlayer 避让和大字体适配。

本切片不扩展酷狗协议。现有 `SearchRepository` 仍只返回歌曲；歌手、歌单、专辑、歌手关注和 MV 是明确的 UI Model / 事件端口，设计示例只进入截图 fixture，不进入 Repository、领域模型、缓存或生产运行态。

## 既有能力与差距

- `:feature:search` 已有真实歌曲搜索、分页、取消旧请求、类型化失败和点击播放；这些行为与测试必须保留。
- 当前页面是标准标题 Toolbar、独立输入框、按钮和单一歌曲列表，与确认稿的搜索型 Toolbar、分类层级、歌手摘要和横向内容不一致。
- `MoeStandardTopBar` 适用于标题页，不适合把可编辑搜索框塞入中心标题槽；Design System 尚缺一个职责明确的搜索型 Toolbar。
- `MoeSongRow`、MiniPlayer、主题、图片加载和页面状态组件可继续复用，不新增第三方依赖。

## 决策

### 搜索型 Toolbar 公共能力

在 `:core:designsystem` 增加窄职责 `MoeSearchTopBar`：左侧可替换导航按钮，中间为带搜索图标、清除动作、IME Search 的单行输入，右侧可选语音动作。它只拥有外观、Insets、触控区和输入语义，不持有查询状态或发起搜索。

标准标题页面继续使用 `MoeStandardTopBar`；不会把二者合并成参数爆炸的万能 Toolbar。

### UI Model 与真实数据边界

- `SearchUiState` 保留真实歌曲、分页、加载和错误字段，并增加内部展示模型：选中分类、可选歌手摘要、可选歌单/专辑卡片。
- ViewModel 的现有 Repository 映射只填歌曲段；未迁移的结果类型保持空，不注入初音未来、计数、关注状态或示例封面。
- 截图测试使用纯 UI fixture 同时填满歌手、歌曲和歌单/专辑段，以证明确认稿结构已经可交付；后续协议切片只替换数据生产者，不重写页面结构。
- Tab 可切换选中视觉；当前只有“综合/歌曲”有真实歌曲内容。其余分类没有数据时显示明确空态，不伪造请求成功。

## 页面状态与交互矩阵

| 状态 | 页面表达 | 行为 |
| --- | --- | --- |
| 未搜索 | 搜索 Toolbar、分类 Tab 与居中引导 | 输入、清除、IME Search、返回可用 |
| 首次加载 | 固定 Toolbar/Tab，正文稳定加载 | 可返回；新提交取消旧请求 |
| 综合内容 | 可选歌手 Hero、歌曲段、可选歌单/专辑段 | 歌曲播放、分页、分类切换；次级动作上抛事件 |
| 歌曲内容 | 紧凑歌曲列表 | 点击播放与分页保持真实闭环 |
| 空结果 | 保留 Toolbar/Tab，正文明确空态 | 可修改关键词或切换分类 |
| 首次错误 | 保留 Toolbar/Tab，错误与重试 | 重试现有查询 |
| 分页错误 | 保留已有内容，尾部弱错误与重试 | 不清空歌曲 |
| 大字体 | Toolbar 输入、Tab 和内容自然增高/横向滚动 | 不裁切核心文本，不缩小触控区 |

## 模块与安全边界

- `:feature:search` 继续只依赖 `:core:model` 与 `:core:designsystem`；UI 不直连网络、播放 Service 或数据库。
- `:app` 继续传入 `onPlay`，决定 MiniPlayer 和底部安全留白；搜索页是子页面，不显示一级 NavigationBar。
- 不新增权限、语音识别 Intent、网络请求、DataStore、Room 表或第三方库；语音图标首切片只上抛事件，不制造录音权限或假识别。
- 截图 fixture 使用仓库已有本地原创/生成图片的独立副本，不裁剪整张设计稿，不记录真实关键词结果或服务载荷。

## 验收门禁

- 视觉：按 [`../design/SEARCH_LAYOUT_SPEC.md`](../design/SEARCH_LAYOUT_SPEC.md) 将确认稿与 Compose 综合内容态同画布并排、叠加、差异复核。
- 截图：Idle、Loading、Content Light/Dark/AMOLED、Empty、Error、分页错误、`1.5×`、`2.0×`。
- 行为：ViewModel 原有搜索/分页/旧请求隔离单测继续通过；输入清除、IME Search、Tab、歌曲点击和分页有自动证据。
- 真机：仅在 ELE-AL00 / API 29 验证首页进入、真实歌曲搜索、播放、MiniPlayer、分类切换与 Back，不启动模拟器。
- 工程：格式、单测、截图、Lint、AndroidTest 编译和 Debug APK 构建全部通过后独立提交并推送。
