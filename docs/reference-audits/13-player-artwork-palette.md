# 播放器封面动态调色技术参考审计

状态：Accepted。审计日期：2026-08-06。

## 范围与产品语义

本审计允许 `:feature:player` 从当前播放歌曲已经加载成功的封面中提取颜色，并把同一组语义色同时提供给封面页和后续歌词页。它不改变 `06-player-cover.png`、`07-player-lyrics.png` 和已确认的 `23a` 至 `23h` 歌词状态稿的布局、层级、排版或控件尺寸。

设计稿确认的是播放器的结构和视觉关系，不是每首歌曲的固定背景色。生产实现中以下颜色由当前封面派生：

- 沉浸式背景渐变的起始色和中间色；
- 进度、高亮歌词和核心播放按钮的强调色；
- 强调容器上的前景色。

主要文字、次要文字和可访问性对比度仍由代码中的语义角色约束，不能直接照搬封面像素。封面缺失、加载失败、取色失败或候选色不合格时，统一使用 MoeKoe 深色静态渐变和深色主题语义色。

## Android 官方约束与依赖结论

- [Android Palette 指南](https://developer.android.com/develop/ui/views/graphics/palette-colors)明确把“根据专辑封面协调歌曲页面颜色”作为适用场景；`Palette` 提供 Vibrant、Muted、Dark 等色样及其像素占比。同步生成不得在主线程执行，连续处理图片时应缓存结果。
- [AndroidX Palette 发布页](https://developer.android.com/jetpack/androidx/releases/palette)当前生产稳定版为 `1.0.0`；`1.1.0-alpha01` 尚处 Alpha。本项目固定 `androidx.palette:palette:1.0.0`，不为尚未需要的新 API 采用 Alpha。
- [Coil 3 `Image.toBitmap`](https://coil-kt.github.io/coil/api/coil-core/coil3/to-bitmap.html)允许直接复用已解码的 Coil 图片。本功能只消费 `AsyncImage` 的成功结果，不另发封面请求，也不把图片或颜色写入数据库。

AndroidX Palette 为 Apache-2.0，维护主体为 AndroidX，职责仅是对小尺寸 Bitmap 做成熟的颜色量化。现有 Compose、Coil 或 Material 依赖不提供等价的封面主色提取能力；因此后续实现允许新增该单一稳定依赖，项目只保留亮度、对比度、语义角色和生命周期策略，不自行重写完整量化器，也不引入模糊、取色或动态主题大库。本审计只确定方案，当前切片不修改依赖或生产代码。

## 成熟开源项目固定参考

### Kreate

- 仓库：`knighthat/Kreate`；固定提交：`f02577e862318df26b13abb02d14d8d824a1b947`；许可证：GPL-3.0，仅学习状态和降级策略，不复制代码。
- 文件：`composeApp/src/androidMain/kotlin/it/fast4x/rimusic/ui/screens/player/Player.kt`、`composeApp/src/androidMain/kotlin/app/kreate/android/screens/player/background/BlurredCover.kt`。
- 采用：取色和背景跟随当前媒体标识更新；背景效果具有黑色遮罩及设备能力降级路径；高频播放进度不参与取色。
- 不采用：项目偏好矩阵、多个背景模式、实时模糊、设备型号分支、KMP 兼容层和 GPL 实现代码。

### Metrolist

- 仓库：`MetrolistGroup/Metrolist`；固定提交：`289ed45d0a429e6e4f19a2a07543b243a0ab2e8b`；许可证：GPL-3.0，仅学习状态和性能策略，不复制代码。
- 文件：`app/src/main/kotlin/com/metrolist/music/ui/player/Player.kt`、`app/src/main/kotlin/com/metrolist/music/ui/player/Thumbnail.kt`。
- 采用：封面加载与背景模式共享当前媒体身份；图片缓存和稳定 Compose 状态避免重复加载；Blur/Gradient 等沉浸背景始终保留可读前景与静态 Surface 回退。
- 不采用：YouTube 数据源、动态主题设置矩阵、复杂播放器样式、项目专属组件和 GPL 实现代码。

## 状态矩阵

| 状态 | 调色行为 | 恢复与隔离 |
|---|---|---|
| 当前封面加载中 | 立即使用 MoeKoe 静态回退，不沿用上一首颜色 | 同一媒体封面成功后原位切换 |
| 封面成功 | 将已解码图片缩小后在后台量化；应用亮度和对比度修正 | 结果仅在媒体标识仍一致时提交 |
| 封面缺失/失败 | 静态深色渐变、品牌强调色和既有占位封面 | 不做额外网络探针；下一首重新开始 |
| Palette 无候选/异常 | 与封面失败相同 | 不清空播放项，不影响封面展示 |
| 快速切歌 | 新媒体先回退；旧任务取消 | 旧媒体晚到结果必须丢弃，不能污染新页面 |
| 同一歌曲重组/进度更新 | 不重新取色 | 取色只由媒体标识和一次图片成功事件驱动 |
| 大字体/歌词滚动 | 颜色不变 | 不因字号、滚动位置或逐字进度重新量化 |
| 低性能设备 | 仍使用低色数、小 Bitmap、静态渐变 | 不启用实时模糊或逐帧采样 |

## 分层与数据流

```text
Coil AsyncImage（既有封面请求）
        ↓ 成功结果 + 当前媒体标识
PlayerArtworkPaletteExtractor
        ├─ 96 × 96 Bitmap
        ├─ Dispatchers.Default
        └─ AndroidX Palette（最多 12 色）
        ↓ 候选色
PlayerPaletteResolver（纯 Kotlin/Compose Color）
        ├─ 背景压暗并与固定深色末端混合
        ├─ 强调色保证相对背景可辨识
        └─ onAccent 在黑/白中选择足够对比的一方
        ↓
PlayerPalette（不可变语义 UI model）
        ├─ 封面页背景、进度与核心控制
        └─ 后续歌词页背景与逐字高亮
```

- `:feature:player` 只处理已经进入 UI 的图片和颜色，不访问网络、Repository、数据库或 Media3 Service。
- 取色结果属于可重建 UI 派生状态，不写 Room/DataStore。Coil 继续负责图片内存/磁盘缓存；单次播放器组合生命周期内，同一媒体成功图片只量化一次。
- 切歌以稳定媒体 `id` 作为代际边界。协程取消必须继续抛出，普通图片/取色异常才回退；回调和结果提交都再次核对媒体 `id`。
- 背景保持静态三段渐变，不引入实时模糊。取色失败不能阻断封面、歌词、播放控制或队列。

## 色彩门禁

- 大面积背景候选先与 MoeKoe 深色基底混合，并限制相对亮度，避免高亮封面造成白字失去对比。
- 强调色必须与最亮背景段达到至少 `3:1` 的非文本可辨识对比；强调容器文字/图标达到至少 `4.5:1`。
- 主要文本使用校验后的浅色语义色，次要文本保持不低于 70% 视觉强调；不能直接采用 Palette 返回的任意正文色。
- 封面为灰阶、近黑、近白或极端高饱和色时仍应得到稳定、可读的背景；无法修正时整体回退，不保留半套动态色。
- `07` 与 `23a` 至 `23h` 的蓝紫示例只表示动态色的一个可能实例，不得硬编码为所有歌曲的播放器色板。

## 验证矩阵

- JVM：明亮/极暗/灰阶/高饱和候选、无候选回退、背景与强调色对比度、强调容器前景选择。
- 模块构建：`:feature:player:testDebugUnitTest`、`:feature:player:compileDebugKotlin`，再运行 App Debug 组装验证依赖装配。
- 视觉：不在本切片使用 ADB 截图。由用户后续在指定真机验证至少一张明亮封面、一张暗色封面、一张灰阶封面、快速切歌和封面失败；需确认背景随当前封面变化且文字、进度与播放按钮始终清晰。

## 门禁结论

产品语义、依赖、线程、状态隔离、失败恢复、颜色约束和测试矩阵无关键待定项，允许实现播放器动态调色基础。该结论只开放封面派生 `PlayerPalette` 及现有封面页接入；歌词 Compose 仍需按已确认结构和歌词状态数据流单独实现，但必须复用同一 `PlayerPalette`，不得建立第二套取色逻辑。
