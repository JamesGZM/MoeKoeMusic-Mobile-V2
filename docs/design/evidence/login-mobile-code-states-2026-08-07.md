# 登录手机号验证码五态设计符合度证据

- 日期：2026-08-07
- 状态：当前实现有效
- 设计期望：[`13-login-phone-immersive.png`](../mockups/13-login-phone-immersive.png)
- Compose 状态：默认、发送中、倒计时、已输入、提交中
- 基准语境：简体中文、Light、`fontScale = 1.0`、无 IME
- Preview 视口：`390 × 845dp`，渲染图 `1024 × 2218px`
- Preview Insets：状态栏、导航栏、cutout 与 IME 均为 `0`
- 统一归一化：按宽度使用单一系数 `852 / 1024 = 0.83203125`，归一化到 `852 × 1846`

## 状态覆盖

| 状态 | Compose 输入 | 动态区域 | 固定区域结论 |
| --- | --- | --- | --- |
| 默认 | 空手机号、空验证码 | 禁用验证码与主按钮 | 通过 |
| 发送中 | 有效手机号、`sendingCode = true` | 发送文案与锁定状态 | 通过 |
| 倒计时 | 有效手机号、`countdownSeconds = 48` | 成功反馈与倒计时 | 通过 |
| 已输入 | 有效手机号与六位验证码 | 输入值与启用按钮 | 通过 |
| 提交中 | 已输入、`loggingIn = true` | 加载指示与提交文案 | 通过 |

五态只改变已登记的输入、反馈、倒计时和按钮状态。`mobileCodeDesignStatesKeepFixedAnchorsAndZeroScrollRange` 在真机可完整承载的同宽高比 `320 × 694dp` 容器内逐态切换，验证 Card、分段选中块和主按钮锚点不移动，且每一态都不暴露滚动动作；绝对设计坐标继续由 `390 × 845dp` Preview 归一化叠加证明。

## 固定锚点

下表单位均为归一化后的设计单位。实际值来自本轮 `validateDebugScreenshotTest` 渲染图；文字栅格和动态内容不用于固定边界判定。

| 锚点 | 期望 | 实际 | 最大偏差 |
| --- | --- | --- | --- |
| 登录 Surface 左侧 / 顶部 | `28 / 566` | `28.4 / 565.8` | `0.4` |
| 分段控件左侧 / 顶部 / 高度 | `61 / 604 / 90` | `61.2 / 603 / 89.6` | `1` |
| 手机号输入框顶部 | `849` | `848` | `1` |
| 验证码输入框顶部 / 底部 | `996 / 1099` | `997 / 1099` | `1` |
| 主按钮顶部 / 底部 | `1161 / 1274` | `1162 / 1274` | `1` |
| 安全页脚分隔线纵向位置 | `1435` | `1434` | `1` |

公共 Button/TextField 接入后的初始渲染曾使主按钮顶部和底部分别落在约 `1164 / 1277`，超出固定边界 `2` 个设计单位的限制。本轮把手机号反馈槽到主按钮的固定间距从 `11` 校准为 `8` 个设计单位；修正后固定边界最大偏差为 `1`，累计纵向漂移不超过 `2`。

## 遮罩与复核边界

- 设计稿中的时间、状态栏和导航栏图标属于系统示意；Preview 不绘制真实系统 Insets，因此不参与像素差异结论。
- 生产 Hero 使用从确认稿派生的无文字插画，叠加图只核对裁切框、主体构图、渐变与 Card 覆盖关系，不要求与原稿合成像素逐点相同。
- 字体栅格、示意文案字形、输入值、倒计时、加载指示和按钮启用色属于动态或平台区域；组件边界、锚点、相邻间距和固定层级不遮罩。
- 默认态按真实校验语义禁用主按钮；确认稿中的蓝色按钮只表达视觉组件，不覆盖 `canSubmitMobileCode` 的产品行为。

证据图片：

- [`login-mobile-code-states-overlay-2026-08-07.png`](login-mobile-code-states-overlay-2026-08-07.png)
- [`login-mobile-code-states-diff-2026-08-07.png`](login-mobile-code-states-diff-2026-08-07.png)

## 适用范围与剩余门禁

本报告证明当前公共组件接入后的手机号验证码五态基准几何、状态稳定性与零滚动语义。Dark、AMOLED、`1.5×` 和窄视口继续由截图回归覆盖，但不替代基准设计符合度。

本报告不证明密码、扫码、风险验证、多账号、IME、短高度、宽屏或真实系统 Insets 已完成设计符合度。真实验证码、密码、主动风控与跨设备扫码仍需用户主动验收。

## 本轮验证

- `./gradlew :feature:login:testDebugUnitTest`：通过。
- `./gradlew --no-configuration-cache :feature:login:validateDebugScreenshotTest`：31/31 通过。
- `./gradlew :feature:login:connectedDebugAndroidTest`：ELE-AL00 / API 29，14/14 通过。
- `./gradlew spotlessCheck :feature:login:lintDebug :app:assembleDebug`：通过。
