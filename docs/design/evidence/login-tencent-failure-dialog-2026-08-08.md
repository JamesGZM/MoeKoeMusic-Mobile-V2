# 腾讯安全验证失败 Dialog 设计符合度

状态：通过。复验日期：2026-08-08。

## 对照范围

- 确认稿：[`21e-risk-tencent-failure-dialog.png`](../mockups/candidates/login-v2/21e-risk-tencent-failure-dialog.png)，`852 × 1846`。
- Compose 基准：`LoginRiskTencentScreenshot`，`390 × 844dp`，输出 `1024 × 2216` 后归一化为 `852 × 1846`。
- 并排图：[`login-tencent-failure-dialog-side-by-side-2026-08-08.png`](login-tencent-failure-dialog-side-by-side-2026-08-08.png)，左侧确认稿、右侧 Compose。
- Dialog Surface 叠加图：[`login-tencent-failure-dialog-surface-overlay-2026-08-08.png`](login-tencent-failure-dialog-surface-overlay-2026-08-08.png)。
- Dialog Surface 差异图：[`login-tencent-failure-dialog-surface-diff-2026-08-08.png`](login-tencent-failure-dialog-surface-diff-2026-08-08.png)。

## 结论

- 失败返回后保留密码页、已输入凭据和安全登录加载上下文，Dialog 不替换底层页面。
- 使用公共 `MoeDialog` 与系统 `Dialog` 窗口居中，不增加顶部、底部、状态栏或导航栏偏移。
- `304dp` Confirm Surface、白色背景、Bold 标题、正文、`12dp` 操作间距、浅蓝 Tonal 取消和 Filled 重新打开与确认稿一致。
- 公共 Dialog 在 Android 运行窗口显式设置 `0.32f` `dimAmount`，不依赖设备主题默认值。

## 截图遮罩说明

Android Gradle Screenshot Test 的离线 Dialog 渲染器固定应用约 65% 平台遮罩，且不消费运行窗口的 `Window.setDimAmount()`；因此全屏并排图只用于检查底层页面保持、Dialog 位置和尺寸，遮罩明度不作为该离线 PNG 的像素门禁。Surface 局部叠加与差异图用于验证 Dialog 本体。运行时遮罩由公共组件显式固定为 32%，指定真机构建与测试覆盖同一实现。

## 回归门禁

- 公共 Dialog 截图与登录失败 Dialog 截图均参与回归。
- 关闭返回密码页；重新打开只重新启动隔离验证，不自动无限重放原密码请求。
- 后续 Dialog 不得覆盖公共 Surface、标题字重、操作顺序、系统居中或 `0.32f` 遮罩。
