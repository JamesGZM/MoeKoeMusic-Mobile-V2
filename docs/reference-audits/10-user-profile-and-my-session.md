# 用户资料与“我的”会话态参考审计

状态：Accepted。审计日期：2026-08-05。

## 决策范围

本审计固定登录完成后“我的”页顶部用户卡片所需的用户资料、VIP 摘要、刷新、部分失败、会话失效提示与退出登录边界。签到、VIP 领取、用户音乐库、云盘和独立用户主页仍按阶段 5B 的后续切片分别审计与实现；本切片不得用占位数字伪装这些能力已经接入。

登录主流程已由 [`09-login-session-and-risk.md`](09-login-session-and-risk.md) 固定。本审计只消费已提交的加密会话，不改变登录协议或会话存储格式。

## 当前实现与缺口

- `AuthState.Authenticated` 目前只有稳定 `userId`；昵称、头像、签名、听歌时长和 VIP 状态不能从登录响应或设计稿推断。
- `:feature:my` 当前是静态页面，尚未持有账户 UI 状态，也没有资料 Repository、刷新或退出确认。
- `AuthRepository.logout()` 已能原子清除 token、userid 与登录 Cookie，同时保留 GUID、MID 和 dfid；“我的”只需调用稳定领域接口，不能直接操作会话存储。
- 已确认设计稿 [`../design/mockups/04-my-v4.png`](../design/mockups/04-my-v4.png) 是已登录主态的唯一视觉基线；[`../design/mockups/18-mobile-states-overlays.png`](../design/mockups/18-mobile-states-overlays.png) 约束加载、错误与覆盖层；未登录入口进入已确认的 [`../design/mockups/13-login-phone-immersive.png`](../design/mockups/13-login-phone-immersive.png)。
- 现有设计已覆盖本切片，不重新生图。以后新增会话失效等未覆盖布局时，必须先依 `AGENTS.md` 使用 `frontend-design` 在 MoeKoe Air 体系内补静态设计图并确认，之后才能制作交互原型或 Compose 页面。

## 功能与状态规格

### 首次进入与刷新

1. Route 获取 `MyViewModel`，Screen 只消费不可变 `StateFlow`。
2. 页面进入时先读取 `AuthRepository.currentState()`；匿名态展示登录入口，不请求资料或 VIP。
3. 已认证态并发请求用户资料与 VIP 摘要。资料成功即可展示用户卡片；VIP 失败只隐藏或标记 VIP 摘要不可用，不把整个页面判为失败。
4. 页面重新获得焦点和用户下拉刷新时重载，旧请求必须可取消；后发请求结果不得被先发请求覆盖。
5. 刷新期间保留已显示的资料并展示刷新指示；首次加载才使用骨架/占位，不用全屏阻断现有本地入口。

### 失败与恢复

| 状态 | 页面行为 | 恢复入口 | 设计依据 |
| --- | --- | --- | --- |
| 匿名 | 顶部登录卡片；本地音乐、历史和设置仍可进入 | “登录”进入 `loginGraph` | `18-mobile-states-overlays.png`、`13-login-phone-immersive.png` |
| 首次加载 | 用户卡片骨架，非账号本地入口可用 | 自动完成或进入错误 | `18-mobile-states-overlays.png` |
| 已登录 | 昵称、头像、可用资料与 VIP 摘要使用真实响应 | 下拉刷新、进入用户主页 | `04-my-v4.png` |
| VIP 部分失败 | 保留资料卡，不伪造 VIP；显示弱提示或省略徽标 | 下拉刷新 | `04-my-v4.png`、`12-feedback-components-v2.png` |
| 资料失败/离线 | 若已有资料则保留并显示可恢复反馈；无缓存时显示内联错误 | “重试”或下拉刷新 | `18-mobile-states-overlays.png`、`12-feedback-components-v2.png` |
| 会话不可用 | 不用网络/协议错误猜测退出；只有会话初始化失败或未来 Endpoint 明确映射的认证失效才展示重新登录提示 | 用户确认后退出并进入登录 | `18-mobile-states-overlays.png`、`11-dialog-components.png` |
| 退出中 | 确认 Dialog 关闭后锁定重复退出操作 | 等待原子会话写入 | `11-dialog-components.png` |
| 退出失败 | 保留已登录 UI 和会话，不提前导航 | `MoeSnackbar` 重试 | `12-feedback-components-v2.png` |

服务端普通拒绝、5xx、超时、离线和响应字段缺失都不能自行解释为 token 过期，也不能自动清除本地会话。未来若固定源码或兼容验收确认认证失效码，应在 Endpoint Decoder 中建立具名类型并补审计与测试，UI 不使用字符串判断。

### 退出登录

- 设置入口或用户卡片菜单触发明确的品牌 Dialog；确认前不改 UI 状态。
- `AuthRepository.logout()` 成功后统一切换匿名态、清空仅内存的资料并重置后续账号音乐库状态；失败时保持原状态。
- 退出不删除匿名设备身份、本地音乐、播放队列或应用设置。
- 本切片不调用服务端退出 Endpoint；固定参考实现没有证明它是本地会话失效的必要前置条件，额外网络写操作也不应凭空引入。

## Android 官方约束

- [Android architecture recommendations](https://developer.android.com/topic/architecture/recommendations) 要求 UI 由 ViewModel 暴露状态、数据层由 Repository 负责，并使用生命周期感知的状态收集。本项目继续使用 `StateFlow`、`collectAsStateWithLifecycle` 和单向事件。
- [ViewModel overview](https://developer.android.com/topic/libraries/architecture/viewmodel) 用于让页面状态跨配置变化保留，并让请求随 ViewModel 生命周期取消；不在 Composable 中持有 Job 或 Repository。
- [Lifecycle in Compose](https://developer.android.com/topic/libraries/architecture/lifecycle) 用于页面重新可见时的受控刷新；重复进入不能创建脱离生命周期的并发任务。
- [Ktor client requests](https://ktor.io/docs/client-requests.html) 已覆盖动态 Host、GET/POST、Header、Cookie、Body 和协程取消。继续复用现有 Ktor 3.5.1 + OkHttp Engine，不增加 Retrofit、手写 socket 或页面直连。

## 固定源码审计

### KuGouMusicApi

- 仓库：`MakcRe/KuGouMusicApi`；本地固定提交：`6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb`；许可证：MIT。
- 本地消费副本：`../MoeKoeMusic-Mobile/api`，固定提交 `283f1e97b110726b208a64b486a657c0fc0a6126`。
- 文件：
  - `module/user_detail.js`
  - `module/user_vip_detail.js`
  - `util/crypto.js`
  - `util/request.js`

采用：`user_detail` 的 RSA 零填充 envelope、Android 签名、Cookie、`x-router` 和请求字段；`user_vip_detail` 的独立 HTTPS origin 与 `busi_type=concept`；两项均为已认证读取。

不采用：Node 包装、Axios、动态配置、任意响应对象和日志正文。协议只迁移到 `:kugou-api`，动态 JSON 容错不进入领域/UI。

### MoeKoeMusic PC

- 本地固定提交：`52c9833afe2e7fedcba8d5b23ff8d1f9731af73a`；许可证：GPL-2.0-only。
- 文件：`src/views/Library.vue`。

采用：资料与 VIP 分开加载；用户卡片展示昵称、头像、等级/VIP、签名、关系数据和听歌时长；账号能力归属“我的”。

不采用：Vue 状态、CSS、Font Awesome、生日彩蛋、鼠标悬停行为和桌面端高密度资料卡。只学习字段消费和产品归属，不复制 GPL UI 实现。

### MoeKoeMusic Mobile

- 本地固定提交：`ab71195d4cf3297332490fd37704d1ae8973d4c5`；许可证：GPL-2.0-only。
- 文件：
  - `src/app/(tabs)/me.tsx`
  - `src/features/account/user-api.ts`
  - `src/features/account/vip-api.ts`

采用：页面重新聚焦时检查会话；资料与 VIP 并发；详情为必需结果、VIP 为可降级结果；刷新请求需要防止旧结果覆盖；页面只消费稳定 `UserProfile`。

不采用：React hooks 状态、Promise 对象穿透、Expo/Tamagui/LinearGradient、Ionicons、服务端错误原文和 UI 内直接启动 API。只学习移动生命周期与失败分层，不迁移 TypeScript 实现。

## 协议与字段映射

### 用户资料

`POST https://gateway.kugou.com/v3/get_my_info?plat=1`，Headers 包含 `Content-Type: application/json` 与 `x-router: usercenter.kugou.com`，使用 Android 签名与会话 Cookie。签名必须覆盖与实际发送完全相同的 UTF-8 JSON bytes；Body 保持固定源码的字段顺序，不能签名后重新序列化。

Body：

| 字段 | 值与来源 |
| --- | --- |
| `visit_time` | 当前 Unix 秒 |
| `usertype` | 固定 `1` |
| `p` | 对 `{token, clienttime}` JSON 执行现有 RSA raw zero-padded 加密并转大写十六进制 |
| `userid` | 已认证会话 userId 的数值形式 |

`p` 中的键名是 `clienttime`，Body 键名是 `visit_time`；二者值相同。不得误写成登录协议使用的其他字段名。现有 `KugouRsa.encryptRawZeroPadded()` 足以实现，不引入新加密依赖；应把公共调用包装为该 Endpoint 的窄函数，避免 UI/data 构造加密 JSON。

主资料领域映射：`userid`、`nickname`、`pic`→`avatarUrl`、`bg_pic`→`backgroundUrl`、`descri`→`signature`、`fans`、`follows`、`duration`→`listenMinutes`。数字字段缺失按 `0` 容错，图片/签名缺失按 `null`/空值；`userid` 或 `nickname` 缺失不能用设计稿文案替代，昵称只能使用稳定的本地“用户 {userId}”降级格式。

### VIP 摘要

`GET https://kugouvip.kugou.com/v1/get_union_vip?busi_type=concept`，使用 Android 签名和会话 Cookie。

读取 `data.busi_vip` 中 `is_vip == 1` 的记录；`product_type` 大小写归一后仅把 `svip` 映射为 `SVIP`，其他有效权益显示 `VIP`。成功响应中的合法空数组表示当前没有有效权益；字段缺失、结构畸形或请求失败表示“VIP 摘要不可用”。两者必须可区分，但都不能让主资料失败。

## 领域、数据与 UI 边界

```text
:feature:my MyRoute / MyScreen / MyViewModel
        │ AuthRepository + UserProfileRepository
        ▼
:data KugouAuthRepository + KugouUserProfileRepository
        │
        ▼
:kugou-api KugouUserClient / Endpoint decoders
        │
        ├── existing KugouCallExecutor
        ├── existing Ktor transport
        └── encrypted KugouSessionProvider
```

- `:kugou-api` 返回协议 DTO/类型化错误；`:data` 合并资料和可选 VIP 为稳定领域 `UserProfile`；`:feature:my` 再转换为 UI 状态。
- 资料与 VIP 都使用 `KugouRetryMode.IdempotentRead`。现有执行器只对 timeout 和 HTTP 5xx 以 250/500ms 延迟重试、总计最多 3 次；离线、连接失败、4xx、协议错误和服务拒绝不重试。
- ViewModel 串行化显式刷新或使用递增 generation；新刷新取消旧 Job。退出与刷新互斥，退出开始后忽略旧资料结果。
- 第一切片不持久缓存资料，避免引入缓存陈旧/跨账号污染。后续若音乐库需要离线资料缓存，必须以 userId 分区并另审计存储生命周期。

## 视觉实现边界

- 已登录顶部卡片严格使用 `04-my-v4.png` 的层级、密度与操作归属，颜色、字号、间距、圆角来自 `:core:designsystem`，不从 PNG 采样散落常量。
- 头像使用现有 Coil 3.4.0；加载失败使用已批准占位，不新增下载器。标准账号、设置、退出、刷新与 chevron 语义使用固定 Compose Material Icons。
- 当前切片只接入真实可用的用户卡片字段与退出。签到、领取 VIP、收藏/关注计数和歌单数量在对应 Repository 完成前保持明确禁用、隐藏或既有占位结构，不显示虚构数字。
- Screen 同时提供普通、匿名、首次加载、已有内容刷新、部分失败、完全失败、退出确认和 `1.5×` 字体 Preview/截图基准；交互目标不小于 `48dp`，图标有 `contentDescription`。

## 依赖与许可证结论

不新增依赖。复用 Ktor/OkHttp、kotlinx.serialization、Coroutines、Hilt、Compose Material Icons 和 Coil；均已在工程依赖审计中记录为 Apache-2.0。KuGouMusicApi 仅迁移目标 Endpoint 的 MIT 协议信息并保留来源；PC/Mobile GPL-2.0-only 只作行为和产品语义参考，不复制代码或视觉资产。

## 不采用项

- 不从登录响应、设计稿昵称或本地 userId 猜测完整用户资料。
- 不把 VIP 请求失败升级为整页失败，不把 VIP 未知显示成“非会员”。
- 不在泛化网络错误时自动退出，不用服务端字符串判断会话失效。
- 不为两个读取 Endpoint 引入 Retrofit、新 RSA 库、Node、WebView、JavaScript Runtime 或额外后台任务。
- 不在本切片提前实现签到、VIP 领取、音乐库或用户主页，也不以假计数满足 `04-my-v4.png`。
- 不使用真实服务探针发现字段；固定 Endpoint、调用层和消费层已足够。实现与固定测试通过后，才由用户在真机登录后主动执行最小兼容验收。

## 验收矩阵

- `:kugou-api`：`user_detail` RSA Node/Kotlin 固定向量；origin/path/query/header/body/cookie/签名快照；资料字段缺失与类型漂移；VIP active/empty/malformed；timeout/5xx 重试和普通拒绝不重试。
- `:data`：匿名态不请求；主资料成功 + VIP 成功、失败、空数组；主资料失败；不同 userId 不串资料；退出成功/存储失败；旧刷新不覆盖新状态。
- `:feature:my`：匿名→登录导航→返回刷新、已登录加载、保留内容刷新、VIP 部分失败、完全失败重试、退出确认/取消/失败/成功和进程恢复 ViewModel 测试。
- Compose：`04` 主态、匿名、加载、部分失败、完全失败、退出 Dialog 与 `1.5×` 字体截图；Material Icons、48dp 触控、TalkBack 描述与 edge-to-edge 检查。
- 真机：仅已连接 Huawei ELE-AL00 / API 29；安装、登录返回“我的”、后台/前台刷新、弱网恢复、图片失败、退出取消/失败保持、退出成功回匿名态和返回栈。当前不启动模拟器。
- 真实服务：只有固定测试与真机静态流程通过后，用户使用测试账号主动触发资料/VIP 读取；不记录响应正文、token、Cookie、手机号或头像 URL。

以上产品语义、技术栈、数据流、失败恢复和测试矩阵无关键待定项。允许先实现 `:kugou-api` 用户资料/VIP 读取，再实现 `:data` 聚合，最后实现 `:feature:my` 账户 UI；每个通过验证的切片分别原子提交。
