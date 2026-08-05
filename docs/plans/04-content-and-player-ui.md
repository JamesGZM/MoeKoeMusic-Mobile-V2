# 阶段 5A：内容与播放器 UI

状态：进行中。音乐内容组件、搜索/本地列表、MiniPlayer 与基础队列已经完成；全屏播放器和其余内容详情仍待实现。

## 目标

按照已批准视觉稿实现主要内容浏览和播放体验，不重新解释信息架构。

## 实现范围

- 底部一级导航只有首页、发现、我的；搜索是从首页进入的独立子页面。
- 实现首页、发现、搜索、歌单/专辑/歌手/排行榜详情。
- 实现 MiniPlayer、沉浸式封面页、歌词页和队列 Bottom Sheet。
- 播放页参考 Kreate 的大封面、封面驱动背景和封面/歌词切换，不复制其 Logo、双层底栏和过密操作。
- 接入 `MoeSnackbar`、`MoeToast` 和 Dialog；持续状态使用页面内组件。
- 页面覆盖加载、内容、空数据、错误、离线和无版权状态。

## 视觉验收

页面与设计稿的唯一映射见 [`../DEVELOPMENT_PLAN.md`](../DEVELOPMENT_PLAN.md)。颜色、排版、间距、圆角和组件尺寸以 [`../DESIGN_SYSTEM.md`](../DESIGN_SYSTEM.md) 为准确实现依据，视觉形态参考 `14` 至 `18` 号通用组件图板。文字必须支持 `1.0×`、`1.3×`、`1.5×`、`2.0×` 字体，交互目标不小于 `48dp`。

`:core:designsystem` 的 Primary、Typography、Spacing、Shapes、Toolbar 和音乐内容组件已经完成第一批校准；后续页面必须继续消费这些 Token 和组件，不得在业务页面用局部常量绕过校准。

## 当前结果（2026-08-05）

- `:core:designsystem` 已实现 Section Header、封面容器、媒体徽标、歌曲行、MiniPlayer 和队列行，并建立浅色、深色与 `1.5×` 字体截图基准。
- Search 与 LocalMusic 已移除各自的重复歌曲行布局，统一使用 `MoeSongRow`；在线封面和 App 专属目录本地封面继续由 Coil 3 加载。
- `PlaybackItem` 使用 `Remote(HTTPS)` 与 `AppFile(相对路径)` 两类稳定封面引用；Media3 metadata、MiniPlayer、队列和 Room v3 快照共享该字段，不持久化短期音频地址。
- Room `2→3` 与完整 `1→2→3` 迁移已在 ELE-AL00 / API 29 的真实 SQLite 上 2/2 通过；既有队列迁移后封面字段保持可空。
- 应用壳 MiniPlayer 已展示真实播放进度、封面及明确播放/暂停和队列语义；子页面隐藏一级导航时会单独处理系统导航栏安全区。
- Search/LocalMusic 截图验证、相关 JVM 单测、Debug 构建与 `MainActivityTest` 真机回归 6/6 通过；真机手动搜索并播放在线歌曲后，MiniPlayer、恢复为暂停状态和队列 Bottom Sheet 均已检查。
- 已在 `06` 至 `08` 号确认设计图之后建立 [`player-flow`](../design/prototypes/player-flow/README.md) 本地交互原型，并验证封面/歌词双页、点击歌词定位、暂无歌词、纵向退出、队列覆盖层和“先关队列、再退出播放器”的返回优先级；原型不定义 Compose 视觉或协议行为。

## 当前剩余

- 沉浸式封面页、歌词页、歌单/专辑/歌手/排行榜详情和对应完整状态矩阵。
- 队列拖拽、全屏播放器 Compose 联动、深色/AMOLED 内容页面与 `2.0×` 字体关键控制验收。
- 真实在线封面缺失/失败占位视觉、TalkBack 顺序、预测返回和复杂队列设备测试。

## 测试

- Route/ViewModel 状态与旧请求取消单测。
- 导航、MiniPlayer 与全屏播放器联动测试。
- 核心页面浅色、深色、纯黑和大字体截图。
- TalkBack 语义、滚动、返回和预测返回设备测试。

## 完成标准

- 主要页面在标准手机上与批准稿的结构、层级和视觉语言一致。
- 搜索不出现在底部导航，播放器不占一级 Tab。
- 大字体不裁切关键控制，错误与加载不只依赖 Toast。
