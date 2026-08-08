# 发现页内容区设计符合度证据

- 确认稿：`docs/design/mockups/02-discover-v2.png`
- 实现截图：`feature/discover/src/screenshotTestDebug/reference/cn/james/music/feature/discover/DiscoverScreenshotTestKt/DiscoverContentLightScreenshot_ContentLight_ce54aba0_0.png`
- 比较范围：确认稿顶部至 MiniPlayer 上边界 `853 × 1555px`，按宽度归一化为 `1024 × 1867px`；实现截图取同尺寸顶部内容区。MiniPlayer 与 NavigationBar 继续由应用壳独立验收。

## 证据文件

- `discover-light-content-2026-08-08-side-by-side.png`
- `discover-light-content-2026-08-08-overlay.png`
- `discover-light-content-2026-08-08-diff.png`

## 结论

- 本轮重新复核纠正了旧证据未拦住的局部漂移：五段 Tab 的文字基线、Hero 标题/副标题/按钮、排行榜封面起点、分类胶囊和三列分类封面均重新与确认稿同画布校准。
- 五段 Tab、选中指示条、Hero 边界与按钮、排行榜标题与三列封面、榜单两行信息与播放入口、分类标题、横向胶囊和三列封面的锚点及纵向节奏现在保持一致；原创图片内容不参与结构坐标判断。
- Light、Dark、AMOLED、Loading、Empty、Error、`1.5×`、`2.0×` 共 8 组截图基线已建立；大字体下排行榜改为纵向行布局，避免三列文本和操作裁切，Tab 与分类胶囊允许水平滚动。
- 保留的 P3 差异为原创插画内容与 Material 图标笔画。图片槽位比例、构图留白、裁切和蓝色语义符合确认稿，不阻塞交付。
- 当前榜单、分类与 Hero 内容是 UI 层设计预览，不声明远端接口已接入，不写入协议、数据库或播放状态。
- 本切片没有修改导航事件、榜单数据、播放接口或页面状态所有权。

## 验证

- `spotlessCheck`。
- `:feature:discover:testDebugUnitTest`（当前无测试源）、`:feature:discover:validateDebugScreenshotTest`、`:feature:discover:lintDebug`。
- `:app:assembleDebug`、`:app:compileDebugAndroidTestKotlin`。
- Huawei ELE-AL00 / Android 10（API 29）运行 `:app:connectedDebugAndroidTest`，25/25 通过；发现页纵向定位继续使用页面专属语义，没有创建或启动模拟器。
