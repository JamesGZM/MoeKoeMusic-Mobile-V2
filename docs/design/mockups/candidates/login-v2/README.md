# 登录补充状态确认稿 v2

状态：已确认。确认日期：2026-08-06。这里的独立单状态图是登录补充状态的正式实现基线。

## 视觉权威

- 登录页面结构、Hero、密度与比例：[`13-login-phone-immersive.png`](../../13-login-phone-immersive.png)。它是必须直接复用的像素基座，不是仅供模型自由发挥的风格参考。
- Dialog：[`11-dialog-components.png`](../../11-dialog-components.png) 与 [`UI_COMPONENTS.md`](../../../../UI_COMPONENTS.md)。
- 操作、输入与按钮层级：[`16-actions-inputs.png`](../../16-actions-inputs.png)。
- 页面状态、Dialog 与 Bottom Sheet：[`18-mobile-states-overlays.png`](../../18-mobile-states-overlays.png)。
- “我的”切换账号背景：[`04-my-v4.png`](../../04-my-v4.png)。

所有确认稿均为独立的 `852 × 1846` 完整手机画布，不使用一张横向总览图承载多个状态。自动生成的示例账号、二维码和服务内容仅表达视觉，不是协议或正式数据。

Dialog 已于 2026-08-06 按 390dp 画布重新校准：普通确认型使用 `304dp` 宽度，六位验证码结构化输入型使用 `320dp` 宽度；两者均保留原排版密度、`48dp` 操作区、`28dp` 圆角与 32% 遮罩，不采用 `18-mobile-states-overlays.png` 约 `350dp` 的图板示例宽度。双操作区统一为 `12dp` 间距的等高按钮，左侧取消使用低强调 Tonal 背景，右侧主操作使用 Filled 背景，默认、提交和错误状态不移动槽位；禁用态保留清晰的中性灰容器，不与白色 Dialog 表面融合。`22d` Bottom Sheet 已确认符合规范，本轮不改动。

### 页面基座矩阵

| 状态 | 固定基座 | 允许变化区域 |
| --- | --- | --- |
| `19a–c`、`20a–e` | `13-login-phone-immersive.png` 完整画布 | 登录卡片中分段控件以下的标题、字段、状态反馈和操作内容 |
| `19d`、`21a–c`、`21e` | 对应 `19` 密码页原状态 | 统一遮罩与独立居中 Dialog；底层页面像素不得变化 |
| `21d` | 隔离安全验证 Activity | Activity 自身加载与失败恢复区域；这是明确例外，不复用登录卡片 |
| `22a–c` | `13-login-phone-immersive.png` 的 Hero 与页面容器 | 登录完成后的账号选择内容；容器边界和页面密度不得变化 |
| `22d` | `04-my-v4.png` 完整画布 | 统一遮罩与符合 `18-mobile-states-overlays.png` 的独立 Bottom Sheet |

重建或补图时必须先复制固定基座，再对允许变化区域做局部设计或确定性合成；禁止为每个状态重新生成整张页面。

## 19 · 密码登录

- [`19a-password-default.png`](19a-password-default.png)：默认态。
- [`19b-password-submitting.png`](19b-password-submitting.png)：提交中，字段与操作锁定。
- [`19c-password-rejected.png`](19c-password-rejected.png)：凭据错误内联显示。
- [`19d-password-risk-dialog.png`](19d-password-risk-dialog.png)：保留密码表单并叠加整屏居中的风险确认 Dialog。

## 20 · 扫码登录

- [`20a-qr-generating.png`](20a-qr-generating.png)：生成中。
- [`20b-qr-waiting.png`](20b-qr-waiting.png)：待扫码。
- [`20c-qr-scanned.png`](20c-qr-scanned.png)：已扫码，等待手机确认。
- [`20d-qr-expired.png`](20d-qr-expired.png)：已过期。
- [`20e-qr-failure.png`](20e-qr-failure.png)：获取失败与恢复操作。

## 21 · 风险验证

- [`21a-risk-sms-default.png`](21a-risk-sms-default.png)：密码页上的短信验证输入 Dialog。
- [`21b-risk-sms-submitting.png`](21b-risk-sms-submitting.png)：短信验证提交中。
- [`21c-risk-sms-rejected.png`](21c-risk-sms-rejected.png)：短信验证码错误，原输入上下文保留。
- [`21d-risk-tencent-loading.png`](21d-risk-tencent-loading.png)：非导出腾讯验证 Activity 的宿主加载态；不伪造供应商验证内容。
- [`21e-risk-tencent-failure-dialog.png`](21e-risk-tencent-failure-dialog.png)：隔离 Activity 取消或失败后返回原密码页的恢复 Dialog。

验证成功后最多重试一次原密码登录，不增加停留式“成功结果页”。

## 22 · 多账号

- [`22a-multi-account-unselected.png`](22a-multi-account-unselected.png)：默认不自动选择。
- [`22b-multi-account-selected.png`](22b-multi-account-selected.png)：用户显式选择。
- [`22c-multi-account-failure.png`](22c-multi-account-failure.png)：失败时保留选择上下文与恢复路径。
- [`22d-switch-account-sheet.png`](22d-switch-account-sheet.png)：未来“我的”切换账号 Bottom Sheet 确认稿。

`22d` 只完成视觉门禁；当前单会话架构尚未支持账号切换，编码前必须另做会话切换规格、技术审计和状态矩阵。

## 实现门禁

1. `19`、`20`、`21`、`22` 的结构、密度和覆盖层已经确认，无需再次申请设计确认。
2. 只有现有图片未覆盖的新状态才先补静态设计并等待确认。
3. 只有交互仍需验证时才更新原型；原型图标与近似布局不得进入 Compose。
4. Compose 按确认稿实现，并通过 Preview、截图测试和用户指定真机验收；不启动模拟器。
