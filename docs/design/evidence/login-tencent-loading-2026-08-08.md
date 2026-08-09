# 腾讯安全验证加载页设计符合度

状态：通过。复验日期：2026-08-09。

## 对照范围

- 确认稿：[`21d-risk-tencent-loading.png`](../mockups/candidates/login-v2/21d-risk-tencent-loading.png)，`852 × 1846`。
- Compose 基准：`TencentCaptchaLoadingScreenshot`，`390 × 845dp`，输出 `1024 × 2218` 后归一化为 `852 × 1846`。
- 叠加图：[`login-tencent-loading-overlay-2026-08-08.png`](login-tencent-loading-overlay-2026-08-08.png)。
- 差异图：[`login-tencent-loading-diff-2026-08-08.png`](login-tencent-loading-diff-2026-08-08.png)。

## 结论

- 页面使用公共 `MoeStandardTopBar`，左上角使用共享 `MoeNavigateBackIcon` 的标准返回入口；本次确认稿仅确定性替换旧关闭组合，标题和内容区保持原布局。
- 去除 WebView 默认页面边距并使用透明宿主背景，供应商内容加载后覆盖加载占位，不额外制造顶部或底部补偿。
- 以 Toolbar 内容区底边为原点，进度环顶部、加载文案、底部安全图标和两行说明的纵向锚点与确认稿一致。
- Preview 不绘制系统状态栏，因此对照时将顶部系统栏高度列为遮罩项；真机由系统状态栏和公共 Toolbar 共同组成确认稿顶部高度。
- 加载动画允许处于不同旋转帧；严格局部像素区域仅覆盖静态加载文案，进度环仍由固定锚点验证位置；Material 图标的内部笔画与确认稿示意图允许存在库级差异。
- `login.tencent-captcha.loading` 已建立独立结构契约；加载环顶边探针测得 `704.94`，确认稿为 `705`，误差 `0.42` 设计单位。

## 回归门禁

- 截图测试覆盖标准字体和 `1.5×` 大字体。
- `RiskCaptchaActivityTest` 验证固定腾讯脚本源、透明页面背景、禁用 JavaScript Interface 和 AppId 注入防护。
- 该页面只负责视觉宿主与安全 WebView 容器，不把腾讯验证码内容伪造成原生页面。
