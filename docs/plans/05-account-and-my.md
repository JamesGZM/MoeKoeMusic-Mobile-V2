# 阶段 5B：账号与“我的”

## 目标

完成 PC 端关键账号和音乐库能力在移动端“我的”信息架构中的闭环。

## 实现范围

- 手机验证码、账号密码、扫码登录、会话恢复和退出。
- “我的”顶部用户卡片、签到、VIP 领取和状态刷新。
- 我喜欢、创建歌单、收藏歌单、收藏专辑、关注歌手、关注好友。
- 云盘、播放历史、本地音乐和设置入口。
- 独立用户主页展示资料、等级、VIP、签名、关系统计、听歌概览和公开歌单。
- 用户主页不重复签到、VIP 领取、云盘、本地音乐或设置。

## 视觉与组件基线

- 登录主状态使用 [`../design/mockups/13-login-phone-immersive.png`](../design/mockups/13-login-phone-immersive.png)，顶部 edge-to-edge 且无独立标题栏。
- 验证码、密码和扫码使用统一分段控件与表单 Surface；多账号选择只在接口返回多个账号后出现。
- 普通返回、手机、密码、二维码和安全图标优先复用 Material Icons，不重复生成 SVG。
- 颜色、排版、输入框、按钮、Dialog 和页面状态遵循 [`../DESIGN_SYSTEM.md`](../DESIGN_SYSTEM.md) 与 [`../UI_COMPONENTS.md`](../UI_COMPONENTS.md)。
- 密码、扫码、短信风控、腾讯图形验证和多账号状态设计稿已确认：[`19-login-password-states.png`](../design/mockups/19-login-password-states.png)、[`20-login-qr-states.png`](../design/mockups/20-login-qr-states.png)、[`21-login-risk-verification.png`](../design/mockups/21-login-risk-verification.png) 与 [`22-login-multi-account.png`](../design/mockups/22-login-multi-account.png)。[`login-flow`](../design/prototypes/login-flow/README.md) 原型验证交互与状态关系；Compose 编码仍需满足协议审计和组件门禁。
- 登录先按独立 [`07-login-flow.md`](07-login-flow.md) 和 [`../reference-audits/09-login-session-and-risk.md`](../reference-audits/09-login-session-and-risk.md) 完成纵向闭环，再由本阶段消费稳定会话实现用户资料、签到、VIP 与音乐库。
- 登录后的用户资料、VIP 摘要、刷新、部分失败和退出按 [`../reference-audits/10-user-profile-and-my-session.md`](../reference-audits/10-user-profile-and-my-session.md) 分三层落地：先 `:kugou-api`，再 `:data`，最后 `:feature:my`；不得在资料或资产接口完成前用设计稿内容和假计数填充页面。
- “我的”已有确认设计 `04-my-v4.png`，本切片无需重新生图。未来新增会话失效等未覆盖布局时，必须先使用 `frontend-design` 按既有 Design System 补静态图并确认，之后才能制作必要原型或编码。

## 测试

- 会话恢复、过期、二次验证和脱敏存储测试。
- 签到/VIP 的成功、重复、不可用和风控状态测试。
- 收藏、关注和歌单写操作的成功、失败与受控重试测试。
- “我的”与用户主页导航、状态和截图测试。

## 完成标准

- `04-my-v4.png` 和 `10-user-profile.png` 对应的信息归属准确。
- PC 端签到、VIP、收藏专辑、关注歌手与关注好友没有遗漏。
- 登录失效和写操作失败使用明确、可恢复的 UI 反馈。
