# 首页、账号与“我的”UI 参考审计

状态：Accepted。审计日期：2026-08-05。

## 产品边界

本里程碑优先交付首页、登录、“我的”和用户主页。阶段 4 中无版权、VIP、网络、会话与协议错误统一接入 `MoeSnackbar`，以及播放地址失效后刷新一次，继续暂停。

首版只发布简体中文，但所有可见文案从 Android 字符串资源读取；布局同时接受伪本地化、RTL 和 2.0 倍字体检查。

## MoeKoeMusic PC

- 固定提交：`52c9833afe2e7fedcba8d5b23ff8d1f9731af73a`；GPL-2.0-only。
- 本地路径：`../MoeKoeMusic`。
- 首页：`src/components/home/HomeRecommendations.vue`。
- 登录：`src/views/Login.vue`。
- 用户音乐库：`src/views/Library.vue`。
- 风险验证：`src/components/RiskVerifyModal.vue`、`src/utils/riskVerify.js`。
- 协议：`api/module/get_verify_info.js`、`api/module/verify_user_info.js`。

采用：`/top/card` 的五种 Radio 模式、`20028`/`ssaCode` 风险识别、验证请求串行化、`KGCodeTX` 载荷格式和验证后最多重试一次。

拒绝：Vue 全局单例、DOM Teleport、动态注入任意脚本、全局请求拦截器直接进入首批登录实现、依赖本地 Node 服务和把服务端字符串直接作为领域错误。

## MoeKoeMusic Mobile

- 固定提交：`ab71195d4cf3297332490fd37704d1ae8973d4c5`；GPL-2.0-only。
- API submodule：`283f1e97b110726b208a64b486a657c0fc0a6126`。
- 本地路径：`../MoeKoeMusic-Mobile`。
- 登录页面：`src/app/login.tsx`。
- 登录解析：`src/features/account/auth.ts`。
- 首页：`src/app/(tabs)/index.tsx`、`src/features/home/load-home-data.ts`。
- 用户：`src/app/(tabs)/me.tsx`、`src/features/account/user-api.ts`、`vip-api.ts`。
- 用户资产：`src/features/library/library-api.ts`、`src/features/cloud/cloud-api.ts`。

采用：验证码、密码、扫码三种登录状态；手机号多账号选择；`sid`、`edt` 与 `eventId` 完整传递；`v_type=32` 短信和 `v_type=23` 腾讯验证分支；二维码轮询的等待、已扫码、过期与失败状态。

拒绝：Expo/Tamagui UI、SecureStore、开发环境响应正文日志、全局 cleartext、通用 WebView 桥和 JavaScript API Runtime。

## 图形验证码结论

Android 实现固定流程：

1. 密码登录返回 `20028` 或非空 `ssaCode` 时构造 `RiskChallenge(eventId, sid, edt)`。
2. 调用 `get_verify_info`；`v_type=32` 展示原生短信输入，`v_type=23` 启动隔离验证码 Activity。
3. 腾讯回调必须同时包含 `ticket` 与 `randstr`。
4. Kotlin 构造 `KGCodeTX|{...}`，而不是信任网页构造酷狗协议载荷。
5. 调用 `verify_user_info` 时继续携带 `eventId/sid/edt`。
6. 验证成功只重试一次原密码登录；再次触发风险即返回类型化失败。

腾讯官方 Android 接入仍要求 WebView/H5，因此本项目只为验证码建立隔离例外。WebView 禁用文件和 Content 访问、混合内容、下载、任意跳转及 Release 调试；桥只接收一次性验证结果，销毁时立即移除。

## Compose 与图标参考

- Kreate `f02577e862318df26b13abb02d14d8d824a1b947`：播放器层级、MiniPlayer 与页面状态；GPL-3.0，仅学习。
- Metrolist `289ed45d0a429e6e4f19a2a07543b243a0ab2e8b`：Compose 列表和响应式布局；GPL-3.0，仅学习。
- Compose Material Icons：标准导航、账号、媒体和状态语义；Apache-2.0。优先直接复用，不为已有语义重新绘制 SVG。
- Android Material 3、Navigation Compose、伪本地化与 WebView 安全文档优先于第三方行为。

视觉实现采用已批准的暖白、天空蓝和柔和语义容器。只有品牌标识或 Material Icons 确实不存在的专属图形才建立 SVG 母版。`frontend-design` 用于根据现有批准稿约束排版、留白和完成度，不引入 Web 前端运行时。生成式视觉图板只确认形态与方向，准确颜色、字号和组件行为以 [`../DESIGN_SYSTEM.md`](../DESIGN_SYSTEM.md) 为准。
