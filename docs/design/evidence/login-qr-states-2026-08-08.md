# 扫码登录五态设计符合度

状态：通过。复验日期：2026-08-08。

## 对照范围

- 确认稿：[`20a`](../mockups/candidates/login-v2/20a-qr-generating.png)、[`20b`](../mockups/candidates/login-v2/20b-qr-waiting.png)、[`20c`](../mockups/candidates/login-v2/20c-qr-scanned.png)、[`20d`](../mockups/candidates/login-v2/20d-qr-expired.png)、[`20e`](../mockups/candidates/login-v2/20e-qr-failure.png)，均为 `852 × 1846`。
- Compose 基准：`LoginQrGeneratingScreenshot`、`LoginQrWaitingScreenshot`、`LoginQrScannedScreenshot`、`LoginQrExpiredScreenshot`、`LoginQrFailureScreenshot`，`390 × 844dp`，输出 `1024 × 2216` 后统一归一化为 `852 × 1846`。
- 五态叠加图：[`login-qr-states-overlay-2026-08-08.png`](login-qr-states-overlay-2026-08-08.png)。
- 五态差异图：[`login-qr-states-diff-2026-08-08.png`](login-qr-states-diff-2026-08-08.png)。
- 待扫码并排图：[`login-qr-waiting-side-by-side-2026-08-08.png`](login-qr-waiting-side-by-side-2026-08-08.png)，左侧确认稿、右侧 Compose。

## 状态结论

| 状态 | 固定结构 | 状态区域 |
| --- | --- | --- |
| 生成中 | Hero、Card、模式选择、标题、说明、三步引导、安全页脚 | 环形进度与生成文案 |
| 待扫码 | 同上 | 真实 ZXing 二维码、中心音乐徽标和倒计时 |
| 已扫码 | 同上 | 蓝色确认容器、完成标识和等待手机确认文案 |
| 已过期 | 同上 | 淡化二维码不保留中心品牌徽标，叠加过期标识、说明和刷新操作 |
| 获取失败 | 同上 | Wi-Fi 与错误徽标、失败说明、重新获取和改用验证码操作 |

五态共用同一 `LoginLayoutSpec` 设计坐标映射。失败态单独增加终止状态高度，使错误组、三步引导和安全页脚落在确认稿纵向锚点；其余状态不借此改变页面基座。

## 可变与遮罩项

- 系统状态栏图标不属于 Compose Preview，对照时遮罩。
- Hero 使用仓库已确认的正式登录素材；不同生成批次的角色像素内容不作为布局差异。
- 二维码矩阵由测试 URL 确定，允许与设计示例的矩阵不同，但边框、尺寸、淡化和中心徽标规则必须一致。
- 倒计时数值和加载环旋转帧允许变化。
- 终止态按钮继续使用用户已确认的全局 `MoeButton` 圆角与颜色，不回退到状态图中的旧按钮圆角示意。
- 图标使用仓库资源或 Material Icons；允许库级笔画差异，不允许改变图标语义。

## 回归门禁

- 标准字体覆盖全部五态；生成、过期和失败状态另覆盖 `1.5×` 大字体。
- 截图基准只能在与 `20a–20e` 的归一化并排复核后更新。
- 本证据只关闭视觉五态，不替代二维码真实状态 `2→4`、扫码可用性和轮询生命周期的真机/服务验收。
