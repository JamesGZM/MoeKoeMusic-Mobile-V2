# 歌词高亮方式与设置准入审计

状态：Accepted（产品与架构）；UI 实现状态：Blocked on user-confirmed visual states。审计日期：2026-08-10。

## 决策

设置页“歌词显示”的确认尾值“逐字歌词”表示歌词高亮方式，而不是歌词布局。Android 固定两档：`Character`（默认）和 `Line`。

- `Character`：使用已解析 KRC syllable timing，从当前原文行首至当前播放前缀使用共享 `PlayerPalette` accent。
- `Line`：当前原文整行使用同一 accent。
- 当前行没有可用 syllable 时，`Character` 安全回退为 `Line`；不得合成时间、重新请求、重新解析或重写缓存。
- 运行时切换只改变 app 注入的纯 Player UI 输入；保留当前 `PlayerLyricsUiState.Content`、播放位置、点击 seek、自动跟随、字号和翻译/音译可见性，不产生 Repository 调用。

`scroll/single` 单行/多行布局不是本设置，也不在本审计准入范围。Android 继续使用已确认的多行可滚动歌词视窗。

## 固定产品证据

| 来源 | 固定事实 | 本项目结论 |
| --- | --- | --- |
| PC `MoeKoeMusic@52c9833afe2e7fedcba8d5b23ff8d1f9731af73a`，GPL-2.0-only | `src/components/FullscreenLyricsSettings.vue:134-141` 分别声明 `highlightMode=char/line`（逐字/逐行）和 `displayMode=scroll/single`（滚动/单行）；默认值为 `char/scroll`（`97-103,112-118`），设置写入 `fullscreen-lyrics-settings` localStorage（`111,165-186`）。 | 迁移高亮语义及二者分离；拒绝 Vue/localStorage、侧滑面板和桌面单行布局。 |
| 同一 PC | `src/components/PlayerControl.vue:238-265,544-568,820-830` 将 single 与滚动列表作为独立布局分支；`src/assets/style/PlayerControl.scss:169-205` 中 char 使用进度色，line 对当前整行使用 primary。 | `Character/Line` 是有消费证据的迁移事实；多行布局保持 Android 确认稿。 |
| 旧 Mobile `MoeKoeMusic-Mobile@ab71195d4cf3297332490fd37704d1ae8973d4c5`，GPL-2.0-only | `src/components/ui/lyrics-view.tsx:30-54,58-153` 固定多行 ScrollView 和整行 active 样式；`src/features/player/types.ts:22-27` 只有 LRC 行；`src/features/settings/store.ts:7-94`、`storage.ts:3-29` 只持久主题/accent。 | 迁移移动端多行、当前行及滚动边界；Mobile 没有可迁移的高亮偏好。 |
| Android 确认稿与当前实现 | `docs/design/mockups/09-settings.png` 的尾值为“逐字歌词”；`07-player-lyrics.png` 与 `23a-23h` 均是多行歌词视窗；`docs/design/PLAYER_LYRICS_LAYOUT_SPEC.md:47-72` 固定歌词层级与滚动所有者。 | “逐字歌词”优先映射 PC `highlightMode`；`Line` 的可见状态和 Settings Dialog 是 Android 新设计状态。 |

## 模块、持久化与失败恢复

- `:core:model` 已于 040 新增纯 `LyricsHighlightModePreference.Character/Line`、`AppSettings` 字段和 Repository setter，默认 Character。
- `:data` 已于 040 使用私有稳定 key `lyrics_highlight_mode_v1` 与显式 storage value `character/line`；缺失、未知或读取失败回 Character，并沿用 `AppSettingsProblem.Read`；写失败保持最后持久值，`CancellationException` 原样传播。
- `:app` 显式映射领域 enum 到 `:feature:player` 的纯 `PlayerLyricsHighlightMode`，沿 `AppThemeViewModel → MainActivity → MoeKoeApp → playerDestination` 传递。`:feature:player` 不依赖 core model、data 或 Hilt。
- `:feature:settings` 以 Value 行和 chevron 打开“选择歌词显示”，选项为“逐字歌词”“逐行歌词”，复用已有 `MoeDialog`/Radio/取消组合。它有独立 job/generation/persisted value；保存中只禁用本行/Dialog；失败回滚且 typed Retry 精确重放最后失败的模式，不取消任何既有偏好写入。
- `:feature:player` 只根据模式渲染已在内存中的文档与 timing；不得把偏好写入歌词 document、Repository、播放 Service 或缓存。

## UI 准入阻塞

产品和架构没有待定项，但 UI 实现**必须等待用户确认**以下两个新增可见状态：

1. 基于 `07-player-lyrics.png` 的逐行高亮 Player 状态：保持多行滚动、字号和翻译/音译密度，只改变当前整行强调方式。
2. 基于现有 Settings `MoeDialog` / RadioButton / Cancel 的“选择歌词显示”Dialog，至少提供 `1.0×` 和 `2.0×`，后者须证明标题、两项和取消不会裁切。

在确认前，`LyricsDisplay` 必须继续保持当前明确 Unavailable，不能以开关、假值或 screenshot golden 代替设计决定。

## 后续原子切片与测试矩阵

1. 设计切片：补齐并确认上述 Player Line 与 Settings Dialog 图；登记正式 layout spec、contract 和 evidence。
2. domain/data：已于 040 完成 enum、DataStore、默认/roundtrip/unknown/read/write/cancel 及不覆盖既有偏好测试。
3. 真实纵向消费：Settings Dialog/retry、app 映射、Player 渲染与截图；默认 Character 既有 Player 基线应零变化，且仍受 UI 确认阻塞。

最低测试：Character 前缀、Line 整行、无 syllable 回退、切换不改变文档/请求/seek/auto-follow；Settings 快速选择代际、保存中禁用、失败回滚/Retry，以及与主题、自动跳过、动态色、附加文本、字号、音质、主题色和缓存清理并发时最后失败归属正确。确认后再新增 Dialog 1×/2×与 Line screenshot，运行 contract、fidelity、impact、golden change；不放宽阈值或扩大 mask。设备仅补验触控、大字体和手势竞争。
