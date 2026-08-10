# 播放淡入淡出准入审计

状态：Deferred。审计日期：2026-08-10。

## 结论

Settings 的“淡入淡出”不能进入领域偏好、DataStore、UI 或播放器实现。固定 PC / 旧 Mobile 均没有可迁移的音频淡化消费者；当前 Android 也只有明确不可提交的占位行。更关键的是“淡入淡出”可能指三种行为，不能以同一个未定义 Toggle 代替产品决策。

| 类型 | 精确定义 | 当前准入 |
| --- | --- | --- |
| A：曲目开始/结束淡化 | 单曲开始时淡入，或在结束前淡出；不与下一曲重叠。 | Deferred：没有来源、时机或异常边界。 |
| B：暂停/恢复音量 ramp | 显式暂停前将当前音量降到零，恢复时从零升回用户音量。 | Deferred：Kreate 可作技术参考，但 PC/Mobile 未定义为 MoeKoe 产品行为。 |
| C：相邻歌曲 crossfade | 当前曲淡出和下一曲淡入重叠播放。 | Deferred：既无产品定义，也超出当前单 Player 架构。 |

这三类不共享“启用后如何工作”的答案。禁止先把行改成开关、持久化一个猜测的默认值，或仅实现页面动画/歌词边缘 fade 冒充音频能力。

## 固定产品与当前实现事实

| 来源 | 固定证据 | 采用 / 拒绝 |
| --- | --- | --- |
| PC `MoeKoeMusic@52c9833afe2e7fedcba8d5b23ff8d1f9731af73a`，GPL-2.0-only | `src/assets/style/PlayerControl.scss:15-40,73-99` 是歌词视窗 mask 与单行歌词 CSS transition；`src/components/PlayerControl.vue:157,238-247` 是封面/歌词视觉 transition。对 `src` 的 audio fade / crossfade / gapless 搜索没有播放偏好或消费者。 | 拒绝把这些视觉效果迁移成音频语义。 |
| 旧 Mobile `MoeKoeMusic-Mobile@ab71195d4cf3297332490fd37704d1ae8973d4c5`，GPL-2.0-only | `src/components/ui/mini-player.tsx:5-14,89` 使用 `FadeInDown` / `FadeOutDown` 作为 MiniPlayer 进出场动画；`src/app/settings.tsx:296-301` 是 About Modal 的 `animationType="fade"`。对 `src` 的 audio fade / crossfade / gapless 搜索没有播放偏好或消费者。 | 拒绝把页面动画作为迁移事实。 |
| Android V2 | `feature/settings/src/main/kotlin/cn/james/music/feature/settings/SettingsUiModels.kt:72-85,264-275` 将 `Fade` 固定为 `SettingsRowUi.Unavailable`；`docs/design/contracts/settings.content.light.properties:63` 明示“尚无消费者”。 | 保持不可提交状态，直到真实行为 Accepted。 |
| Android V2 | `playback/src/main/kotlin/cn/james/music/playback/MoeKoePlaybackService.kt:40-84` 只创建一个 `ExoPlayer` 与一个 `MediaLibrarySession`；`playback/src/main/kotlin/cn/james/music/playback/Media3PlaybackController.kt:118-159,208-239` 直接下发 play/pause/seek/skip 命令；工程内无 volume/crossfade/gapless 运行时策略。 | 不能把 Feature 或 Controller 变成第二个 Player 所有者。 |

## Android 与成熟项目约束

- [Media3 ExoPlayer API](https://developer.android.com/reference/androidx/media3/exoplayer/ExoPlayer) 要求对同一 Player 的访问串行于同一个 application thread；[player events](https://developer.android.com/media/media3/exoplayer/listening-to-player-events) 区分自动切换、seek、重复和播放列表变化。因此淡化计时器、取消、最终音量和切歌必须归 Service 的同一串行边界，不能由 Compose 或 ViewModel 驱动。
- Kreate `f02577e862318df26b13abb02d14d8d824a1b947`，GPL-3.0：`composeApp/src/androidMain/kotlin/app/kreate/android/themed/common/screens/settings/general/PlayerSettings.kt:104-110` 提供 `AUDIO_FADE_DURATION`；`composeApp/src/androidMain/kotlin/app/kreate/android/service/player/StatefulPlayerImpl.kt:132-201,404-447` 使用单 Player `ValueAnimator` 在 play/pause 时取消旧动画并做音量 ramp。只借鉴 B 的“单一协调器、取消旧动画、恢复用户音量”约束；不复制 GPL 代码，也不采用其 KMP / SharedPreferences 架构。
- Metrolist `289ed45d0a429e6e4f19a2a07543b243a0ab2e8b`，GPL-3.0：`app/src/main/kotlin/com/metrolist/music/ui/screens/settings/PlayerSettings.kt:102-113,306-360` 将 crossfade、1–15 秒时长和 gapless 例外分为独立偏好；`app/src/main/kotlin/com/metrolist/music/playback/MusicService.kt:305-397,928-936,4592-4792` 维护 secondary/fading ExoPlayer、在 crossfade 时关闭 offload、安排下一项、交换 Player/MediaSession 并清理旧 Player。只借鉴 C 必须显式处理双 Player、offload、Session 交接和失败清理的风险；不复制 GPL 实现，且不接受其大型 Service 结构。

## 缺失的五项产品选择

在以下五项由用户确认前，不得实施：

1. **语义**：第一版只选择 A、B、C 中的一项，还是明确规划多个独立能力；不得将它们合并为“淡入淡出”。
2. **时长与默认**：默认关闭还是默认多少毫秒/秒、允许哪些档位、是否需要单独的设置 Dialog 而非 Toggle。
3. **曲目转换范围**：A/C 是否只在自然结束生效；手动上一首/下一首、`playNow`、队列删除/重排、repeat-one、shuffle、seek 和未知时长各如何处理。
4. **暂停与失败边界**：B 是否仅限用户显式播放/暂停；音频焦点、耳机拔出、系统媒体按钮、错误、地址刷新、自动跳过、后台/Service 销毁时取消、静音或恢复的精确规则。
5. **连续输出兼容性**：C 的 gapless 例外定义（同专辑或其他产品规则）、音频焦点与双输出切换、offload 是禁用、降级还是不支持，以及设备不支持/次级来源解析失败时的确定性退化。

## 已冻结的未来架构边界

- 若选择 A 或 B：先以纯策略确定事件资格，再由 `:core:model` / `:data` 持久化**已确认**的语义与时长；`:feature:settings` 仅写 Repository 并显示状态；`:playback` 的 `MoeKoePlaybackService` 在 Player application thread 拥有单一 `FadeCoordinator` 和代际取消。当前 `Media3PlaybackController` 的直接 play/pause 路径必须先改成 Service 可一致拦截的命令边界；UI、ViewModel、Repository 不得写 Player volume，也不得持久化瞬态动画/音量。
- 若选择 C：须先完成单独的架构准入，批准 Service 内双 Player、唯一权威 current item、MediaSession 交换、快照、音频焦点、地址刷新与 secondary resolve 失败的协调方案。它不是 Settings-first 切片，也不得让 `:feature:player`、`:app` 或 `:data` 创建 ExoPlayer。
- 所有方案均保留现有“地址最多刷新一次”、自动跳过、队列和恢复快照的责任边界；fade 失败不能吞掉原始播放错误或把运行时 volume 写入 Room。

## 后续原子顺序与验证矩阵

1. 用户确认上述五项并补齐 A/B/C 对应的 Settings 与 Player 视觉状态；未确认前保持 `Fade` Unavailable。
2. 只为所选语义完成 Accepted 架构/失败恢复审计，再实现纯策略和 `:playback` 可测边界；C 必须先批准双 Player 迁移。
3. 之后才依次接入领域/DataStore、Service 消费、Settings UI 与截图契约，禁止反向以 UI 倒逼产品语义。

最低测试：纯策略覆盖自然/手动转换、seek、repeat/shuffle、未知时长、快速相反动作和代际取消；Service fake 覆盖最终音量、暂停/播放、地址刷新、错误/自动跳过、焦点/noisy 及无陈旧回调。C 另覆盖 primary/secondary 交接、MediaSession/快照唯一性、secondary resolve/error 和 offload 降级。指定真机还必须覆盖 API 26/32/33/36、耳机/Bluetooth、锁屏/通知/媒体按钮、音频焦点、noisy、后台、不同输出和 offload 能力；本审计未运行设备、模拟器或真实服务。

## 非目标与未验证项

- 本切片不新增依赖、权限、网络、后台任务、DataStore key、公共接口或音频实现。
- 不复制 PC/Mobile、Kreate 或 Metrolist 的 GPL 代码；固定源码只作为产品和风险对照。
- Media3 的实际设备输出、offload、焦点时序、蓝牙和通知控制只能在用户指定真机上验收，不能由 JVM、截图或本审计替代。
