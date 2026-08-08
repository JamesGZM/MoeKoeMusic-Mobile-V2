# 登录短信风险 Dialog 三态设计符合度（2026-08-07）

## 范围

- 确认稿：`21a-risk-sms-default.png`、`21b-risk-sms-submitting.png`、`21c-risk-sms-rejected.png`。
- Compose：`LoginRiskSmsScreenshot`、`LoginRiskSmsSubmittingScreenshot`、`LoginRiskSmsRejectedScreenshot`。
- 条件：简体中文、Light、`fontScale = 1.0`；运行截图统一缩放到 `852 × 1846`。

## 人工确认与数据边界

用户确认确认按钮保留当前全局 Filled 风格；取消按钮按设计图使用浅蓝 Tonal 背景和蓝色文字。Dialog 使用白色 Surface、Bold 标题与紧凑输入型间距，仍由系统 `Dialog` 负责居中，不增加 Insets 补偿。

`AuthRiskChallenge` 不提供手机号，正式 UI 使用“验证码已发送至账号绑定手机号，10 分钟内有效”，不把设计稿示例 `138 **** 8000` 硬编码进产品或领域模型。

## 状态矩阵

| 状态 | 验证码 | 输入边框 | 操作 |
| --- | --- | --- | --- |
| `21a` 默认 | 空，首格聚焦 | 首格 Primary | 取消可用；确认使用已确认的全局禁用态 |
| `21b` 提交 | `246810` | 全部中性禁用 | 两个操作锁定；确认显示进度 |
| `21c` 错误 | 保留 `246810` | 六格 Error | 显示内联错误；取消与确认可恢复 |

默认态 Surface 通过紧凑操作区间距恢复确认稿高度与系统居中位置；独立探针测得顶边 `689.75`，确认稿为 `690`，误差 `0.42` 设计单位。宽度保持约 `699 / 852`。

## 视觉证据

- [`risk-sms-states-overlay-2026-08-07.png`](risk-sms-states-overlay-2026-08-07.png)：三态 50% 叠加。
- [`risk-sms-states-diff-2026-08-07.png`](risk-sms-states-diff-2026-08-07.png)：三态放大差异。

差异图中的 Hero、系统状态栏、字体栅格和正式泛化手机号文案不作为固定几何失败；Dialog Surface、标题层级、验证码格、操作区和底层页面状态参与复核。

## 自动验证

- `:core:designsystem:validateDebugScreenshotTest`：12/12 通过。
- `login.risk-sms.default` 结构契约通过固定 `2` 设计单位阈值，原上下边界最大偏差约 `6` 的视觉债务已关闭。
- `:feature:login:validateDebugScreenshotTest`：回归基准通过。
- 新增真机行为测试验证错误态保留验证码与恢复操作；完整结果在提交前记录。

## 未覆盖

- 腾讯隔离验证和失败恢复 `21d–e`、真实账号短信风控协议仍独立验收。
