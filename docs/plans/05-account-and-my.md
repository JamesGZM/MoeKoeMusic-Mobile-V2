# 阶段 5B：账号与“我的”

## 目标

完成 PC 端关键账号和音乐库能力在移动端“我的”信息架构中的闭环。

## 当前进度

- 手机验证码、多账号、账号密码、扫码和风控验证的协议、状态机与功能 UI 已形成可运行切片；现有 Compose 仍需按已确认独立单状态图返工，真实扫码 `2→4`、会话恢复和主动风控兼容验收尚未完成，因此登录阶段仍不能标记完成。
- 登录后资料、VIP 摘要、页面恢复刷新、部分失败降级和确认退出已经贯通 `:kugou-api`、`:data` 与 `:feature:my`，并使用真实会话和服务结果。
- “我的”匿名与登录态已按 `04-my-anonymous.png`、`04-my-v4.png` 完成整页视觉结构，并于 2026-08-08 补齐 [`my-content-2026-08-08.md`](../design/evidence/my-content-2026-08-08.md) 的同画布并排、叠加和差异证据：账户卡、品牌头像、签到/VIP、四项快捷入口、收藏与关注统计层级、创建歌单标题/列表/独立空态插画均保留；截图 fixture 提供确认稿示例数据，运行态仍只显示 Repository 的真实资料与明确空态，不把示例计数写入业务状态。
- 匿名与登录态齿轮均已接入唯一 Settings destination，登录态资料区提供独立账号菜单，退出确认不再借用设置齿轮；My → Settings → 主题切换 → Back 已在指定 ELE-AL00 / API 29 真机通过。
- 设置与应用偏好已通过 [`../reference-audits/18-settings-and-preferences.md`](../reference-audits/18-settings-and-preferences.md) 门禁；主题持久化、应用级消费、`09-settings.png` 五个完整分组、14 个一致高度 Item、长页面滚动和关于 Dialog 均已交付。未接能力保留确认稿静态视觉但不写入假偏好，后续随真实消费者逐项接入。
- 用户主页已完成 [`20-user-profile-ui`](../reference-audits/20-user-profile-ui.md) 门禁，并按 [`USER_PROFILE_LAYOUT_SPEC.md`](../design/USER_PROFILE_LAYOUT_SPEC.md) 与 `10-user-profile.png` 落入独立 `:feature:profile`：从已登录“我的”资料区上抛事件，由 `:app` 注册类型安全子页面；Toolbar、资料 Hero、关系统计、编辑资料、听歌概览和三行公开歌单均已按确认稿交付。`user-profile.content.light` 结构 contract、三锚点 probe、13 组状态/适配截图和归一化对照证据已通过，覆盖 Light、Dark、AMOLED、加载、空、错误、离线、两档大字体、长文本与宽屏；所有示例字段只存在于 UI fixture，真实业务仍按后续纵向切片接入。

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
- 旧密码、扫码、短信风控、腾讯图形验证和多账号横向状态稿已撤销实现基线资格；新版独立单状态图见 [`login-v2` 确认稿索引](../design/mockups/candidates/login-v2/README.md)，可直接用于 Compose 返工。[`login-flow`](../design/prototypes/login-flow/README.md) 只保留为历史交互参考。
- 登录先按独立 [`07-login-flow.md`](07-login-flow.md) 和 [`../reference-audits/09-login-session-and-risk.md`](../reference-audits/09-login-session-and-risk.md) 完成纵向闭环，再由本阶段消费稳定会话实现用户资料、签到、VIP 与音乐库。
- 登录后的用户资料、VIP 摘要、刷新、部分失败和退出按 [`../reference-audits/10-user-profile-and-my-session.md`](../reference-audits/10-user-profile-and-my-session.md) 分三层落地：先 `:kugou-api`，再 `:data`，最后 `:feature:my`；不得在资料或资产接口完成前用设计稿内容和假计数填充页面。
- 匿名访问采用硬门禁：签到、领取 VIP、我喜欢、最近播放、云盘、收藏歌单、收藏专辑、关注歌手、关注好友、创建歌单和用户主页等账号资产保持正常入口形态；匿名点击时立即进入现有 `loginGraph`，进入登录前不请求对应账号资产。硬门禁不依赖对应接口是否已经迁移，不能用禁用控件、重复登录说明或假数据替代。
- “我的”已登录主态使用确认设计 [`04-my-v4.png`](../design/mockups/04-my-v4.png)，未登录状态使用 2026-08-07 确认的 [`04-my-anonymous.png`](../design/mockups/04-my-anonymous.png)。未来新增会话失效等未覆盖布局时仍须先补静态设计图并确认，之后才能制作必要原型或编码。

## 测试

- 会话恢复、过期、二次验证和脱敏存储测试。
- 签到/VIP 的成功、重复、不可用和风控状态测试。
- 收藏、关注和歌单写操作的成功、失败与受控重试测试。
- “我的”与用户主页导航、状态和截图测试。
- 匿名态逐项验证所有账号资产进入唯一 `loginGraph`，不发起受认证资产请求；本地音乐与应用级设置作为不触发门禁的反例。
- 登录成功返回原“我的”栈并刷新真实账号数据；匿名截图不得出现重复的“登录后可用 / 同步 / 查看”或“待接入”文案。

## 完成标准

- `04-my-v4.png` 和 `10-user-profile.png` 对应的信息归属准确。
- PC 端签到、VIP、收藏专辑、关注歌手与关注好友没有遗漏。
- 登录失效和写操作失败使用明确、可恢复的 UI 反馈。
