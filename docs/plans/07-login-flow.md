# 阶段 5A：登录纵向闭环

状态：进行中（协议与行为切片保留；视觉完成结论已于 2026-08-07 撤回，等待按页面适配契约重新落地）。确认日期：2026-08-05；适配契约确认日期：2026-08-07。

## 产品定义

用户从“我的”未登录卡片进入独立登录页，可使用手机号验证码、账号密码或酷狗 App 扫码登录。完成语义是认证响应被类型化解码、加密会话提交成功、页面返回上一入口且“我的”刷新为已登录；仅网络成功或仅拿到 token 都不算完成。

本计划包含登录、会话恢复和退出所需基础能力。不包含注册、找回密码、签到、VIP、收藏、关注、云盘和用户详情。

## 用户状态与恢复

- 默认手机号页：校验 11 位手机号，发送成功后开始 60 秒倒计时；发送失败可手动重试。
- 验证码提交：提交中锁定；多账号响应进入账号选择页面，未选择时不能继续；失败保留选择上下文供手动重试。
- 密码提交：凭据错误内联展示；风险挑战中止当前提交，验证成功后最多重试一次；不支持的验证类型提示改用验证码登录。
- 扫码：生成、待扫码、已扫码待确认、过期、连续检查失败和手动刷新分别建模；离开页面立即取消轮询。
- 安全验证：短信验证在原生 UI 完成；腾讯图形验证只在隔离 Activity/WebView 完成；取消返回密码页，不自动重发原请求。
- 会话：进程重建从加密存储恢复；存储失败保持未登录；退出保留匿名设备身份并清除登录凭据。
- 离线：不自动重放登录和验证；显示可恢复的本地文案。权限状态不适用，登录不申请电话、短信、相机或存储权限。

## 视觉设计门禁

登录主状态 `13` 与新版补充单状态图均为已确认基线；旧 `19` 至 `22` 横向总览已撤销实现基线资格：

设计图到运行窗口的尺寸、锚点和验收规则见 [`LOGIN_LAYOUT_SPEC.md`](../design/LOGIN_LAYOUT_SPEC.md)。它要求以 `852 × 1846` 为设计坐标，在 Compact 窗口按当前容器宽度使用唯一比例映射所有组件；不能按真机分辨率逐项修改。现有 Compose 截图在通过确认稿归一化叠加前只属于旧实现回归证据。

| 状态 | 权威设计图 |
| --- | --- |
| 手机号默认、发送与通用结构 | [`13-login-phone-immersive.png`](../design/mockups/13-login-phone-immersive.png) |
| 密码默认、提交、凭据错误、风控触发 | [`login-v2` 确认稿 19](../design/mockups/candidates/login-v2/README.md#19--密码登录) |
| 扫码生成、等待、已扫码、过期、失败 | [`login-v2` 确认稿 20](../design/mockups/candidates/login-v2/README.md#20--扫码登录) |
| 短信默认、提交、错误与腾讯加载、失败返回 | [`login-v2` 确认稿 21](../design/mockups/candidates/login-v2/README.md#21--风险验证) |
| 多账号选择、未选择禁用、提交失败、切换 | [`login-v2` 确认稿 22](../design/mockups/candidates/login-v2/README.md#22--多账号) |
| 离线、通用错误和临时反馈 | [`18-mobile-states-overlays.png`](../design/mockups/18-mobile-states-overlays.png) |

[`login-flow`](../design/prototypes/login-flow/README.md) 是基于已撤销旧稿建立的历史原型，只能参考切换、返回、提交锁定、轮询取消和状态转移；不得用其页面结构、密度或图标覆盖新版确认稿。仅在确有必要验证复杂交互时更新原型。

本轮登录设计与状态图已经完整，不新建原型，直接按适配契约实施。登录不是固有滚动页面：全部确认状态在约 `390 × 845dp`、`1.0×` 字体和无 IME 时滚动范围必须为 `0`；短高度、IME 和大字体只有真实溢出时才启用单一条件滚动。Hero/Surface 重叠不得通过负 `offset` 留下不可见滚动占位。

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
5. 视觉与整体验收：先完成主手机号页面的设计测量、统一尺寸映射、标准视口零滚动和确认稿叠加，再逐状态覆盖密码/扫码/风险/多账号；最后验证短高度/IME 条件滚动、大字体/无障碍、显示矩阵、API 29 真机和文档状态。

每个切片完成对应最快检查和受影响模块测试后立即提交，不混入下一切片。

当前第 1、2、3 项和第 4 项的协议、Repository、ViewModel、生命周期与安全边界实现保留；既有自动测试仍可证明相应行为。2026-08-06 建立的 26 张登录截图只证明当时 Compose 实现内部稳定，未直接与确认稿做归一化叠加，因此不再作为“视觉已完成”的证据。第 5 项从手机号验证码主状态重新开始：先按 `LOGIN_LAYOUT_SPEC.md` 完成设计坐标测量、统一缩放和设计符合度，再扩展到其余状态。二维码真实状态 `2→4`、用户主动风控兼容和真实验证码登录仍保持人工验收项，不在视觉规范切片中触发。

## 验收命令与真机范围

- JVM：`./gradlew :kugou-api:test :data:testDebugUnitTest :feature:login:testDebugUnitTest`
- 格式/静态：`./gradlew spotlessCheck :feature:login:lintDebug :app:lintDebug`
- 截图：`./gradlew --no-configuration-cache :feature:login:validateDebugScreenshotTest`
- 构建：`./gradlew :app:assembleDebug`
- 真机：`adb -s 8KE5T19529025794 install -r app/build/outputs/apk/debug/app-debug.apk` 后执行登录导航和用户主动验证场景。

本阶段不创建或启动模拟器。真实账号、短信、密码、token、Cookie 和验证票据不得进入仓库、测试输出或日志。

## 完成标准

- 三种登录方式及全部确认状态可达，返回和取消行为与设计一致。
- 基准 `1.0×` 场景的固定结构通过确认稿归一化叠加，所有组件使用同一页面缩放系数；截图基准更新有设计符合度证据。
- 全部确认状态在标准视口滚动范围为 `0`；短高度、IME 和大字体只在真实溢出时滚动，条件解除后位置归零。
- 登录成功只发生在加密会话提交完成后；重启恢复、退出和损坏恢复正确。
- 多账号不自动选择，风险验证不循环重试，二维码离页不继续轮询。
- WebView 只存在于非导出的腾讯验证 Activity，安全设置和 allowlist 有设备测试证据。
- 固定向量、单元、截图、构建和当前 API 29 真机验收通过；首次 Endpoint 真实兼容验证已记录且无敏感数据。
