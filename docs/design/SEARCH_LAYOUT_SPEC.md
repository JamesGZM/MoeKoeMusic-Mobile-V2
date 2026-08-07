# 搜索结果页设计复刻与适配契约

状态：Accepted。确认日期：2026-08-08。适用范围：搜索未提交、加载、综合结果、歌曲结果、空、错误、分页与大字体状态。

## 权威来源与画布

1. [`03-search-results-v2.png`](mockups/03-search-results-v2.png) 的 `852 × 1846px` 画布决定搜索型 Toolbar、分类、歌手 Hero、歌曲和横向卡片的结构、比例、间距与首屏密度。
2. [`../DESIGN_SYSTEM.md`](../DESIGN_SYSTEM.md)、`15-toolbar-navigation.png`、`16-actions-inputs.png` 与 `17-music-content-components.png` 决定触控区、输入图标槽、主题语义和音乐行行为。
3. Android 负责系统状态栏和导航栏；Feature 截图不绘制时间、信号、电池或手势条。App MiniPlayer 单独验收，不复制进页面。

## 搜索型 Toolbar 与分类

- Toolbar 消费状态栏 Insets；返回按钮使用共享 Chevron 和至少 `48dp` 触控区。
- 搜索输入占剩余主宽度，圆角约 `20dp`，低对比 Surface/Outline；搜索图标、文本、清除按钮都有独立槽位，图标背景不得填满输入框高度。
- 提交使用 IME Search；确认稿右侧语音图标作为独立 `48dp` 事件端口，不在首个 UI 切片申请录音权限。
- “综合、歌曲、歌单、专辑、歌手、MV”使用单行可横向滚动 Tab；选中项为 Primary 文字和短指示条，不使用六张独立胶囊卡片。

## 综合结果正文

- 页面正文使用一个 `LazyColumn`。歌手 Hero、歌曲段和歌单/专辑段是同一滚动所有者，不嵌套纵向列表。
- 歌手 Hero 水平边距约 `14dp`，圆角 `16dp`，头像约 `72dp` 圆形裁切；昵称、徽标、统计和代表作保持两级文本，关注按钮为 Primary Filled 操作。
- 歌曲区标题左对齐，右侧“查看全部 + Chevron”至少 `48dp`；歌曲行封面约 `52dp`，标题/副标题两行，尾部品质/MV 徽标和更多操作保持紧凑。
- 当前播放歌曲使用 Primary 指示，不改变整行高度。封面失败仍保留相同槽位。
- 歌单与专辑使用横向 `LazyRow`，卡片宽约 `104dp`，方形封面、圆角 `12dp`、两行文字和可选播放覆盖；不得把横向卡片改成纵向大卡列表。

## 状态、主题与适配

- Idle/Loading/Empty/Error 保留搜索 Toolbar 和分类，不用全屏替换应用壳。
- 首次错误居中展示重试；分页错误只出现在已有内容尾部。
- Light/Dark/AMOLED 通过语义色生成；位图不反转。图片使用真实本地 fixture 或 URL，并以 `ContentScale.Crop` 裁切。
- `fontScale >= 1.3` 时歌手 Hero 允许纵向重排，歌曲尾部徽标可下移或隐藏为语义动作；Tab 与横向卡片保持横向滚动。核心文字与操作必须可达，不横向裁切。
- Compact 单列；Medium/Expanded 限制正文最大宽度，不擅自改双栏。短高度和 MiniPlayer 存在时正文继续自然滚动。

## 设计符合度验证

1. 将确认稿去除系统栏和 App MiniPlayer 后，按宽度归一化到 Compose `390dp` 内容截图。
2. 同画布比较 Toolbar 搜索框、Tab 指示条、Hero、歌曲标题、第一/第四行、歌单段标题和首张横卡锚点。
3. 生成并排、50% 叠加和差异图；可见尺寸偏差必须修正后重新比较。
4. 仅平台字体栅格、Material 图标笔画和已登记的原创图片内容可列为 P3；结构缺失、错误滚动方向、输入图标槽、间距、圆角和行高不豁免。
