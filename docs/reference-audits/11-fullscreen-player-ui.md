# 全屏播放器 UI 技术参考审计

状态：Accepted。审计日期：2026-08-05。

## 范围与视觉权威

本审计只允许开始全屏播放器的 Compose 外壳、封面页、播放进度、核心控制和现有队列覆盖层联动。视觉结构以已确认的 [`06-player-cover.png`](../design/mockups/06-player-cover.png)、[`07-player-lyrics.png`](../design/mockups/07-player-lyrics.png)、[`08-player-queue.png`](../design/mockups/08-player-queue.png) 为权威；[`player-flow`](../design/prototypes/player-flow/README.md) 只固定分页、退出、歌词定位、队列层级和返回优先级。

歌词搜索、下载、KRC/LRC 解析、翻译/音译和缓存不在首个切片中。没有固定协议审计前不得用虚构歌词接口或把参考项目实现移入生产代码。首个切片不展示伪歌词页；第二页只在歌词数据流完成独立协议与技术审计后接入。

## Android 官方约束

- [Pager in Compose](https://developer.android.com/develop/ui/compose/layouts/pager)：封面/歌词使用 `HorizontalPager` 和 `rememberPagerState`；页指示器读取 Pager 状态。页面按需组合，进入歌词页后才允许触发歌词加载，不能用两个常驻全屏页面加手写位移替代。
- [Predictive back in Compose](https://developer.android.com/develop/ui/compose/system/predictive-back)：播放器作为 Navigation Compose 子目的地，由导航栈负责退出和预测返回。队列打开时 Material Sheet 先消费返回；不注册永久吞掉系统返回的全局处理器。
- [Window insets in Compose](https://developer.android.com/develop/ui/compose/system/insets)：背景绘制到系统栏后方，顶栏和底部交互分别消费 `statusBars`、`navigationBars`/`safeGestures`，避免手势导航与队列拖动冲突。不得用固定状态栏高度或设备型号分支。
- [Accessibility in Compose](https://developer.android.com/develop/ui/compose/accessibility)：图标按钮提供动作语义，播放/暂停和收藏暴露状态；装饰背景、页点和模糊层不进入语义树。大字体下优先保留标题、进度和核心播放控制，次要操作可降级或滚动。

本切片继续使用仓库已有 AndroidX Compose、Navigation Compose、Material 3、Media3 和 Coil 3，均为 Apache-2.0；不新增依赖。封面取色不是首个切片的前置条件，先使用 Design System 深色渐变回退，避免为单页引入新的调色或模糊库。

## 成熟开源项目固定参考

### Kreate

- 仓库：`knighthat/Kreate`；固定提交：`f02577e862318df26b13abb02d14d8d824a1b947`；许可证：GPL-3.0，仅学习结构和状态处理，不复制代码。
- 文件：`composeApp/src/androidMain/kotlin/it/fast4x/rimusic/ui/screens/player/Player.kt`、`Lyrics.kt`、`Queue.kt`，以及 `app/kreate/android/screens/player/background/BlurredCover.kt`。
- 采用：Pager 状态与当前媒体索引分离；系统栏 inset 由布局消费；歌词、封面和队列是同一播放器层级中的状态；背景效果具有可关闭/降级路径。
- 不采用：RiMusic 历史兼容分支、KMP 偏好体系、多个播放器样式、封面切歌 Pager、复杂可视化、网络歌词提供方和自定义 Bottom Sheet。

### Metrolist

- 仓库：`MetrolistGroup/Metrolist`；固定提交：`289ed45d0a429e6e4f19a2a07543b243a0ab2e8b`；许可证：GPL-3.0，仅学习结构和性能策略，不复制代码。
- 文件：`app/src/main/kotlin/com/metrolist/music/ui/player/Player.kt`、`Queue.kt`、`Thumbnail.kt`、`PlayerSlider.kt`，以及 `ui/component/Lyrics.kt`。
- 采用：播放状态保持单一 Player 来源；进度拖动使用本地拖动值并在结束时提交 seek；队列使用稳定 key 和惰性列表；当前项具有明确状态；图片与背景失败时仍有可读回退。
- 不采用：单一大型 `app` 模块、YouTube/歌词提供方、播放器 Service 中的在线业务、复杂菜单、动态主题配置矩阵和项目专属图标资源。

### MoeKoe 固定产品参考

- PC `MoeKoeMusic@52c9833afe2e7fedcba8d5b23ff8d1f9731af73a`（GPL-2.0-only）：`src/components/QueueList.vue`、`src/views/Lyrics.vue`。采用打开队列后定位当前项和明确的播放/移除语义；拒绝桌面浮窗、右侧弹层、Electron IPC 和 Font Awesome。
- RN `MoeKoeMusic-Mobile@ab71195d4cf3297332490fd37704d1ae8973d4c5`（GPL-2.0-only）：`src/app/player.tsx`、`src/components/ui/lyrics-view.tsx`、`queue-sheet.tsx`。采用双页结构、进入歌词页后加载、点击歌词 seek、手动滚动后暂缓自动跟随和队列最高约 `68%` 视口；拒绝 Expo Audio、Tamagui、RN Modal 和其图标包。

## 产品状态矩阵

| 状态 | 页面行为 | 恢复/动作 |
|---|---|---|
| 无当前歌曲 | MiniPlayer 不提供入口；若因恢复竞争进入目的地，显示简短空态并允许返回 | 返回来源页面，不创建演示队列 |
| Controller 连接中 | 保留当前快照封面与元数据，核心按钮显示不可提交状态 | 连接完成后原位恢复 |
| Buffering | 保留进度和页面层级，播放按钮表达加载 | 成功继续；失败走现有类型化错误反馈 |
| Ready / 播放 | 标题、歌手、封面、位置、时长和暂停动作完整 | 暂停、seek、上一首、下一首、切换模式 |
| Ready / 暂停 | 页面保持，不自动退出 | 播放或其他队列操作 |
| 时长未知 | 进度条不可拖动，时间使用安全占位 | Media3 给出有效时长后启用 |
| 封面缺失/失败 | 使用 Design System 深色渐变与音乐占位语义 | 新歌曲或图片成功后替换，不额外探针 |
| 离线/VIP/无版权/验证/协议错误 | 页面不清空队列；复用现有 `PlaybackError` 与 Snackbar 映射 | 仅可恢复错误提供一次显式重试 |
| 队列打开 | 同页 Modal Bottom Sheet，最高约 `68%`，当前项高亮 | 返回先关队列；选歌后保持或关闭以最终实现规范为准 |
| 清空或删除最后一首 | 播放器失去当前项后退出到来源页面 | MiniPlayer 同步消失 |
| Activity 重建 | 当前目的地与 Pager 页可保存；真实媒体状态仍来自 Controller | 不从 UI 保存第二份播放状态 |

## 模块与数据流

```text
MediaLibraryService / Media3 Player
              ↓ StateFlow
       PlaybackController (:playback)
              ↓
      AppPlaybackViewModel (:app)
       ├─ PlaybackState（低频队列/状态）
       ├─ PlaybackProgress（高频位置）
       └─ 明确事件：play/pause/seek/skip/mode/queue
              ↓ values + lambdas
     PlayerRoute / PlayerScreen (:feature:player)
```

- `:playback` 继续是唯一持有 ExoPlayer 的模块；`:feature:player` 不依赖 Service、Repository、数据库或网络。
- `:feature:player` 提供无状态 `PlayerScreen`、目的地声明和纯 UI model。`:app` 作为组合根把现有 `AppPlaybackViewModel` 状态与事件注入，避免建立第二个 Controller 状态源。
- 全屏播放器是 Navigation Compose 子目的地，不加入底部一级导航。进入该目的地时隐藏 MiniPlayer 和底部导航；退出后原页面与 MiniPlayer 保持。
- `PlaybackProgress` 只下沉到进度/时间区域，封面和背景不能每秒重组。拖动中显示本地值，结束时调用一次 `seekTo`。
- 队列继续复用现有 `MoeKoeQueueSheet` 和 `MoeQueueRow`；先完成层级与返回，再单独实现拖拽排序，避免在首个切片同时改变队列状态机。

## 首个 Compose 切片

1. 新建 `:feature:player`，实现设计图 `06` 的全屏封面页、系统栏 inset、封面失败回退、标题/歌手、进度与播放/暂停、上一首、下一首、模式和队列入口。
2. App NavHost 注册播放器子目的地；MiniPlayer 主区域打开播放器，队列按钮仍直接打开队列。播放器目的地隐藏 MiniPlayer/一级导航。
3. `AppPlaybackViewModel` 仅补齐 `seekTo`、`skipPrevious`、`skipNext`、`setMode` 事件代理和失败报告；不复制媒体状态。
4. 返回由导航栈处理；队列打开时由 `ModalBottomSheet` 优先消费返回。

收藏、下载、分享、歌词设置和动态封面取色在各自数据能力完成前不做无效按钮。设计中的位置可以保留布局余量，但不得用 Toast 冒充功能完成。

## 验证矩阵

- JVM：`AppPlaybackViewModel` 的 seek/skip/mode 接线、拒绝结果反馈；播放器纯映射/格式化单测。
- Compose UI：有歌曲、无歌曲、未知时长、播放、暂停、Buffering、无封面；点击 MiniPlayer 打开、收起返回、队列返回优先级。
- Screenshot/Preview：`390 × 844dp` 深色基线，至少 `1.0×`、`1.5×`、`2.0×` 字体；封面成功与回退各一张。
- 真机：ELE-AL00 / API 29 验证三键导航栏 inset、播放不中断、seek、上下首、队列、Activity 重建和返回；预测返回动画另在 API 35+ 设备可用时补验，不用模拟器替代用户指定真机门禁。
- 性能：播放进度更新不触发封面图片重载；Pager/队列滚动不创建网络请求；低性能设备始终可使用静态渐变回退。

## 门禁结论

首个切片的产品语义、技术栈、数据流、失败恢复和测试矩阵无关键待定项，允许开始 `:feature:player` 封面页与 App 导航联动。歌词协议、动态取色、收藏/下载/分享和队列拖拽仍是后续独立门禁，不得借本结论提前实现。
