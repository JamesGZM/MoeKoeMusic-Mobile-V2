# 登录密码三态设计符合度证据

- 日期：2026-08-07
- 状态：当前实现有效
- 设计期望：[`19a-password-default.png`](../mockups/candidates/login-v2/19a-password-default.png)、[`19b-password-submitting.png`](../mockups/candidates/login-v2/19b-password-submitting.png)、[`19c-password-rejected.png`](../mockups/candidates/login-v2/19c-password-rejected.png)
- Compose 状态：密码默认、提交中、凭据错误
- 基准语境：简体中文、Light、`fontScale = 1.0`、无 IME
- Preview 视口：`390 × 844dp`，渲染图 `1024 × 2216px`
- Preview Insets：状态栏、导航栏、cutout 与 IME 均为 `0`
- 统一归一化：按宽度使用单一系数 `852 / 1024 = 0.83203125`，归一化到 `852 × 1846`

## 人工确认与状态矩阵

2026-08-07 用户确认默认、提交和凭据错误三态统一采用 `y = 1192` 的主按钮顶部锚点，反馈槽始终保留，状态切换不得推动主操作；`19a`、`19b` 已将主操作、协议和安全页脚同步到与 `19c` 相同的纵向基座。用户同时确认 `19a` 的浅蓝 Filled 按钮禁用态可作为全局按钮参考。

| 状态 | 文案 | 按钮状态 | 可变区域 | 固定区域结论 |
| --- | --- | --- | --- | --- |
| `19a` 默认 | “登录并继续” | 浅蓝禁用 | 空账号、空密码 | 通过 |
| `19b` 提交 | “正在安全登录” | 主蓝加载 | 输入值、密码遮罩、进度 | 通过 |
| `19c` 错误 | “重新登录” | 主蓝启用 | 输入值、错误反馈 | 通过 |

`passwordDesignStatesKeepFixedAnchorsAndZeroScrollRange` 在真机可完整承载的同宽高比 `320 × 694dp` 容器内逐态切换，验证 Card、分段选中块和主按钮锚点不移动，且三态均不暴露滚动动作；绝对坐标由 Preview 归一化叠加证明。

## 固定锚点

下表单位均为归一化后的设计单位。文字、输入值、密码圆点和加载指示不用于固定边界判定。

| 锚点 | 期望 | 实际 | 最大偏差 |
| --- | --- | --- | --- |
| 登录 Surface 左侧 / 顶部 | `28 / 566` | `28.4 / 565.8` | `0.4` |
| 分段控件左侧 / 顶部 / 高度 | `61 / 604 / 90` | `61.2 / 603 / 89.6` | `1` |
| 账号输入框顶部 | `849` | `849..851` | `2` |
| 密码输入框顶部 / 底部 | `996 / 1099..1101` | `998 / 1098..1100` | `2` |
| 三态主按钮顶部 | `1192` | `1194` | `2` |
| 主按钮底部 | `1304..1305` | `1306` | `2` |

密码反馈槽到按钮的固定间距由 `32` 校准为 `31` 个设计单位，使整枚按钮的顶部和底部同时落入固定边界 `2` 个设计单位的限制。默认、提交和错误切换不改变按钮位置。

## 全局禁用按钮

Filled 主按钮非加载禁用态使用约 `35%` 主色容器与弱化 `onPrimary` 内容，对应 `19a` 的低饱和浅蓝层级；不再使用中性灰容器。加载态仍保持完整主色并显示进度。该规则落在 `MoeButton`，登录、Dialog 和后续消费者共享；Tonal、Outlined、Text 与 Destructive 按钮不受影响。

证据图片：

- [`password-states-overlay-2026-08-07.png`](password-states-overlay-2026-08-07.png)
- [`password-states-diff-2026-08-07.png`](password-states-diff-2026-08-07.png)

## 遮罩与剩余门禁

- 设计稿中的系统时间、状态栏和导航栏示意不参与 App 像素差异结论。
- Hero 派生插画只核对裁切、渐变和 Card 覆盖关系，不要求合成像素逐点相同。
- 字体栅格、动态输入值、密码遮罩和加载进度按契约遮罩；组件边界、锚点、层级和状态颜色不遮罩。
- 本报告不证明 `19d` 风险确认 Dialog、短信/腾讯验证、扫码、多账号、IME、宽屏或真实账号协议场景完成设计符合度。

## 本轮验证

- `./gradlew :feature:login:testDebugUnitTest`：通过。
- `./gradlew --no-configuration-cache :core:designsystem:validateDebugScreenshotTest :feature:login:validateDebugScreenshotTest`：Design System 12/12、登录 31/31 通过。
- `./gradlew :feature:login:connectedDebugAndroidTest`：ELE-AL00 / API 29，15/15 通过。
- `./gradlew :feature:login:testDebugUnitTest spotlessCheck :core:designsystem:lintDebug :feature:login:lintDebug :app:assembleDebug`：通过。
