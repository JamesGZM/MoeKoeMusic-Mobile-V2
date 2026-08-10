# 每日 VIP 领取协议与数据准入

状态：**Accepted（协议与数据边界）**。UI mutation states 与已确认双按钮改为单一状态驱动动作仍待用户确认。审计日期：2026-08-10。

## 决策与范围

“我的”页当前的“签到 / 领取 VIP”是确认主态中的静态视觉槽位，不是已经接入的业务能力。此审计只准入登录后的每日概念版 VIP 领取及可选升级所需的 Kotlin 协议、领域和数据边界；不接入 UI、不改变截图/contract、不运行真实服务，也不迁移旧 `youth_vip`。

- **迁移事实**：PC 固定提交 `52c9833afe2e7fedcba8d5b23ff8d1f9731af73a`（GPL-2.0-only）把“签到”接到 legacy `youth_vip`，把“领取 VIP”接到 day 领取后再询问升级；见 `../MoeKoeMusic/src/views/Library.vue:406-444`。
- **迁移事实**：旧 Mobile 固定提交 `ab71195d4cf3297332490fd37704d1ae8973d4c5`（GPL-2.0-only）不调用 legacy，只执行 day 领取；成功或当天已领取才询问用户是否升级；见 `../MoeKoeMusic-Mobile/src/features/account/vip-api.ts:37-92`、`src/app/(tabs)/me.tsx:211-254`。
- **迁移事实**：固定 `KuGouMusicApi@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb`（MIT）将 legacy 标记为“目前不可使用”；day/upgrade 为概念版测试接口，且 upgrade 的前置是 day；见 `../MoeKoeMusic/api/docs/README.md:1497-1526`。
- **Android 新决定**：拒绝 legacy `youth_vip`，采用旧 Mobile 的一次状态驱动 day→可选 upgrade 流程；不自动领取、不以本地日期替代服务端裁决、不增加后台任务或依赖。

PC `Helpers.getVip()` 的自动 day→固定 500ms→upgrade 路径会在失败后仍写本地日期，见 `../MoeKoeMusic/src/components/player/Helpers.js:75-99`；它不属于可迁移行为。

## 协议请求事实

三条请求均使用固定 API 请求层的默认 `https://gateway.kugou.com`、Android signature、设备/客户端 query、会话 token/userId 与设备 headers；见 `../MoeKoeMusic/api/util/request.js:67-159`。固定 API 的 `cookie` 仅被用来构造默认参数/设备上下文，并由包装层维持；Android 应继续复用现有 `KugouSessionSnapshot.requestContext()` 与 `KugouRequestFactory` 的 Cookie 注入，不能复制 Node/Axios 或其行为指纹生成。

| Endpoint | method / origin / path | endpoint 参数和 body | 签名、会话与重试 |
| --- | --- | --- | --- |
| `youth_day_vip` | `POST https://gateway.kugou.com/youth/v1/recharge/receive_vip_listen_song` | query 额外为 `source_id=90139`、`receive_day=YYYY-MM-DD`；无 body。固定模块虽然声明 `application/x-www-form-urlencoded`，但实际把两项置于 `params`。 | Android signature 覆盖 query 与空 body；要求已认证会话；写操作 `KugouRetryMode.None`。见 `../MoeKoeMusic/api/module/youth_day_vip.js:4-12`。 |
| `youth_day_vip_upgrade` | `POST https://gateway.kugou.com/youth/v1/listen_song/upgrade_vip_reward` | query 额外为 `kugouid`（显式 userId 或 cookie userId 的数值）和 `ad_type=1`；无 body。 | Android signature；要求已认证会话；`None`。见 `../MoeKoeMusic/api/module/youth_day_vip_upgrade.js:4-19`。 |
| legacy `youth_vip` | `POST https://gateway.kugou.com/youth/v1/ad/play_report` | JSON body 是固定 ad id 与 `play_start/end`，二者相差 30 秒。 | Android signature 覆盖 JSON bytes；文档称当前不可用；**拒绝迁移**。见 `../MoeKoeMusic/api/module/youth_vip.js:2-16`。 |

固定 Node request 层对这三条 mutation 没有 retry loop；其一次 Axios 调用在 `../MoeKoeMusic/api/util/request.js:182-242`。Android 现有执行器也只能对 `IdempotentRead` 重试，见 `kugou-api/src/main/kotlin/cn/james/music/kugou/api/transport/KugouCallExecutor.kt:21-42`，所以 mutation 必须保持 `None`。

`receive_day` 的唯一固定格式是 `YYYY-MM-DD`。PC 和 Mobile 都通过 `toISOString().split('T')[0]` 生成 UTC 日期，分别见 `../MoeKoeMusic/src/views/Library.vue:418-421` 与 `../MoeKoeMusic-Mobile/src/features/account/vip-api.ts:12-14`；API 源码没有证明服务端日界线。Android 采用可注入 Clock 的 UTC 日期是适配决定，真实服务日界线仍待验证。

## 响应、错误与状态

固定 API 只承诺统一外壳 `{ status, body, cookie, headers? }`，并把 `body.status == 0` 或非零 `error_code` 作为请求拒绝，见 `../MoeKoeMusic/api/util/request.js:30-37,209-240`。它没有给出 day/upgrade 的完整 schema。

| 领域结果 | 已有固定依据 | Android 处理 |
| --- | --- | --- |
| `Claimed`（day） | Mobile 以 body `status == 1` 判定 day 成功，见 `vip-api.ts:45-51`。 | 领取成功；之后可提供 upgrade 确认并刷新现有 VIP 摘要。 |
| `Upgraded`（upgrade） | Mobile 以 body `status == 1` 判定 upgrade 成功，见 `vip-api.ts:73-81`。 | 升级成功；不是另一种 `Claimed`，之后可刷新现有 VIP 摘要。 |
| `AlreadyClaimed` | PC/Mobile 都识别 `error_code=131001`，见 `Library.vue:435-438`、`vip-api.ts:52-56`。 | 中性结果；允许显示可选升级确认，不把它视作可自动重试失败。 |
| `RiskBlocked` | PC/Mobile 都识别 `error_code=20028`，见 `Library.vue:439-441`、`vip-api.ts:57-59`。 | 类型化阻断；不通过降级、重放或复制 Node 生成的 `sid/edt` 绕过。 |
| `AuthenticationRequired` | Endpoint 注释和文档只说明“需要登录”，没有可迁移服务错误码。 | token 非空且 userId 为正数的本地 preflight 不满足时直接返回；服务端未知拒绝不得猜成登出。 |
| `Unavailable` | 仅 legacy 的“目前不可使用”有来源；day/upgrade 没有稳定 unavailable code。 | 只可用于明确已知的 legacy 拒绝；day/upgrade 的未知服务拒绝先保留 `Rejected`/`Protocol`。 |
| `Protocol` | body 非 JSON、结构畸形、缺少必要状态或未知 code。 | 保持类型化，不使用 `error_msg` 字符串作业务分支。 |

离线、超时、连接、HTTP 和响应 `ssa-code` 保留已有 transport 类型；`CancellationException` 原样传播。没有固定来源的服务端 auth/unavailable code、实际 reward 字段、日界线和 upgrade 成功后的权益延迟都列为真实服务未验证项。

## 产品映射与 UI 阻塞

当前 Android 匿名态的两个按钮均导航登录，已登录态传入 `null` 而成为无操作点击，见 `feature/my/src/main/kotlin/cn/james/music/feature/my/MyAccountSection.kt:163,225,231-264`；`MyAction` 也没有领取事件，见 `feature/my/src/main/kotlin/cn/james/music/feature/my/MyAction.kt:3-24`。

因此不能把两个视觉标签机械映射为三个上游 endpoint：

1. “签到”不能接 legacy，因为其固定来源已明确不可用。
2. 若用户确认将确认稿中的双按钮合并/改为单动作，推荐文案与行为为“签到领取 VIP”：day 成功或当天已领取后展示升级确认；确认才 upgrade。
3. 若用户坚持双按钮，必须先确认每个按钮的状态语义、upgrade 是否可以独立触发及已领取后的禁用/反馈，不以 PC 的旧 legacy 映射掩盖冲突。

现有 `my.authenticated.default` 仅覆盖静态主态，不覆盖提交中、upgrade 确认、成功、当天已领取、风控、重新登录或重试状态，见 `docs/design/contracts/my.authenticated.default.properties:1-65`。这些状态必须先有用户确认的静态设计和 contract，才能进入 Compose 实现。

## 领域、并发与模块边界

后续最小状态机为：`Anonymous → RequireLogin`，`Ready → ClaimingDay`，day 的 `Claimed | AlreadyClaimed → OfferUpgrade`，确认后 `Upgrading`；Risk/Auth/Unavailable/Failure 进入对应类型化反馈并只提供用户显式重试。取消确认不写业务状态，取消协程不显示失败。

```text
:feature:my  MyAction / MyViewModel / immutable claim UI state
      │       (仅在 UI 设计确认后)
      ▼
:core:model  DailyVipClaimRepository + typed result/error
      ▼
:data        session preflight、UTC date、DTO → domain、Hilt binding
      ▼
:kugou-api   narrow youth request builder/client/decoder + fake transport tests
```

- `:kugou-api` 负责 origin/path/query/body、签名、response code 与 transport 错误，不能依赖 Compose。
- `:data` 以现有 `KugouSessionProvider` 初始化一次会话；token 非空且正 userId 才发写请求。每次 action 使用会话/日期快照，账号切换、logout 或新 generation 使旧结果失效。
- `:core:model` 不暴露 `error_code`、JSON、Cookie 或 request spec；新 port 不应塞入只读 `UserProfileRepository`。
- `:feature:my` 不读 DataStore/网络/会话；单飞 action job，重复点击无第二次请求。成功后请求既有 profile refresh，但 profile summary 仍是读取结果，不是假定的领取凭据。

## 原子实施与测试矩阵

1. **协议**：`KugouYouthVipRequestBuilder/Client/Decoder`，以 fake transport 快照只覆盖已准入的 day/upgrade 两条 request；legacy 通过固定源码/本审计以及“没有 legacy public API 或 request id”的架构回归证明未迁移，不能为它构造第三条 request。验证 Android 签名覆盖精确 UTF-8 body、Cookie/会话注入、`None` retry、`131001`/`20028`/畸形/HTTP/SSA/cancellation。
2. **领域与数据**：新增窄 `DailyVipClaimRepository`，覆盖匿名零 transport、会话初始化失败、UTC date、claimed/already/risk/auth/protocol 映射、day→upgrade 前置、取消、logout/账号切换和迟到结果隔离。
3. **UI（Blocked）**：用户确认单动作或双动作和所有 mutation 图后，再建立 design contract、ViewModel state machine、Dialog/Snackbar、48dp/无障碍、1×/2× screenshot；覆盖单飞、确认/取消、成功刷新、风险/鉴权、失败 Retry 与不干扰普通资料刷新。

真实服务与真机仅在上述离线测试通过后、由用户以专用测试账号受控执行：实际登录会话、日界线、daily 已领取、upgrade、风控、服务端 auth/unavailable 分类与权益摘要刷新。不得记录 token、Cookie、签名值、完整响应正文或账号资料。

## 非目标

- 不迁移 legacy 广告播放报告、自动领取、固定延迟或本地“今日已领取”持久门闩。
- 不修改登录、安全验证、播放、音质解析、DataStore、缓存或 VIP 摘要读取语义。
- 不以当前 VIP 徽标、服务端错误文本或旧 PC 自动路径推断领取状态。
