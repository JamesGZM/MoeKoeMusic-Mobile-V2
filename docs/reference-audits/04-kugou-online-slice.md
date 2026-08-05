# 阶段 4：酷狗在线闭环参考审计

状态：Accepted。审计日期：2026-08-05。

## 产品能力与非目标

阶段 4 只建立“匿名设备初始化 → 搜索歌曲 → 解析播放地址 → 交给 Media3 播放”的最小闭环。入口是首页搜索框进入的独立搜索子页面；结果点击后立即播放，并保留现有本地队列。

本阶段不实现登录、首页推荐、收藏、歌词、音质选择、云盘、下载、缓存和完整播放器 UI。真实接口验证是人工集成检查，不成为普通单元测试依赖。

## Android 官方约束

- 网络只声明 `INTERNET`，不新增设备、电话、广告标识或存储权限。
- 阶段 4 的四个目标 Endpoint 均使用 HTTPS；默认拒绝明文 HTTP，不因旧 PC/Mobile 登录接口放开全局 cleartext。
- Android Keystore 只保存本机不可导出的 AES-GCM 密钥；会话密文由 `:data` 持久化。`:kugou-api` 通过端口读写会话，不依赖 Android Framework。
- 网络请求必须有连接、读取和整体超时；只有匿名注册、搜索等幂等读取可以有限重试，歌曲地址失效只允许显式重新解析一次。
- 日志只记录 request id、Endpoint、耗时、结果分类和脱敏标识摘要，不记录完整 Cookie、token、dfid、mid、签名、搜索词或播放 URL。

官方依据：

- [Network security configuration](https://developer.android.com/privacy-and-security/security-config)
- [Cleartext communications risks](https://developer.android.com/privacy-and-security/risks/cleartext-communications)
- [Android Keystore](https://developer.android.com/privacy-and-security/keystore)

## KuGouMusicApi 固定协议基准

- 仓库：`https://github.com/MakcRe/KuGouMusicApi`
- 本地路径：`../MoeKoeMusic/api`
- 固定提交：`6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb`
- 许可证：MIT，可迁移与修改；必须保留 Copyright 与 MIT 声明。

审计文件：

- `util/request.js`：默认参数、Header、Cookie 合并、签名选择、动态 Host 与错误包装。
- `util/helper.js`：Android、Register、Web 签名和 `signKey`。
- `util/crypto.js`：MD5、SHA-1、AES-CBC、RSA 与 playlist 加解密。
- `util/util.js`：GUID、MID、Cookie 解析和字节处理。
- `util/config.json`：只审计酷狗 app/client 版本；禁止复制无关平台凭据。
- `module/register_dev.js`：匿名设备注册的加密 Body、Host 与 dfid 提取。
- `module/search.js`：歌曲搜索参数与 `x-router`。
- `module/privilege_lite.js`：播放前权限和质量资源映射。
- `module/song_url.js`：播放地址参数、短期 URL 与无版权/VIP分支。

采用：固定输入的字节级算法、参数排序、请求快照、Endpoint Host/Header、Cookie 合并和动态响应容错。

拒绝：Express 路由、Node/Buffer Runtime、代理环境变量、服务端 IP 透传、WebGL 模拟、SSA 行为模拟、全量模块生成、无关配置字段，以及把服务端正文或异常对象直接暴露为 UI 错误。

## MoeKoeMusic Mobile

- 仓库：`https://github.com/MoeKoeMusic/MoeKoeMusic-Mobile`
- 本地路径：`../MoeKoeMusic-Mobile`
- 固定提交：`ab71195d4cf3297332490fd37704d1ae8973d4c5`
- API submodule 现场提交：`283f1e97b110726b208a64b486a657c0fc0a6126`
- 许可证：GPL-2.0；本项目同为 GPL-2.0-only，可参考自身项目实现，但仍以固定 API 基准决定协议。

审计文件：

- `src/lib/kugou-api/index.ts`：运行时只初始化一次、注册后确认 dfid、响应 Cookie 回写。
- `src/lib/kugou-api/device.ts`：GUID 持久化、MID 派生和移动设备参数采集。
- `src/lib/kugou-api/session.ts`、`storage.ts`：会话 hydration、合并和安全存储。
- `src/lib/kugou-api/use-axios.ts`：二进制响应归一化与请求边界。
- `src/features/player/song-url.ts`：播放地址候选、时长和无版权/VIP 映射。

采用：并发初始化合并为单一任务、设备身份先持久化再注册、Cookie 即时合并、播放地址不持久化、响应字段兼容只留在协议层。

拒绝：Expo SecureStore、Hermes polyfill、动态模块表、开发日志输出响应正文、全局 cleartext 放行，以及一次初始化失败后无分类地重复注册。

## MoeKoeMusic PC

- 仓库：`https://github.com/MoeKoeMusic/MoeKoeMusic`
- 本地路径：`../MoeKoeMusic`
- 固定提交：`52c9833afe2e7fedcba8d5b23ff8d1f9731af73a`
- API submodule：上述权威 `6efe84e`。
- 许可证：GPL-2.0。

审计文件：

- `src/views/Search.vue`：搜索分页、分类和空结果产品语义。
- `src/components/search/SongSearchList.vue`：歌曲、歌手、专辑、时长与质量信息层级。
- `src/utils/request.js`：身份透传、风控入口和 PC 错误路径。
- `src/components/player/AudioController.js`：短期播放地址进入队列后的刷新行为。

采用：第一版搜索结果字段、分页语义、搜索到播放的用户路径和无版权反馈。

拒绝：依赖本地 Node 服务的 HTTP 包装、Vue 状态结构、登录前置路由、UI 直接理解协议字段、Console 输出响应数据，以及把 PC 的全部搜索分类一次性带入阶段 4。

## 独立实现结论

三套来源共同确认了协议、移动初始化和 PC 产品语义，但没有适合直接复制到 Android 的 Kotlin 分层实现。本项目独立建立：

```text
SearchRoute
    ↓
SearchRepository (:data)
    ↓
KugouClient (:kugou-api)
    ├── DeviceIdentity + SessionStore port
    ├── Signer / Crypto / RequestFactory
    └── Ktor Client + OkHttp Engine + DTO

Kugou PlaybackSource
    ↓
KugouSourceResolver (:data)
    ↓
PlaybackController (:playback)
```

协议常量与算法只来自 MIT 固定基准；PC/Mobile 用于验证调用次序和产品语义。无 Node、WebView、JavaScript Runtime、遥测或额外权限进入 Android App。

## 真实服务兼容性补审（2026-08-05）

首次真实测试证明固定源码与离线向量不足以代表服务当前行为：

- Kotlin `register_dev` 真实调用成功，响应可以解密并取得非空 dfid。
- 随后调用固定版 `/v3/search/song` 返回类型化协议错误 `error_code=152`；不打印服务端正文。
- `https://complexsearch.kugou.com/v6/search/complex` 返回 HTTP 200、`text/plain` 且内容不是 JSON，不能作为 Android JSON 协议使用。
- `https://songsearch.kugou.com/song_search_v2` 使用最小匿名参数返回 `status=1`、`error_code=0` 和非空歌曲列表；Kotlin 的“注册 → 匿名搜索”真实集成测试随后通过。

额外对照项目：

- SPlayer-Next `75b4301ce12ffb62a556753b32a99642ae0cd831`，`electron/main/apis/kugou/modules/search.ts` 与 `core/config.ts`，AGPL-3.0。它将 `mobilecdn` 作为主路径、HTTPS `song_search_v2` 作为回退；本项目只采用“匿名搜索需要独立公开路径”的判断，不复制实现。其 `mobilecdn` 地址为 HTTP，且 HTTPS 证书主机名校验失败，因此不符合本项目全 HTTPS 边界。
- UnblockNeteaseMusic/server `39e21bfb4b7581f39785b190aeced201d23f0d41`，`src/provider/kugou.js`，LGPL-3.0。它同样使用 `mobilecdn` 匿名搜索，证明该路径有长期实践；本项目不采用其 HTTP Endpoint、播放 URL 算法或代码。
- kugou-music-api Go `950cbf0b6c60f980d316ab7a6568ef023820e89e`，`sdk/generated_api_specs.go` 与 `examples/simple/main.go`，MIT。2026-08-05 本地运行示例同样得到 `error_code=152` 和空列表，交叉证明固定 Android 搜索接口的当前限制不是 Kotlin 特有问题；该项目仍把 HTTP 200 当成功，本项目拒绝这种判定。

结论：固定 `6efe84e` 仍是加密、签名、注册和登录后 Android 协议的基准，但 Endpoint 是否“当前可用”必须由真实服务测试决定。匿名搜索采用独立实现的最小 HTTPS WebFilter 请求；不引入 AGPL/LGPL 代码，也不放开明文流量。
