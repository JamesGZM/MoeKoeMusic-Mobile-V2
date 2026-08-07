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
- P3: MiniPlayer 与底部导航的源图符合度由下一“通用播放组件”切片处理。

final result: passed
