# 登录风险确认 Dialog 设计符合度（2026-08-07）

## 范围与决策

- 目标状态：[`19d-password-risk-dialog.png`](../mockups/candidates/login-v2/19d-password-risk-dialog.png)。
- 实现状态：`LoginPasswordRiskRequiredScreenshot`。
- Dialog 继续使用 Compose 系统 `Dialog` 承担窗口居中、返回键与模态语义；业务层只组合公共 `MoeDialog` Surface 和内容，不增加顶部、底部或系统栏偏移。
- 用户于 2026-08-07 明确确认：修改 Dialog 内容宽高不应改变系统居中，也不需要业务代码处理顶部、底部间距。

## 固定视觉核对

| 项目 | 确认稿 | Compose 截图 | 结论 |
| --- | ---: | ---: | --- |
| 普通确认 Dialog 宽度 | `664 / 852` | `798 / 1024`，归一化为 `663.96 / 852` | 通过 |
| 操作区 | 低强调取消 + Filled 主操作 | 公共 `MoeDialogActions` | 通过 |
| 底层主操作 | `正在安全登录` + 进度 | 风控存在期间保持加载态 | 通过 |
| 正文 | `为保障账号安全，请先完成额外验证后再登录。` | 字符串资源一致 | 通过 |

宽度修复仅调整 `MoeDialogSurface` 的 Modifier 约束顺序，使 `304dp` 最大宽度真正生效；没有新增定位、Insets 或设备分支。

## 自动验证

- `:core:designsystem:validateDebugScreenshotTest`：通过。
- `login.password.risk-confirm-dialog` 独立探针测得 Surface 顶边 `722.24`，确认稿为 `722`，误差 `0.42` 设计单位；正文单行排布与系统居中高度债务已关闭。
- `:feature:login:validateDebugScreenshotTest`：回归基准通过。
- 其余单元、真机、lint 与 assemble 结果在本切片提交前统一记录。

## 未覆盖

- 本报告不证明短信风险验证 `21a–c`、腾讯失败恢复 `21e` 或真实账号风控协议完成验收；它们继续作为独立切片。
