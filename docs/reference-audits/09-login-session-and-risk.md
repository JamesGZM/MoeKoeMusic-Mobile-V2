# 登录、会话与安全验证参考审计

状态：Accepted。审计日期：2026-08-05。

## 决策范围

本审计固定手机号验证码、密码、扫码、多账号、安全验证、会话提交与退出的协议和 Android 边界。账号资料、签到、VIP 和用户音乐库另按阶段 5B 审计，不在本切片中实现。

## 当前实现与缺口

- `:kugou-api` 已基于 Ktor Client + OkHttp Engine 实现短信和密码登录、临时 AES/RSA 包装、类型化会话、多账号、风险方式与验证提交，以及二维码 key/check Endpoint 和 `0/1/2/4` 类型化状态；`:feature:login` 已实现 ZXing 渲染和生命周期轮询，真机可扫/服务兼容仍待验收，视觉仍需等待新版单状态候选确认后返工。
- `:data` 已通过 Android Keystore AES-256-GCM 保存版本化 `KugouSessionSnapshot`，并实现认证互斥、会话原子提交、退出保留匿名身份，以及密码/风险领域映射。
- `:feature:login` 已拥有短信/多账号、密码与短信/腾讯安全验证状态，并实现验证成功后最多一次的原密码重试；`:app` 已提供非导出的隔离腾讯 Activity，扫码状态机仍待后续原子提交。
- 登录主状态设计稿 `13` 继续作为已确认基线；旧 `19` 至 `22` 横向总览稿的实现基线资格已撤销，新版独立单状态候选仍待确认。交互原型仅保留为历史状态关系参考，不能作为 Compose 图标、尺寸或视觉实现依据。
- 当前 `KugouRequestFactory` 拒绝 HTTP origin，而固定 `captcha_sent.js` 仍使用 `http://login.user.kugou.com`。不得因此全局允许明文流量。

## Android 官方约束

### 网络安全

- [Network Security Configuration](https://developer.android.com/privacy-and-security/security-config) 支持按域声明明文例外；应用基线保持 `cleartextTrafficPermitted=false`，仅 `login.user.kugou.com` 精确域可以为固定短信 Endpoint 开启 HTTP，不包含子域。
- [Cleartext communications](https://developer.android.com/privacy-and-security/risks/cleartext-communications) 明确建议 HTTPS，并只对必要目标设置例外。Ktor 层仍需显式校验 Endpoint origin，不能只依赖 Manifest。
- 登录、验证码、验证提交和会话写入均不自动重放。二维码状态检查由页面生命周期内的显式轮询控制，不使用全局重试插件。

### WebView 安全例外

- [WebView native bridge risks](https://developer.android.com/privacy-and-security/risks/insecure-webview-native-bridges) 要求 HTTPS、校验 scheme/host，并警告 `addJavascriptInterface` 和无 origin 校验的消息通道。
- [Unsafe URI loading](https://developer.android.com/privacy-and-security/risks/unsafe-uri-loading) 要求同时验证 URI scheme 与 host，不能用字符串前缀判断。
- [Unsafe file inclusion](https://developer.android.com/privacy-and-security/risks/webview-unsafe-file-inclusion) 要求禁用文件访问、`file://` 跨域能力和非必要内容访问。

腾讯图形验证是项目唯一允许的登录 WebView：放在非导出的独立 Activity；只加载 HTTPS 且只允许 `turing.captcha.qcloud.com` 及验证实际跳转所需、经真机记录确认的腾讯域；禁止 `addJavascriptInterface`、文件访问、内容访问、文件选择、下载和任意外部导航；消息必须使用 AndroidX WebKit 的 origin allowlist；Activity 只返回成功、取消或类型化失败，不接触 Repository、token、Cookie 或密码。域名单在首次真机兼容验收中只增补实际证据，不预先宽放 `*.qq.com`。

实现按[腾讯云 Web 客户端接入](https://cloud.tencent.com/document/product/1110/36841)使用官方 `https://turing.captcha.qcloud.com/TCaptcha.js`，并通过 AndroidX WebKit `addWebMessageListener` 精确 origin 规则接收类型化回调；CSP、请求拦截和导航策略初始只允许该 HTTPS origin。当前 ELE-AL00 / API 29 已验证最终 Manifest 中 Activity 非导出；真实验证码若请求其他腾讯资源域，只能在用户主动验收取得证据后逐个加入，不使用通配符。

### 会话存储

- [Android Keystore](https://developer.android.com/privacy-and-security/keystore) 保持密钥不可导出；[Cryptography](https://developer.android.com/privacy-and-security/cryptography) 支持当前 AES-256-GCM 方案。
- 登录成功只有在 token、userid、必要 Cookie 解码完成并且加密会话写入成功后才成立。写入失败返回存储错误，不先导航到已登录页面。
- 退出登录保留 GUID、MID 与 dfid，只移除 token、userid 和登录 Cookie；损坏密文继续走既有“删除密文和旧 key、重新匿名初始化”的恢复路径。

## 固定源码审计

### KuGouMusicApi

- 仓库：`MakcRe/KuGouMusicApi`；本地固定提交：`6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb`；许可证：MIT。
- 文件：
  - `module/captcha_sent.js`
  - `module/login_cellphone.js`
  - `module/login.js`
  - `module/login_qr_key.js`
  - `module/login_qr_check.js`
  - `module/get_verify_info.js`
  - `module/verify_user_info.js`
  - `module/sidedt.js`
  - `util/request.js`

采用：Endpoint origin/path、Android/Web 签名、AES 临时 key + RSA 包装、`support_multi=1`、`secu_params` 解密、Cookie 合并、二维码状态 `0/1/2/4`、验证类型 `23/32`，以及服务响应中实际存在的 `sid/edt` 透传。

不采用：Node Axios/Promise 包装、动态 `process.env`、浏览器/WebGL 指纹生成和 `sidedt.js` 的 JavaScript 模拟。固定 `util/request.js` 会在收到 `ssa-code` 响应头后调用 `generateSimulate`，人为生成鼠标轨迹、WebGL、`sid/edt` 并改写响应；这不是服务原始字段，Android 不迁移或伪造该行为。

### `sid/edt` 证据边界

- 原生 Ktor 保留 `ssa-code` 响应头，并只在响应体确实存在时携带 `sid/edt`；领域挑战将后两者建模为可缺失，不把 Node 包装层生成值误写成服务事实。
- 获取验证方式只依赖 `eventid`，可以在 `sid/edt` 缺失时继续；提交验证按固定接口发送服务值或空值，禁止自动生成浏览器行为指纹。
- 普通密码认证不受此边界影响。短信或腾讯二次验证能否在原生直连且空 `sid/edt` 下通过，必须在实现与安全测试完成后由用户在真机主动触发验证；通过前不得把风控闭环标记完成。

### MoeKoeMusic PC

- 本地固定提交：`52c9833afe2e7fedcba8d5b23ff8d1f9731af73a`；许可证：GPL-2.0-only。
- 文件：
  - `src/views/Login.vue`
  - `src/components/RiskVerifyModal.vue`
  - `src/utils/riskVerify.js`

采用：手机号多账号必须显式选择；二维码在已扫码、成功、过期之间分态；风险验证串行；验证成功后原登录最多重试一次；腾讯票据格式为 `KGCodeTX|{ticket,randstr,txappid}`。

不采用：Vue 全局挂载、动态 DOM 清理、全局 Axios 拦截重试、服务端原文 Toast、Font Awesome 图标和浏览器脚本注入。只学习行为，不复制 UI 代码。

### MoeKoeMusic Mobile

- 本地固定提交：`ab71195d4cf3297332490fd37704d1ae8973d4c5`；许可证：GPL-2.0-only。
- 文件：
  - `src/app/login.tsx`
  - `src/features/account/auth.ts`
  - `src/lib/kugou-api/use-axios.ts`
  - `src/lib/kugou-api/session.ts`

采用：手机号格式与脱敏；`data.info_list` 的多账号兼容映射；`ssaCode/sid/edt` 挑战解析；离开二维码页即取消旧轮询；连续三次检查失败转可恢复错误；密码验证成功后只重试一次。

不采用：React Native WebView 注入 HTML、弱类型对象流入页面、错误对象穿透、定时器脱离 ViewModel 生命周期和服务端消息直接展示。只学习移动流程和响应兼容，不迁移 TypeScript 实现。

## 成熟库优先决策

| 决策点 | 候选 | 选择 | 原因与边界 |
| --- | --- | --- | --- |
| HTTP、JSON、协程 | 自写 socket / Retrofit / 现有 Ktor | 复用 Ktor 3.5.1 + OkHttp 5.4.0 + kotlinx.serialization 1.11.0 | 现有成熟传输已覆盖动态 Host、原始 Body、Cookie 和取消；不再造请求栈 |
| 二维码编码 | 自写矩阵 / ZXing Core | ZXing Core 3.5.4，Apache-2.0 | 只把登录 URL 编码为 `BitMatrix`；不引入含扫描 Activity 和相机权限的 Android Embedded 层 |
| 图形验证容器 | 自定义 JS Runtime / 系统 WebView / AndroidX WebKit | 系统 WebView + AndroidX WebKit 1.16.0，Apache-2.0 | WebView 是腾讯 H5 必需例外；WebKit 提供带 origin 规则的兼容消息 API；不引入通用桥 |
| 会话加密 | 明文 DataStore / 新加密库 / 现有 JCA + Keystore | 复用现有 Keystore AES-256-GCM + DataStore | 已有平台实现与真机测试，不增加重复安全依赖 |
| 图片 | 自写下载 / 现有 Coil | 复用 Coil 3.4.0 | 多账号头像沿用现有图片栈；失败显示设计系统占位，不影响选择 |
| 图标 | 原型字符 / 自绘 SVG / Material Icons | 固定 Compose Material Icons | 设计图语义是权威；不复制原型近似图标 |

ZXing Core 仅负责通用二维码算法；二维码颜色、留白、尺寸和无障碍由 Compose/Design System 决定。AndroidX WebKit 仅进入隔离验证能力，不允许普通业务页面使用 WebView。

## 协议与类型化结果

| 操作 | 固定协议 | 成功结果 | 失败/分支 |
| --- | --- | --- | --- |
| 发送短信 | `POST http://login.user.kugou.com/v7/send_mobile_code`，`businessid=5`、`plat=3` | `CodeSent` | 手机号无效、限流、网络、协议拒绝 |
| 验证码登录 | `POST https://loginserviceretry.kugou.com/v7/login_by_verifycode` | `AuthenticatedSession` | `MultipleAccounts`、凭据错误、网络、协议错误 |
| 密码登录 | `POST https://gateway.kugou.com/v9/login_by_pwd` + `x-router: login.user.kugou.com` | `AuthenticatedSession` | `RiskChallenge`、多账号提示、凭据错误 |
| 获取验证方式 | `POST /verifyservice/v3/get_verify_info` | `Sms(type=32)` 或 `Tencent(type=23, txAppId)` | 不支持类型、网络、协议错误 |
| 提交验证 | `POST https://verifyservice.kugou.com/v4/verify_user_info?clientver=11510` | `Verified` | 票据错误、过期、取消、网络 |
| 获取扫码 key | `GET https://login-user.kugou.com/v2/qrcode` | key + 登录 URL | 网络、协议错误 |
| 检查扫码 | `GET https://login-user.kugou.com/v2/get_userinfo_qrcode` | Waiting/Scanned/Expired/Authenticated | 连续失败、未知状态 |

二维码协议子切片已按 KuGouMusicApi `6efe84e` 的 `module/login_qr_key.js`、`module/login_qr_check.js`、`module/login_qr_create.js` 和 Mobile `ab71195` 的 `src/app/login.tsx` 固定实现：key 请求覆盖 `appid=1001` 并保留 `srcappid=2919`、`plat=4`、Web 签名；check 请求不启用传输自动重试；未知状态作为协议错误。状态 4 仅在 token/userid 解码且加密会话提交完成后返回认证成功，扫码 key 和登录 URL 的诊断字符串均脱敏。

- `secu_params` 只能在 `:kugou-api` 用本次请求 AES key 解密；缺 token/userid 是协议错误。
- `info_list` 只映射 `userid/nickname/pic/p_grade`，过滤空或 `0` userid；UI 不读取原始 JSON。
- `20028`、响应体 `ssaCode` 或响应头 `ssa-code` 统一映射 `RiskChallenge(eventId,sid?,edt?)`；未知服务文案进入脱敏诊断，不成为 UI 文案。
- 密码、短信码、腾讯 ticket、扫码 key 只存在于当前内存流程，不写 SavedState、Room、日志或 analytics。

## 架构与生命周期

```text
:feature:login Route / Screen / ViewModel
        │ AuthRepository（稳定领域结果）
        ▼
:data KugouAuthRepository + 登录互斥/会话提交
        │
        ▼
:kugou-api AuthClient / Endpoint / Decoder
        │
        ├── Ktor transport
        └── KugouSessionStore（由 :data 加密实现）

RiskCaptchaActivity（独立平台边界）
        └── ActivityResult：success / cancel / typed failure
```

- 同一 ViewModel 同时只允许一个提交操作；切换登录方式取消旧任务。当前登录页会话内保留已输入手机号，避免验证码、密码和扫码方式往返时重复输入；验证码、密码、风险票据与二维码会话等敏感或模式专属状态仍在离开所属方式时清除，退出登录页后由 ViewModel 生命周期整体释放。
- 二维码轮询间隔 2 秒；离开扫码方式、刷新、返回或 ViewModel 清除时立即取消。连续三次网络/协议检查失败进入错误，用户手动刷新生成新 key。
- 验证成功后只重试触发挑战的密码请求一次；再次出现挑战时停止，防止循环。
- 多账号重试复用当前手机号和短信码，但必须由用户选择 userid；返回登录页或进程死亡即清空。
- `:app` 只注册 `loginGraph` 和处理登录完成/返回事件，不持有表单或认证状态。

## 不采用项

- 不使用真实服务探针发现字段；固定 API、PC 和 Mobile 已覆盖请求与消费层。实现完成后才做最小兼容验收。
- 不全局允许明文流量，不接受任意 HTTP 跳转。
- 不使用 `addJavascriptInterface`、Node、JavaScript Runtime、内嵌 HTTP 服务或通用 WebView API 桥。
- 不自动选择多账号，不持久化密码/短信码/扫码 key，不显示服务端原始错误。
- 不把风险验证做成全局网络拦截器；首个切片只处理登录触发的明确挑战，未来共享时另做审计。

## 验收矩阵

- `:kugou-api`：AES/RSA 和完整请求快照的 Node/Kotlin 固定向量；登录成功、`secu_params` 缺失、多账号、20028、二维码 `0/1/2/4/未知` Decoder 测试。
- `:data`：会话合并、存储失败不报告成功、退出保留匿名身份、并发登录互斥和类型化错误映射。
- `:feature:login`：三种模式、提交锁定、倒计时、切换取消、一次风控重试、多账号显式选择、二维码连续失败与刷新 ViewModel 测试。
- Compose：确认设计图对应的普通、加载、错误、扫码、风控和多账号截图；Material Icons、48dp 触控、contentDescription 与 `1.5×` 字体检查。
- 真机：仅当前已连接的 Huawei ELE-AL00 / API 29；安装、三种入口导航、返回、键盘、旋转/后台恢复、腾讯验证隔离、二维码可扫、会话恢复和退出。当前不启动模拟器。2026-08-05 已通过扫码入口、真实 key 生成、离页切换测试，并将设备截图用项目同版 ZXing 成功解码且确认固定 URL 前缀；截图和 key 未进入仓库。状态 `2→4`、登录会话恢复仍需另一台已登录酷狗设备由用户主动完成。
- 真实服务：固定测试先通过后，使用测试账号手动执行发送短信、验证码登录、多账号、密码/风控与扫码兼容验收；不保存响应正文、手机号、token、Cookie、二维码或验证码。涉及验证码和账号操作必须由用户在真机上主动完成。

以上证据允许继续普通密码登录、风险 UI 和隔离验证容器的可审查实现；风险挑战的 `sid/edt` 兼容性保留为真机验收项，在用户主动触发并取得成功证据前不视为闭环。
