# 阶段 5A：登录纵向闭环

状态：进行中（密码、安全验证 UI 与隔离腾讯容器已完成）。确认日期：2026-08-05。

## 产品定义

用户从“我的”未登录卡片进入独立登录页，可使用手机号验证码、账号密码或酷狗 App 扫码登录。完成语义是认证响应被类型化解码、加密会话提交成功、页面返回上一入口且“我的”刷新为已登录；仅网络成功或仅拿到 token 都不算完成。

本计划包含登录、会话恢复和退出所需基础能力。不包含注册、找回密码、签到、VIP、收藏、关注、云盘和用户详情。

## 用户状态与恢复

- 默认手机号页：校验 11 位手机号，发送成功后开始 60 秒倒计时；发送失败可手动重试。
- 验证码提交：提交中锁定；多账号响应打开选择 Sheet，未选择时不能继续；失败保留选择上下文供手动重试。
- 密码提交：凭据错误内联展示；风险挑战中止当前提交，验证成功后最多重试一次；不支持的验证类型提示改用验证码登录。
- 扫码：生成、待扫码、已扫码待确认、过期、连续检查失败和手动刷新分别建模；离开页面立即取消轮询。
- 安全验证：短信验证在原生 UI 完成；腾讯图形验证只在隔离 Activity/WebView 完成；取消返回密码页，不自动重发原请求。
- 会话：进程重建从加密存储恢复；存储失败保持未登录；退出保留匿名设备身份并清除登录凭据。
- 离线：不自动重放登录和验证；显示可恢复的本地文案。权限状态不适用，登录不申请电话、短信、相机或存储权限。

## 视觉设计门禁

所有实现图均为已确认状态，确认日期 2026-08-05：

| 状态 | 权威设计图 |
| --- | --- |
| 手机号默认、发送与通用结构 | [`13-login-phone-immersive.png`](../design/mockups/13-login-phone-immersive.png) |
| 密码默认、提交、凭据错误、风控触发 | [`19-login-password-states.png`](../design/mockups/19-login-password-states.png) |
| 扫码生成、等待、已扫码、过期、失败 | [`20-login-qr-states.png`](../design/mockups/20-login-qr-states.png) |
| 短信/腾讯安全验证、失败、取消、成功 | [`21-login-risk-verification.png`](../design/mockups/21-login-risk-verification.png) |
| 多账号选择、未选择禁用、提交失败、切换 | [`22-login-multi-account.png`](../design/mockups/22-login-multi-account.png) |
| 离线、通用错误和临时反馈 | [`18-mobile-states-overlays.png`](../design/mockups/18-mobile-states-overlays.png) |

[`login-flow`](../design/prototypes/login-flow/README.md) 已在设计图确认后建立，只验证切换、返回、提交锁定、轮询取消和状态转移。页面结构、层级、密度和图标以设计图为准；原型中的字符和近似图标禁止进入 Compose。

视觉沿用 MoeKoe Air 的明亮蓝色、沉浸式顶部、统一分段控件和表单 Surface。Token 来自 `:core:designsystem`，正式图标使用固定 Material Icons，二维码使用 ZXing 数据矩阵并按设计图绘制。

## 技术设计

详细证据和安全边界见 [`../reference-audits/09-login-session-and-risk.md`](../reference-audits/09-login-session-and-risk.md)。

- 新增 `:feature:login`，拥有 Navigation、Route、Screen、ViewModel、UI model 和测试；不依赖 `:app` 或其他 Feature。
- `:kugou-api` 新增 Endpoint、临时加密上下文、DTO/Decoder 和类型化 `KugouAuthClient`；动态 JSON 不越过模块边界。
- `:data` 提供 `AuthRepository`，串行化登录，会话响应只通过一次加密写入提交；退出保留匿名身份。
- `:app` 只组合 `loginGraph`、从“我的”触发导航，并承载非导出的 `RiskCaptchaActivity` 平台入口。
- 新依赖限定为 ZXing Core 3.5.4（Apache-2.0，二维码编码）与 AndroidX WebKit 1.16.0（Apache-2.0，隔离验证兼容安全 API）。现有 Ktor、Serialization、Coil、DataStore 与 Keystore 直接复用。
- 网络默认拒绝明文；仅固定短信 Host 设置精确域例外。所有认证写操作 `retryMode=None`。

## 实施切片与原子提交

1. 协议与领域基础：加密向量、登录 Endpoint、Decoder、Auth 结果和会话合并测试。
2. 短信纵向闭环：`AuthRepository`、独立 Feature、验证码登录、多账号、退出/恢复与真机导航。
3. 密码与安全验证：一次重试状态机、短信验证、隔离腾讯 Activity 和安全测试。
4. 扫码闭环：ZXing 编码、生命周期轮询、过期/失败恢复和真机可扫验收。
5. 视觉与整体验收：全部确认状态截图、大字体/无障碍、API 29 真机端到端和文档状态更新。

每个切片完成对应最快检查和受影响模块测试后立即提交，不混入下一切片。

当前已完成第 1、2 项、第 3 项，以及第 4 项的协议/领域子切片：密码请求、风险挑战/方式/提交协议、Repository 领域基础、密码表单、一次重试 ViewModel、短信验证 UI、基于 AndroidX WebKit 的非导出腾讯 Activity、二维码 key/check Endpoint、`0/1/2/4` 类型化 Decoder，以及扫码成功后的原子会话提交。普通、凭据错误、风险入口、短信/腾讯验证恢复与 `1.5×` 字体已有截图基准；密码入口、系统返回和腾讯 Activity 非导出门禁已通过 ELE-AL00 / API 29 真机测试且未提交真实凭据。第 4 项仍待 ZXing 编码、Compose 状态、2 秒生命周期轮询、连续三次失败恢复和真机可扫验收；用户主动风控兼容验收也尚未完成。固定 Node 请求层生成的模拟 `sid/edt` 不迁移，原生响应缺失和腾讯实际资源域名按审计保留为真机验收项。

## 验收命令与真机范围

- JVM：`./gradlew :kugou-api:test :data:testDebugUnitTest :feature:login:testDebugUnitTest`
- 格式/静态：`./gradlew spotlessCheck :feature:login:lintDebug :app:lintDebug`
- 截图：`./gradlew --no-configuration-cache :feature:login:validateDebugScreenshotTest`
- 构建：`./gradlew :app:assembleDebug`
- 真机：`adb -s 8KE5T19529025794 install -r app/build/outputs/apk/debug/app-debug.apk` 后执行登录导航和用户主动验证场景。

本阶段不创建或启动模拟器。真实账号、短信、密码、token、Cookie 和验证票据不得进入仓库、测试输出或日志。

## 完成标准

- 三种登录方式及全部确认状态可达，返回和取消行为与设计一致。
- 登录成功只发生在加密会话提交完成后；重启恢复、退出和损坏恢复正确。
- 多账号不自动选择，风险验证不循环重试，二维码离页不继续轮询。
- WebView 只存在于非导出的腾讯验证 Activity，安全设置和 allowlist 有设备测试证据。
- 固定向量、单元、截图、构建和当前 API 29 真机验收通过；首次 Endpoint 真实兼容验证已记录且无敏感数据。
