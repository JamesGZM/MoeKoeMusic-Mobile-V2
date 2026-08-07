# “我的首页”设计 QA

- Source visual truth: `docs/design/mockups/04-my-anonymous.png`、`docs/design/mockups/04-my-v4.png`
- Implementation screenshots: `feature/my/src/screenshotTestDebug/reference/cn/james/music/feature/my/MyScreenshotTestKt/MyAnonymousScreenshot_Anonymous_aaadb213_0.png`、`MyAuthenticatedScreenshot_Authenticated_0f7821ba_0.png`
- State: 简体中文、浅色、匿名 / 已登录；另检查匿名深色与匿名 / 已登录 `1.5×` 字体。
- Viewport: Compose Preview `390 × 844 dp`；实现截图 `1024 × 2216 px`，约 `2.626×` density。源图均为 `853 × 1844 px`，比较时按内容宽度归一化，不把像素密度差异作为问题。
- Scope normalization: 源图底部 MiniPlayer 与三项导航属于 app shell，不在 `:feature:my` 截图内；本轮只比较“我的”可滚动内容区域。app shell 将由下一原子切片“通用播放组件”单独对照。

## Full-view comparison evidence

源图和最新实现已在同一比较输入中逐对打开。匿名态的信息顺序、账户卡、四项快捷入口、收藏与关注、创建歌单空态一致；登录态的信息顺序、统计副文案、“新建”入口和三行歌单结构一致。实现保持单一纵向滚动，不把长设计稿压缩进单屏。

## Focused-region comparison evidence

- Account card: 品牌头像 / 用户头像、圆形设置承载、标题与副文案、右箭头、签到和 VIP 双按钮均已逐项核对。
- Library cards: 四列栅格、分隔线、圆 / 圆角图标底、标题与登录态统计层级已逐项核对。
- Playlist region: 匿名空态与登录态标题、新建动作、三张独立封面、主副文案、更多操作锚点已逐项核对。
- Typography / large text: `1.5×` 时改为两列并允许账户卡与按钮自然增高，没有裁切或重叠。

## Findings

没有剩余 P0 / P1 / P2。

- Fonts and typography: 使用项目 Material 3 中文系统字体；层级、字重、统计副文案和换行行为与源图意图一致。字体家族存在平台渲染差异，归类为可接受差异。
- Spacing and layout rhythm: 页面边距已收敛为 14dp，账户卡不再被齿轮 / 箭头撑高；卡片、分组与列表的顺序和纵向节奏一致。
- Colors and tokens: 主蓝、VIP 金、浅蓝账户容器、粉 / 紫 / 薄荷 / 红收藏图标底均映射现有主题 token；深色主题使用同一语义色系统。
- Image quality and asset fidelity: 匿名头像、登录态头像和三张歌单封面均为独立高分辨率位图，不再使用系统头像或空白封面替代；圆形 / 圆角裁切清晰，无透明边缘问题。
- Copy and content: 静态产品文案与确认稿一致；示例统计和歌单只存在于截图 fixture，运行时不写入业务状态。

## Comparison history

1. Initial comparison: P1 登录态缺少数量副文案和歌单列表；P2 设置齿轮无圆形承载且接线后撑高账户卡；P2 匿名 / 登录头像和歌单封面为占位。
2. Fixes: 增加可绑定的 `MyLibraryUi` / `MyPlaylistUi`，恢复确认稿完整结构；重做账户卡尾部布局和设置 Surface；加入五张独立视觉资产；恢复签到 / VIP 的确认稿视觉状态。
3. Post-fix evidence: 更新后的匿名、登录、深色和 `1.5×` 截图重新生成并与两张源图同输入复核，未发现可操作的 P0 / P1 / P2 差异。

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
