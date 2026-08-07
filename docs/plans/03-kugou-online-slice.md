# 阶段 4：酷狗在线闭环

状态：进行中。技术审计已通过，参考审计见 [`../reference-audits/04-kugou-online-slice.md`](../reference-audits/04-kugou-online-slice.md)。

## 目标

以最小纵向闭环验证酷狗协议迁移、数据层和播放器在线来源的组合方式。

## 实现范围

- 迁移平台常量、设备身份、匿名设备注册和会话保存。
- 迁移搜索和歌曲地址解析所需签名、加密、Header、Cookie 与响应兼容。
- 建立 Search Repository 和在线播放来源解析接口。
- 实现最小搜索界面，搜索结果可以交给已稳定的播放内核。
- 不在本阶段扩展首页、登录、收藏或完整详情页。

## 用户入口与完成语义

- 首页搜索框进入独立 Search Route，搜索不加入底部 Tab。
- 首版只搜索歌曲；提交空白关键词不发请求。
- 新搜索取消旧搜索，旧响应不得覆盖新结果；分页失败保留已加载内容并允许重试。
- 点击可播放结果后，地址解析成功才调用 `PlaybackController.playNow`；无版权、VIP、风控、网络和协议错误显示类型化 Snackbar。
- 播放 URL 是短期值，不写入 Room；失效时由来源解析器重新请求一次，仍失败则报告播放错误。

## 技术栈与模块边界

- 本条已由 2026-08-05 的客户端选型补审替代：使用 Ktor Client + OkHttp Engine + Kotlin Serialization，详见 `../reference-audits/07-kugou-http-client.md`。
- `:kugou-api` 是纯 JVM 模块，拥有协议配置、加密签名、Endpoint、DTO、Cookie 与传输；不依赖 Android、Compose 或 Media3。
- `:data` 实现设备身份和加密会话存储、Search Repository、DTO/Domain 映射与 `Kugou` 播放来源解析。
- `:feature:search` 只消费搜索领域状态；`:playback` 继续只依赖 `PlaybackSourceResolver`，不直接调用网络。
- App 只新增 `INTERNET` 权限，阶段 4 Endpoint 全部使用 HTTPS，不放开 cleartext。

首批稳定领域接口：

```kotlin
data class Song(
    val id: String,
    val hash: String,
    val title: String,
    val artistName: String,
    val albumId: String?,
    val albumTitle: String?,
    val durationMs: Long,
    val artworkUrl: String?,
)

data class SearchPage(
    val items: List<Song>,
    val page: Int,
    val hasMore: Boolean,
)

interface SearchRepository {
    suspend fun searchSongs(keyword: String, page: Int): Result<SearchPage>
}
```

网络 DTO 不作为公共 API。设备 `GUID/MID/dfid`、Cookie、签名和播放 URL 不进入 `Song`，日志只允许脱敏摘要。

## 数据流与失败恢复

1. 首次请求在单飞锁内读取或生成 GUID/MID，并保存后再调用 `register_dev`。
2. 注册成功且响应包含 dfid 后原子更新加密会话；失败保留 GUID/MID，下一次用户操作按错误类型重试。
3. 搜索通过统一 RequestFactory 注入身份、时间、平台参数、签名与 Router Header。
4. 选歌先请求 `privilege_lite` 得到可用资源，再调用 `song_url`；无可用地址时映射为无版权或 VIP，不制造空 URL。
5. Ktor Transport 保留 Set-Cookie，KugouSession 显式合并；协议层忽略未知字段，但关键字段缺失返回 `ProtocolError`。
6. 超时和 5xx 仅对幂等读取最多重试两次并指数退避；签名失败、风控、4xx 和解析错误不自动重放。
7. 地址解析成功后构造现有 `PlaybackItem` 并交给播放内核；Media3 是运行时播放状态唯一来源。

## 实施顺序

1. 固定 Node fixture，迁移 MD5、SHA-1、AES-CBC、RSA、MID、三类签名与请求规范化，并完成 Node/Kotlin 对照测试。
2. 建立类型化配置、请求/响应、Cookie 与错误模型；使用 Fake Transport 完成快照测试。
3. 实现 Android Keystore 会话存储与匿名设备注册，验证并发单飞和失败恢复。
4. 实现歌曲搜索 DTO、映射、Repository 与独立 Search Route。
5. 实现 privilege/song_url 来源解析并接入 `PlaybackController.playNow`。
6. 最后才运行手动真实接口和真机在线播放验证；普通 CI 全程离线。

## 测试

- KuGouMusicApi 固定版本与 Kotlin 的加密、签名、参数和请求快照对照。
- DTO 缺失字段、类型漂移、风控、超时和无版权映射测试。
- 匿名注册、搜索、地址解析 Repository 测试。
- 真实接口验证单独标记，不阻塞普通 JVM 单测。

## 当前实施结果

- `:kugou-api` 已建立独立 Kotlin 的 MD5、SHA-1、MID、playlist AES-CBC、RSA PKCS#1 v1.5 和请求签名基础。
- 使用完全虚构的 GUID、MID、dfid、关键词和歌曲 Hash，从 `KuGouMusicApi@6efe84e` 直接生成固定 Node 输出；Kotlin 已逐项对齐摘要、MID、Android/Register/Web 签名、带 UTF-8 Body 签名、`signKey` 和 AES 密文。
- RSA 使用相同公开协议公钥验证 1024-bit PKCS#1 加密块和随机填充行为；没有迁移任何账号、Cookie 或无关平台配置。
- 已建立 `KugouRequestSpec`、`KugouPreparedRequest`、`KugouTransport`、`KugouRawResponse` 和类型化协议结果；Endpoint 构造与实际网络执行不再耦合。
- `KugouRequestFactory` 统一注入平台、身份、时间、认证参数、协议 Header、Cookie、`signKey` 和签名；固定搜索请求与 Node 快照一致，并在 Transport 前拒绝明文 Endpoint。
- Cookie 合并保留值中的 `=`、支持服务端删除且拒绝 CR/LF 注入；请求、上下文、响应和 Cookie 的 `toString()` 均不输出敏感值。
- JSON 解码要求顶层 Object，并区分 HTTP、风控、畸形响应和服务端拒绝；服务端正文不会进入错误对象。
- Fake Transport 验证请求可离线捕获；Ktor Client 使用 10 秒连接、15 秒读取和 20 秒整体超时，OkHttp Engine 关闭隐式连接重试，协程取消传播到底层 Call。
- 网络异常已映射为离线、超时和连接错误。只有标记为幂等读取的超时或 5xx 最多额外尝试两次，退避为 250ms、500ms；4xx、离线、连接、风控和协议错误不自动重放。
- `register_dev`、歌曲 `search`、`privilege_lite`、`song_url` 四个 Endpoint 已按固定源码构造；歌曲地址的 `signKey` 与完整签名继续通过固定 Node 输出验证。
- 设备身份、会话端口、注册 AES/RSA 编解码和匿名初始化单飞已实现；身份先保存，注册成功并落库后才返回 Ready，恢复到已有 dfid 时不重复请求。
- 2026-08-05 首次真实验证发现固定 Android 搜索在匿名注册后仍返回 `152`，独立 Go 实现也可复现；该接口不再作为匿名搜索完成依据。
- 经 SPlayer-Next、UnblockNeteaseMusic 与当前服务补审，新增不携带设备身份的 HTTPS WebFilter 搜索。真实“设备注册 → 匿名歌曲搜索”测试已返回非空列表并通过。
- `:data` 已使用独立 DataStore 与 Android Keystore AES-256-GCM 实现会话端口，密文损坏和 key 失效会安全清除后重新初始化；API 29 真机已验证加解密、随机密文和删 key 后拒绝旧密文。
- Ktor + OkHttp Engine 使用离线拦截器验证 Unicode Query、Header、原始 Body 字节、响应 Header 和异常映射；当前 `:kugou-api` 默认离线测试与显式真实集成测试均通过。
- `:core:model` 已增加稳定的 `Song`、`SearchPage`、`SearchResult` 和 `SearchError`；网络字段、服务错误码和 Android URI 均未泄漏到领域层。
- `KugouSongSearchDecoder` 已兼容 ID、时长和总数的字符串/数字漂移，跳过单个坏条目，但关键列表缺失或整页不可用时返回协议错误，不制造空歌曲。
- `KugouSearchRepository` 已串联匿名会话、真实 Transport、协议解码、分页和类型化错误映射；搜索 Host 不接收 MID、dfid、Cookie 或签名，App Manifest 仅新增 `INTERNET` 权限。
- 2026-08-05 显式运行 `LiveKugouSearchRepositoryTest`，真实完成“设备注册 → 匿名搜索 → DTO 解码 → Domain 映射”，服务返回非空领域歌曲列表；记录未保存关键词结果、响应正文、dfid 或 Cookie。
- 独立 Search Route 已从首页进入且不占底部 Tab，覆盖未搜索、加载、空结果、错误、内容和分页状态；新搜索取消旧任务并以 generation 防止旧响应覆盖新结果。
- 搜索页面已建立标准与 `1.5×` 字体截图基准，ViewModel 覆盖空关键词、成功状态和旧请求隔离。2026-08-05 在 Huawei API 29 真机实际搜索并显示标题、歌手、时长与封面语义。
- `privilege_lite` 的基础资源与 `relate_goods`、`song_url` 的主/备 URL、时长、格式及无版权/VIP 分支已按 PC API、PC 播放队列和 Mobile `song-url.ts` 实现，未通过真实响应猜测业务格式。
- 匿名播放沿用 Mobile 已验证路径，使用 `song_url(free_part=1)`；`privilege_lite` 作为登录后音质候选能力保留，不让匿名播放多依赖一次账号相关请求。返回的 HTTP CDN 候选只升级为 HTTPS，不开放明文流量，短期地址不写入 Room。
- 2026-08-05 真实服务验收通过 `privilege_lite` 结构解码和安全播放地址解析；Huawei API 29 真机点击搜索结果后 Media3 进入 Playing，播放位置持续增长、缓冲与系统媒体元数据正常。
- 已保留上游 MIT NOTICE 与完整许可证。
- 播放源不再用 `null` 抹平失败：无版权、VIP、离线、超时、连接、风控、登录要求、服务、协议、会话初始化和无效来源均映射为稳定的 `PlaybackSourceError`；首次 `playNow` 在入队前完成解析，失败会同步返回类型化拒绝。
- Media3 遇到酷狗在线源的 HTTP/网络 IO 错误时，会对当前歌曲显式重新解析地址一次；同一错误链再次失败不会继续刷新，本地源和解码错误不会触发网络解析。队列中已存在的同歌曲也会替换为新解析 URI，短期地址仍不写入 Room。
- `:core:designsystem` 已按确认稿 `12-feedback-components-v2.png` 与 `UI_COMPONENTS.md` 实现 `MoeSnackbar` 的成功、信息、错误和警告状态；旧 `18-mobile-states-overlays.png` 已暂停实现基线资格，但不撤销该行为证据。App 根层在 MiniPlayer 上方显示单条播放反馈，可恢复错误提供一个“重试”动作，4 秒后按事件 id 安全收起。
- `KugouPlaybackSourceResolverTest`、`PlaybackAddressRefreshPolicyTest` 和 `AppPlaybackViewModelTest` 覆盖类型映射、一次刷新、非网络错误豁免、重试与旧反馈隔离；`MoeSnackbarScreenshotTest` 已生成并通过 390dp 浅色截图基准。
- 2026-08-05 在 Huawei ELE-AL00 / Android 10（API 29）安装最新 Debug 包，真实搜索 `Linkin Park` 后播放 `Numb`：MiniPlayer 显示“暂停”，系统媒体会话为 Playing、`error=null`，位置从约 2.9 秒增长到约 12.0 秒，证明预解析未破坏在线播放闭环。
- 同一设备完整执行 `MainActivityTest`，6/6 通过且无跳过，覆盖根层反馈接入后的首页、搜索返回栈、我的、本地音乐、登录、密码登录和 QR 登录导航回归。

当前剩余风险：真实 CDN 地址过期尚未在可控时间内自然复现，一次刷新目前由纯策略测试和 Media3 集成编译覆盖；无版权、VIP 与风控的线上样本也未稳定取得，因此对应 Snackbar 仍以协议 fixture、ViewModel 单测和截图基准为验收依据。API 26、33、36、后台播放和进程重启矩阵继续保留在阶段完成门禁中。

测试矩阵补充：

- Crypto：固定 Unicode/空值/字节 Body、AES PKCS7、RSA 解密行为、MID 大整数和参数排序。
- Request：固定时间、身份、Cookie、Body 与 Header 的 Node/Kotlin快照；禁止真实密钥或账号数据。
- Session：首次生成、重启恢复、并发初始化、注册缺 dfid、损坏密文、Keystore key 失效和清除。
- Transport：超时、断网、非 JSON、Set-Cookie、多 Host、风控 Header、4xx/5xx 与脱敏诊断。
- Search：空结果、分页、字段缺失/漂移、取消、旧请求隔离和分页重试。
- Playback：无版权、VIP、URL 缺失、备选 URL、一次刷新、Media3 接管与本地队列保留。
- Compose：Idle、输入、加载、内容、空、错误、分页、1.5×/2.0× 字体及三种主题。
- 真机：API 26、29、33、36 的匿名初始化、搜索、在线播放、后台播放和重启后会话恢复。

## 完成标准

- 全新安装可生成并安全保存匿名设备身份。
- 搜索结果可以解析可播放地址并通过 Media3 播放。
- 协议日志脱敏，固定对照测试稳定通过。
- 阶段审计、协议来源声明、真实接口手工结果和未验证风险均写入仓库。
