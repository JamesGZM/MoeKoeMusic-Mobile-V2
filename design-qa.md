# “我的首页”设计 QA

- Source visual truth: `docs/design/mockups/04-my-anonymous.png`、`docs/design/mockups/04-my-v4.png`
- Implementation screenshots: `feature/my/src/screenshotTestDebug/reference/cn/james/music/feature/my/MyScreenshotTestKt/MyAnonymousScreenshot_Anonymous_aaadb213_0.png`、`MyAuthenticatedScreenshot_Authenticated_0f7821ba_0.png`
- State: 简体中文、浅色、匿名 / 已登录；另检查匿名深色与匿名 / 已登录 `1.5×` 字体。
- Viewport: Compose Preview `390 × 844 dp`；实现截图 `1024 × 2216 px`，约 `2.626×` density。源图均为 `853 × 1844 px`，比较时按内容宽度归一化，不把像素密度差异作为问题。
- Scope normalization: 源图底部 MiniPlayer 与三项导航属于 app shell，不在 `:feature:my` 截图内；本轮只比较“我的”可滚动内容区域。app shell 将由下一原子切片“通用播放组件”单独对照。

## Full-view comparison evidence

已登录与匿名态均保存了同画布并排、50% 叠加和差异图，见 [`docs/design/evidence/my-content-2026-08-08.md`](docs/design/evidence/my-content-2026-08-08.md)。确认稿的页面内容区与实现截图按宽度和内容边界归一化；MiniPlayer / NavigationBar 继续由应用壳与公共组件独立验收。

## Focused-region comparison evidence

- Account card: 头像、设置承载、标题/副文案、右箭头、签到和 VIP 双按钮已按并排图重新校准尺寸与垂直锚点。
- Library cards: 四列栅格、分隔线、圆 / 圆角图标底、标题与登录态统计层级已按叠加图收紧。
- Playlist region: 匿名空态改用独立浅蓝插画；登录态保留标题、新建动作、三张独立封面、主副文案和更多操作锚点。
- Typography / large text: `1.5×` 时改为两列并允许账户卡与按钮自然增高，没有裁切或重叠。

## Findings

没有剩余 P0 / P1 / P2。

- Fonts and typography: 使用项目 Material 3 中文系统字体；层级、字重、统计副文案和换行行为与源图意图一致。字体家族存在平台渲染差异，归类为可接受差异。
- Spacing and layout rhythm: 页面边距保持 `14dp`，顶部起点、账户卡底部、四列卡片高度、Section 间距和紧凑歌单行已按确认稿证据收口。
- Colors and tokens: 主蓝、VIP 金、浅蓝账户容器、粉 / 紫 / 薄荷 / 红收藏图标底均映射现有主题 token；深色主题使用同一语义色系统。
- Image quality and asset fidelity: 匿名头像、登录态头像和三张歌单封面均为独立高分辨率位图，不再使用系统头像或空白封面替代；圆形 / 圆角裁切清晰，无透明边缘问题。
- Copy and content: 静态产品文案与确认稿一致；示例统计和歌单只存在于截图 fixture，运行时不写入业务状态。

## Comparison history

1. Initial comparison: P1 登录态缺少数量副文案和歌单列表；P2 设置齿轮无圆形承载且接线后撑高账户卡；P2 匿名 / 登录头像和歌单封面为占位。
2. First fixes: 增加可绑定的 `MyLibraryUi` / `MyPlaylistUi`，恢复确认稿完整结构；重做账户卡尾部布局和设置 Surface；加入头像与歌单视觉资产。
3. Evidence-backed correction: 保存实际并排/叠加/差异图后，继续修正过大的图标槽、Section 标题、胶囊按钮、账户卡密度和匿名空态占位。
4. Post-fix evidence: 更新后的匿名、登录、深色和 `1.5×` 截图重新生成；主态确认稿对照没有剩余可操作的 P0 / P1 / P2 差异。

## Follow-up polish

- P3: Material Icons 与源图中的定制线性图标存在轻微笔画差异；当前属于同一图标语义和一致的 Material 图标族，不阻塞本页面交付。

final result: passed

---

# 首页设计 QA

- Source visual truth: `docs/design/mockups/01-home-material3-v2.png`
- Layout contract: `docs/design/HOME_LAYOUT_SPEC.md`
- Implementation screenshot: `feature/home/src/screenshotTestDebug/reference/cn/james/music/feature/home/HomeScreenshotTestKt/HomeContentLightScreenshot_ContentLight_389295c6_0.png`
- Evidence: `docs/design/evidence/home-content-2026-08-08.md` 及同目录 side-by-side / overlay / diff。
- Viewport: Compose Preview `390 × 844dp`；截图 `1024 × 2216px`。确认稿内容区 `852 × 1595px` 按宽度归一化到 `1024 × 1917px` 后比较，应用壳 MiniPlayer / NavigationBar 独立验收。

## Findings

没有剩余 P0 / P1 / P2。

- 已恢复同一行品牌/搜索/头像、Radio Hero、三入口连续 Surface、每日推荐四行、品质/MV 视觉槽、更多操作锚点和四列歌单。
- 正常态区段边界和纵向密度已用同输入并排与叠加复核；不再用简化 Banner、时长尾部或空白封面替代确认稿结构。
- Light、Dark、AMOLED、Loading、Empty、Failure、RefreshProblem、`1.5×`、`2.0×` 共 9 个状态基线覆盖；大字体重排后关键文案和搜索入口可达且不裁切。
- ELE-AL00 / API 29 的 App instrumentation 回归 24/24 通过，覆盖首页搜索返回、一级导航、MiniPlayer 控件与切歌；未创建模拟器。
- 运行时只绑定真实首页数据；截图 fixture 的 HQ/MV、描述副标题和本地封面不进入协议或持久化模型。
- P3: Material 图标与稿内定制线性图标存在轻微笔画差异；原创 ImageGen 插画与稿内示例人物不同，但槽位、构图、裁切和色彩语义一致。

final result: passed

---

# 发现页设计 QA

- Source visual truth: `docs/design/mockups/02-discover-v2.png`
- Layout contract: `docs/design/DISCOVER_LAYOUT_SPEC.md`
- Implementation screenshot: `feature/discover/src/screenshotTestDebug/reference/cn/james/music/feature/discover/DiscoverScreenshotTestKt/DiscoverContentLightScreenshot_ContentLight_ce54aba0_0.png`
- Evidence: `docs/design/evidence/discover-content-2026-08-08.md` 及同目录 side-by-side / overlay / diff。
- Viewport: Compose Preview `390 × 843dp`；实现截图 `1024 × 2213px`。确认稿内容区 `853 × 1555px` 按宽度归一化到 `1024 × 1867px` 后比较，应用壳 MiniPlayer / NavigationBar 独立验收。

## Findings

没有剩余 P0 / P1 / P2。

- 已恢复五段顶部分段导航、本周新声 Hero、热门排行榜三列卡、分类胶囊和三列歌单封面；不再使用“仍在规划阶段”占位页。
- 正常态的 Hero、排行榜与分类区锚点和纵向密度已用同输入并排、叠加与差异图复核。
- Light、Dark、AMOLED、Loading、Empty、Error、`1.5×`、`2.0×` 共 8 个状态基线覆盖；大字体时排行榜重排为纵向列表，操作和文本不裁切。
- Hero、榜单与分类图片使用独立高分辨率位图；页面复用主题语义色和应用壳 MiniPlayer，不复制播放组件。
- ELE-AL00 / API 29 的 App instrumentation 回归 24/24 通过，覆盖发现页内容、纵向滚动、一级导航返回和既有 MiniPlayer 行为；未创建或启动模拟器。
- 当前预览内容只存在于发现 UI 层，不伪装协议、缓存、详情路由或播放业务已经完成。
- P3: 原创 ImageGen 人物与确认稿示例人物不同，Material 图标存在轻微笔画差异；槽位、构图、裁切、层级与色彩语义一致。

final result: passed

---

# 全屏播放器封面页设计 QA

- Source visual truth: `docs/design/mockups/06-player-cover.png`
- Layout contract: `docs/design/PLAYER_LAYOUT_SPEC.md`
- Implementation screenshot: `feature/player/src/screenshotTestDebug/reference/cn/james/music/feature/player/PlayerScreenshotTestKt/PlayerCoverScreenshot_Cover_fbd8d4e9_0.png`
- Evidence: `docs/design/evidence/player-cover-2026-08-08.md` 及同目录 side-by-side / overlay / diff。
- Viewport: Compose Preview `390 × 844dp`；实现截图 `1024 × 2216px`，确认稿 `853 × 1844px` 按完整页面归一化比较。

## Findings

没有剩余 P0 / P1 / P2。

- 补齐右上更多、分页点、品质徽标、收藏、下载、加入歌单、分享和独立队列动作；核心播放端口保持原接线。
- 方形封面、顶部三节点、信息区、进度、五项核心控制和四项次级动作的边界与纵向节奏已同输入复核。
- 播放、暂停、缓冲、连接中、未知时长、封面失败、空播放项、`1.5×`、`2.0×` 共 9 个状态基线覆盖；大字体允许滚动且不裁切核心控制。
- ELE-AL00 / API 29 的 App instrumentation 回归 24/24 通过；未创建或启动模拟器。
- 下载、加歌单、分享、收藏和更多只保留视觉语义，不以假反馈伪装功能完成。
- P3: 原创 ImageGen 封面与确认稿示例图不同；当前使用静态深色调色回退，后续真实封面动态色仍按既有审计接入；Material 图标存在轻微笔画差异。

final result: passed

---

# 通用 MiniPlayer 设计 QA

- Source visual truth: `docs/design/mockups/17-music-content-components.png`，并以 `01-home-material3-v2.png`、`04-my-v4.png` 的底部应用壳复核。
- Implementation screenshots: `MoeMiniPlayerLightScreenshot`、`MoeMiniPlayerDarkScreenshot`、`MoeMiniPlayerLargeTextScreenshot`。
- Viewport: 聚焦组件 Preview 使用 `390 × 104dp`，大字体使用 `390 × 120dp / 1.5×`；组件在同宽内容区按 Compose 逻辑尺寸比较，不直接把图板像素当作 dp。

## Comparison evidence

确认稿与三张聚焦截图已在同一比较输入中打开。实现保留封面、标题/歌手/质量徽标、上一首、主色圆形播放暂停、下一首、队列和带起止时间的独立进度区；浅色、深色与大字体结构一致，关键控制没有消失或重叠。ELE-AL00 / API 29 真机测试进一步断言四项操作的语义区域均不小于 `48dp`，并完成下一首切换。

## Findings

没有剩余 P0 / P1 / P2。

- P3: Material `QueueMusic`、`SkipPrevious`、`SkipNext` 与图板生成图标存在轻微笔画差异，但语义、视觉中心和方向一致。
- P3: `1.5×` 时长曲名按单行省略以固定四项播放控制；这是组件板“关键控制不消失”约束下的可访问性取舍。

final result: passed
