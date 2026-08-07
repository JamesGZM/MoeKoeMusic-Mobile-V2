# 首页正常态设计符合度证据

- 权威设计：`docs/design/mockups/01-home-material3-v2.png`
- 实现截图：`feature/home/src/screenshotTestDebug/reference/cn/james/music/feature/home/HomeScreenshotTestKt/HomeContentLightScreenshot_ContentLight_389295c6_0.png`
- 状态：简体中文、Light、`1.0×` 字体、正常内容 fixture。
- 画布：设计 `852 × 1846px`；Compose Preview `390 × 844dp`，截图 `1024 × 2216px`。比较时将设计图 `y = 0..1595` 的首页内容区按宽度归一化到 `1024 × 1917px`，与实现同范围对照；MiniPlayer 和 NavigationBar 由应用壳独立验证。

## 证据文件

- `home-light-content-2026-08-08-side-by-side.png`：左侧确认稿，右侧 Compose 实现。
- `home-light-content-2026-08-08-overlay.png`：相同内容范围的 50% 叠加。
- `home-light-content-2026-08-08-diff.png`：像素差异图；原创图片内容和字体栅格不作为结构偏差。

## 复核结论

- 顶部品牌、搜索框和圆形头像保持同一行；搜索仍接入既有独立目的地。
- Radio Hero、三入口连续 Surface、每日推荐四行和推荐歌单四列均已恢复，不再使用空白占位或简化 Banner 代替。
- Hero、快捷入口、两个分组标题、歌曲行和歌单区的首屏顺序及纵向密度与确认稿一致；页面只有一个纵向滚动所有者，MiniPlayer 与底栏不进入页面列表。
- 歌曲与歌单的示例徽标、副标题和本地封面只存在于截图 fixture；运行时继续消费真实标题、艺人、网络封面和播放事件，没有扩充协议或伪造服务能力。
- Dark、AMOLED、Loading、Empty、Failure、缓存刷新弱提示、`1.5×` 和 `2.0×` 已建立独立截图。`2.0×` 顶部重排、Hero/快捷入口增高并由外层滚动承载，关键文案与搜索入口不裁切。
- 指定 ELE-AL00 / API 29 真机的 `:app:connectedDebugAndroidTest` 24/24 通过，覆盖首页搜索返回、三项一级导航、MiniPlayer 控件与切歌以及全量应用回归；没有创建或启动模拟器。

没有剩余 P0 / P1 / P2。P3 为 Material 图标与生成稿定制图标的轻微笔画差异，以及项目原创插画与稿内示例人物不同；语义、槽位比例、裁切和配色一致。

final result: passed
