# 登录补充状态候选图 v2

状态：待用户逐组确认。这里的图片不是实现基线，不得在确认前用于制作新原型或修改 Compose 布局。

## 视觉权威

- 登录页面结构、Hero、密度与比例：[`13-login-phone-immersive.png`](../../13-login-phone-immersive.png)。
- Dialog：[`11-dialog-components.png`](../../11-dialog-components.png) 与 [`UI_COMPONENTS.md`](../../../../UI_COMPONENTS.md)。
- 操作、输入与按钮层级：[`16-actions-inputs.png`](../../16-actions-inputs.png)。
- 页面状态、Dialog 与 Bottom Sheet：[`18-mobile-states-overlays.png`](../../18-mobile-states-overlays.png)。
- “我的”切换账号背景：[`04-my-v4.png`](../../04-my-v4.png)。

所有候选均为独立的 `852 × 1846` 完整手机画布，不使用一张横向总览图承载多个状态。自动生成的示例账号、二维码和服务内容仅表达视觉，不是协议或正式数据。

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
- [`22d-switch-account-sheet.png`](22d-switch-account-sheet.png)：未来“我的”切换账号 Bottom Sheet 候选。

`22d` 只完成视觉门禁；当前单会话架构尚未支持账号切换，编码前必须另做会话切换规格、技术审计和状态矩阵。

## 确认门禁

1. 用户逐组确认 `19`、`20`、`21`、`22` 的结构、密度和覆盖层。
2. 确认后将候选提升为正式唯一映射，并同步阶段计划与 Design System。
3. 只有交互仍需验证时才更新原型；原型图标与近似布局不得进入 Compose。
4. 最后才按确认稿修改 Compose，并通过 Preview、截图测试和用户指定真机验收；不启动模拟器。
