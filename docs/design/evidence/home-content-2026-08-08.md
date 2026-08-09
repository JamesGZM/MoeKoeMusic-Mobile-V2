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
- 2026-08-09 的误通过复核纠正了旧证据反写的 `36dp` 封面：确认稿 `105` 设计单位应映射为约 `48dp`。歌曲行固定为 `48dp`、封面恢复 `48dp`，标题/艺人字级、徽标、更多图形和分隔线起点同步校准。
- Hero 文案与主按钮、快捷入口字级、四列歌单封面和两行文本已按同视口画布重新校准；页面仍只有一个纵向滚动所有者，MiniPlayer 与底栏不进入页面列表。
- 歌曲与歌单的示例徽标、副标题和本地封面只存在于截图 fixture；运行时继续消费真实标题、艺人、网络封面和播放事件，没有扩充协议或伪造服务能力。
- Dark、AMOLED、Loading、Empty、Failure、缓存刷新弱提示、`1.5×` 和 `2.0×` 已建立独立截图。`2.0×` 顶部重排、Hero/快捷入口增高并由外层滚动承载，关键文案与搜索入口不裁切。
- 本切片只修改 `HomeScreen` 的展示几何和截图基线，没有修改 ViewModel、Repository、Room、协议、导航或播放行为。

最新机器证据使用严格 `fixed=2`、`cumulativeY=3`：六个结构锚点最大误差 `1.08`，累计漂移 `1.61`；顶部、Hero、快捷入口、Section、歌曲文字和尾部操作共 10 个局部 region 全部通过。旧版 `8/24` 宽松阈值已删除。

## 验证

- `spotlessCheck`。
- `:feature:home:testDebugUnitTest`、`:feature:home:validateDebugScreenshotTest`、`:feature:home:lintDebug`。
- `:app:assembleDebug`、`:app:compileDebugAndroidTestKotlin`。
- Huawei ELE-AL00 / Android 10（API 29）运行 `:app:connectedDebugAndroidTest`，25/25 通过，覆盖首页搜索返回、三项一级导航、MiniPlayer 控制与应用级回归；没有创建或启动模拟器。

没有剩余 P0 / P1 / P2。P3 为 Material 图标与生成稿定制图标的轻微笔画差异，以及项目原创插画与稿内示例人物不同；语义、槽位比例、裁切和配色一致。

final result: passed
