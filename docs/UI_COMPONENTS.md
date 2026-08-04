# UI 反馈与 Dialog 组件规范

状态：第一版视觉与交互基准。本文约束 Compose 中的 Dialog、`MoeSnackbar`、`MoeToast` 和系统 Toast；具体颜色值在建立 `:core:designsystem` 时落入语义化 token，并通过截图测试和真机继续校准。

视觉参考：

- [`11-dialog-components.png`](design/mockups/11-dialog-components.png)
- [`12-feedback-components-v2.png`](design/mockups/12-feedback-components-v2.png)

## 反馈方式选择

| 场景 | 组件 | 原因 |
| --- | --- | --- |
| 用户必须确认后才能继续 | Dialog | 阻断当前流程并呈现明确选择 |
| 危险或不可撤销操作 | Dialog | 防止误触，说明影响范围 |
| 可恢复错误、成功结果、撤销操作 | `MoeSnackbar` | 不阻断页面，可附带一个操作 |
| 复制完成等低优先级、无操作反馈 | `MoeToast` | 应用前台内保持品牌一致性 |
| Compose Host 不可用且无需统一外观 | 系统 Toast | 接受 Android 系统样式 |
| 加载、空数据、持续错误 | 页面内状态 | 状态持续存在，不应依赖瞬时提示 |

禁止用 `MoeToast` 或系统 Toast 呈现登录失效、领取 VIP 失败、播放失败、网络重试或任何需要用户操作的信息。

## Dialog

### 通用尺寸与外观

- 手机端宽度为 `min(320.dp, windowWidth - 48.dp)`；大字体下允许内容增高，不固定高度。
- 容器使用 `surfaceContainerHigh`，圆角 `28.dp`，遮罩使用约 32% 黑色；不使用玻璃模糊和厚重阴影。
- 外边距 `24.dp`，内容内边距 `24.dp`，标题与正文间距 `16.dp`，正文与操作区间距 `24.dp`。
- 标题采用约 `24sp / 32sp`，正文采用 `14sp / 20sp`，按钮标签不小于 `14sp`。
- 所有操作区域至少 `48.dp` 高。取消操作在左，主要操作在右；危险操作只将最终确认按钮设为 `error` 语义色。
- 图标只辅助表达语义，不替代标题和正文。颜色不能成为区分成功、警告和错误的唯一方式。

### 组件类型

#### 确认操作

用于领取 VIP、切换会影响播放的设置等需要二次确认但可安全取消的操作。包含标题、简短影响说明、“取消”和一个主要操作。

#### 单按钮提示

用于用户必须阅读后关闭的结果，例如兼容接口已失效。只有“知道了”；普通成功结果优先使用 Snackbar，不弹 Dialog。

#### 危险操作

用于删除歌单、清空队列或清除缓存。标题必须明确动作，正文说明对象与后果，不使用模糊的“确定吗”。进行中的请求禁用重复提交；服务端成功前不提前关闭。

#### 文本输入

用于新建歌单和重命名。使用单个 Material 3 文本框，提供可见 Label、字符限制和内联错误；空值或非法名称时禁用主要按钮。键盘弹出后 Dialog 必须跟随 IME 调整，不能遮住操作区。

### 关闭与状态

- 普通确认和输入 Dialog 支持系统返回；点击遮罩是否关闭由业务风险决定。
- 危险操作和提交中的 Dialog 不因遮罩点击关闭，但仍需处理系统返回和进程重建。
- Dialog 是否显示属于页面状态，不在 Composable 内保存业务结果。
- ViewModel 暴露稳定的 `DialogState?`，UI 通过明确事件处理确认、取消和输入变化；网络层错误先映射为领域错误，再映射成本地化文案。

## MoeSnackbar

### 通用尺寸与位置

- 页面只设置一个 `MoeSnackbarHost`，位于 MiniPlayer、底部导航和 IME 上方，不覆盖播放控制。
- 左右边距 `16.dp`，最小高度 `48.dp`；有两行内容或操作时允许增高，不固定宽高。
- 正文最多两行，只允许一个尾部操作。更复杂的选择使用 Dialog 或独立页面。
- 组件使用 `16.dp` 圆角、约 `1.dp` 语义描边和轻量阴影；图标放入柔和的圆形色调容器，不使用默认黑色长条。
- 普通与可撤销反馈使用淡蓝容器；成功使用 `successContainer`，警告使用 `warningContainer`，错误使用 Material 3 `errorContainer`。`success*` 与 `warning*` 是 MoeKoe 设计系统扩展 token，必须同时定义对应的 `on*` 颜色。
- 深色和纯黑主题使用相同语义映射切换到深色容器，不简单反转浅色值。所有正文、图标和操作色满足对比度要求。
- 成功和普通状态使用短时展示；带“撤销”或“重试”的反馈使用较长时长。TalkBack 开启时尊重系统建议的延长时间。

### 语义类型

| 类型 | 示例 | 操作 |
| --- | --- | --- |
| 成功 | 签到成功，已获得 3 小时 VIP | 通常无 |
| 可撤销 | 已从歌单移除 | 撤销 |
| 错误 | 网络异常，请稍后重试 | 重试 |
| 状态提醒 | 今天已经领取过了 | 通常无 |

连续相同消息需要去重；不同消息按顺序展示。导航后仍与当前任务相关的消息可以继续展示，已经失去上下文的消息应丢弃。

### Compose 状态约定

- 瞬时反馈仍通过不可变 `StateFlow` 暴露，例如带唯一 `id` 的 `pendingMessage`，避免额外暴露无生命周期约束的事件流。
- Route 收集状态并调用 `SnackbarHostState.showSnackbar`；展示完成后通过 `onMessageConsumed(id)` 明确消费。
- Repository 和 `:kugou-api` 不生成 UI 字符串，只返回类型化结果或错误。

## MoeToast 与系统 Toast

### MoeToast

- `MoeToast` 是 Compose 应用内轻提示，不是 Android `Toast`，用于“已复制歌曲链接”等短暂、无操作、即使错过也不影响任务完成的反馈。
- 宽度按内容自适应，最大 `320.dp`；最小高度 `48.dp`，水平内边距 `16.dp`，圆角 `18.dp`，使用 `surfaceContainerHighest`、`outlineVariant` 描边和轻量阴影。
- 可带一个 `20.dp` 语义图标，正文采用 `14sp / 20sp`，最多两行；不放按钮、不承载错误详情。
- 由根布局中的 `MoeToastHost` 统一排队、去重和播放进入/退出动画，位置在 MiniPlayer、底部导航和 IME 上方，默认短时展示。

### 系统 Toast

- 仅在 Compose Host 不可用且确实无需统一视觉时使用，例如应用切到后台后的低优先级结果。
- Android 系统 Toast 的外观由系统控制，接受系统颜色、圆角、图标和位置，不做像素级复刻。
- 禁止自定义 Window Toast。如果反馈需要品牌外观，使用 `MoeToast`；如果需要操作按钮，使用 `MoeSnackbar`。

## 无障碍与验证

- Dialog 打开后焦点进入标题或首个输入控件，关闭后返回触发它的控件。
- `MoeSnackbar` 和 `MoeToast` 使用适当的 live region 语义；Snackbar 操作按钮必须有可读文本，不能只有图标。
- 在 `1.0×`、`1.3×`、`1.5×` 字体倍率下验证不裁切，并测试 TalkBack、横屏、深色和纯黑主题。
- `MoeAlertDialog`、`MoeInputDialog`、`MoeSnackbarHost` 和 `MoeToastHost` 建立后，为四种 Dialog、四种 Snackbar 语义和两种 Toast 长度分别提供 Preview 或截图测试。
