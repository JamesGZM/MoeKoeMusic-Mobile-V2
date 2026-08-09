# 腾讯安全验证加载页设计符合度

状态：通过。宿主校准复验日期：2026-08-09。

## 对照范围

- 确认稿：[`21d-risk-tencent-loading.png`](../mockups/candidates/login-v2/21d-risk-tencent-loading.png)，`852 × 1846`。
- Compose 基准：`TencentCaptchaLoadingScreenshot`，`390 × 845dp`，输出 `1024 × 2218` 后归一化为 `852 × 1846`。
- 叠加图：[`login-tencent-loading-overlay-2026-08-08.png`](login-tencent-loading-overlay-2026-08-08.png)。
- 差异图：[`login-tencent-loading-diff-2026-08-08.png`](login-tencent-loading-diff-2026-08-08.png)。
- 并排图：[`login-tencent-loading-side-by-side-2026-08-09.png`](login-tencent-loading-side-by-side-2026-08-09.png)。

## 结论

- 页面使用公共 `MoeStandardTopBar`，左上角使用共享 `MoeNavigateBackIcon` 的标准返回入口；标题下方是纯白、填满剩余视口的 WebView 宿主。
- Preview/loading fallback 只绘制居中的 `24dp` 最小不定进度环和“正在打开滑块安全验证…”。远端 `TCaptcha.js` 创建的 H5/iframe 在真实 Activity 中满铺接管该内容区；供应商滑块不是静态设计真值，也不被 Compose 截图伪造。
- 不再绘制渐变、大环、盾牌或两行原生页脚。HTML 的 `html/body`、`#status` 与腾讯容器/iframe 都使用白色、`100%` 宽高和 `overflow:hidden`，不造成宿主外的空白或滚动。
- `login.tencent-captcha.loading` 保持 strict core-page contract：导航、Toolbar 标题、白色 host 与加载文案为局部像素 region；进度环顶部探针实测 `932.16`、登记 `932`，误差 `0.16` 个设计单位。加载环的旋转帧不作为供应商内容或布局豁免。

## 回归门禁

- 截图测试覆盖标准字体和 `1.5×` 大字体。
- `RiskCaptchaActivityTest` 验证固定腾讯脚本源、全尺寸白色宿主、禁用 JavaScript Interface、精确 CSP/origin 边界和 AppId 注入防护。
- 该页面只负责可控宿主与安全 WebView 容器；真实服务/真机兼容时由腾讯供应商内容接管，仍需用户主动验收。
