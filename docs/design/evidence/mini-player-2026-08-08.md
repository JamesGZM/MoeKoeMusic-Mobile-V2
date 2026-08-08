# 通用 MiniPlayer 确认稿设计符合度

- 权威设计：[`17-music-content-components.png`](../mockups/17-music-content-components.png) 的 MiniPlayer 组件区；[`01-home-material3-v2.png`](../mockups/01-home-material3-v2.png) 用于校验它与页面壳层的实际密度。
- 实现截图：`MoeMiniPlayerLightScreenshot`；Dark 与 `1.5×` 字体继续作为回归基线。
- 状态：简体中文、Light、`390 × 72dp`、播放中、存在封面/质量徽标/进度。

## 同画布方法

- 页面壳层稿截取 `x = 20..832, y = 1594..1736` 的 `812 × 142px` MiniPlayer Surface，归一化为 `972 × 170px`。
- Compose 截图截取 `x = 26..998, y = 10..180` 的同一 Surface，保持 `972 × 170px`。
- 组件板另截取 `x = 362..774, y = 666..738` 的 `412 × 72px` 样例并归一化到同一画布，用于复核组件结构而非页面像素密度。
- 并排图左侧为确认稿、右侧为实现；叠加与差异图只使用页面壳层同视口样本。

## 证据文件

- 页面壳层：[`side-by-side`](mini-player-shell-2026-08-08-side-by-side.png)、[`overlay`](mini-player-shell-2026-08-08-overlay.png)、[`diff`](mini-player-shell-2026-08-08-diff.png)。
- 组件板：[`side-by-side`](mini-player-component-2026-08-08-side-by-side.png)。

## 复核结论

- 连续底部 Surface、`12dp` 圆角、紧凑封面、单行标题/歌手、质量徽标、上一首/播放暂停/下一首/队列、独立时间与进度条均与确认稿同序。
- 标题不再因过大的字级和控制槽位提前省略；四个控制节点的可见图标保持紧凑，但语义触控区域仍为 `48dp`。
- 截图 fixture 使用仓库已有的真实方形插画资源，不再用渐变或圆形头像假装歌曲封面；运行时封面仍由既有 `PlaybackItem` 与 Coil 链路提供。
- 确认稿封面内容、Android 字体栅格化与 Material Icons 笔画存在 P3 像素差异；结构、锚点、密度、色阶和触控约束一致。
- 本切片没有修改播放状态、Media3、队列命令、导航或协议行为。

## 验证

- `spotlessCheck`。
- `:core:designsystem:testDebugUnitTest`、`:core:designsystem:validateDebugScreenshotTest`、`:core:designsystem:lintDebug`。
- `:app:assembleDebug`、`:app:compileDebugAndroidTestKotlin`。
- Huawei ELE-AL00 / Android 10（API 29）运行 `:app:connectedDebugAndroidTest`，25/25 通过；其中 MiniPlayer 用例继续逐项断言上一首、播放/暂停、下一首与队列的语义触控区域不小于 `48dp`。

final result: passed
