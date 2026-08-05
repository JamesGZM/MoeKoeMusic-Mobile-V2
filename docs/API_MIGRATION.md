# KuGouMusicApi Kotlin 迁移

## 基准与目标

迁移基准固定为：

```text
Repository: https://github.com/MakcRe/KuGouMusicApi
Commit:     6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb
License:    MIT
```

目标是将 App 实际需要的酷狗协议迁移为 Kotlin，不在 Android 中运行 Node、Express、本地 HTTP 服务或 JavaScript Runtime。

阶段 4 的固定源码文件、PC/Mobile 提交、采用/拒绝点、Android 安全边界和数据流见 [`reference-audits/04-kugou-online-slice.md`](reference-audits/04-kugou-online-slice.md)。迁移不得机械复制 `util/config.json`；只提取目标酷狗 Endpoint 实际使用且经固定测试证明必要的协议常量，拒绝无关平台凭据。

当前已完成第一批纯 JVM 对照：MD5、SHA-1、MID、Android/Register/Web 签名、带字节 Body 的签名、`signKey`、playlist AES 与 RSA PKCS#1 行为。固定输入全部为虚构数据。

统一请求层也已建立：RequestFactory 在固定时间和虚构身份下生成可快照的请求，Transport 是 suspend 端口，Cookie 与响应错误使用稳定类型。请求对象的诊断字符串只暴露字段名和字节数，不暴露参数、Header、Cookie 或 Body 值。下一批实现 OkHttp Transport 和首批 Endpoint。

## 参考优先级

1. 固定提交的 KuGouMusicApi：协议参数、签名和加密的最终依据。
2. React Native Mobile：设备注册、会话恢复、接口调用顺序和移动端响应兼容参考。
3. PC 端：完整接口覆盖、音质回退、风控和高级歌词规则参考。

如果三者行为冲突，先编写可复现测试，再决定兼容策略，不能凭印象选择。

## 建议目录

```text
kugou-api/src/main/kotlin/.../
├── config/
│   └── KugouPlatformConfig.kt
├── crypto/
│   └── KugouCrypto.kt
├── signing/
│   └── KugouRequestSigner.kt
├── session/
│   ├── KugouDeviceIdentity.kt
│   ├── KugouSession.kt
│   └── KugouSessionStore.kt
├── transport/
│   ├── KugouRequest.kt
│   ├── KugouResponse.kt
│   └── KugouRequestExecutor.kt
├── endpoint/
└── dto/
```

## Node 到 Kotlin 的映射

| Node 实现 | Kotlin 实现 |
| --- | --- |
| Axios | OkHttp |
| Buffer | ByteArray |
| CryptoJS | Java Cryptography Architecture |
| node-forge RSA | JCA，必要时 Bouncy Castle |
| process.env platform | KugouPlatformConfig |
| Cookie 对象 | KugouSession + OkHttp Cookie 处理 |
| Express 路由 | 类型化 Endpoint 函数 |
| localStorage/SecureStore | App 提供的加密 SessionStore |

## 公共迁移顺序

1. 平台常量与 `lite` 配置。
2. 随机值、Cookie、MID 和通用编码工具。
3. MD5、SHA1、AES、RSA 与歌词解码。
4. Android、register 和 web 三种签名。
5. 统一请求参数、Header、动态 Host 与响应处理。
6. 设备注册和会话持久化。
7. 具体 Endpoint。

## 首批 Endpoint

### 技术验证闭环

- `register_dev`
- `search`
- `privilege_lite`
- `song_url`
- `search_lyric`
- `lyric`

### 首页与详情

- `yueku_banner`
- `everyday_recommend`
- `top_playlist`
- `playlist_track_all`
- `rank_list`
- `rank_audio`
- `album_songs`
- `artist_detail`
- `artist_audios`

### 登录与音乐库

- `captcha_sent`
- `login_cellphone`
- `login`
- `login_qr_key`
- `login_qr_create`
- `login_qr_check`
- `user_detail`
- `user_vip_detail`
- `youth_vip`（兼容 PC 端签到；上游已标记为可能移除）
- `youth_day_vip`
- `youth_day_vip_upgrade`
- `youth_month_vip_record`
- `youth_union_vip`
- `user_playlist`
- `user_follow`
- `playlist_add`
- `playlist_tracks_add`
- `playlist_tracks_del`

只迁移 App 已进入开发范围的接口，不一次性机械翻译全部模块。

签到与 VIP 领取均要求登录。UI 不应把当前 VIP 徽标当作领取入口；领取状态、当日已领取、接口不可用和账号风控必须映射为明确的领域状态。其中旧 `youth_vip` 只作 PC 功能兼容，不能成为唯一领取路径。

音乐库数据沿用 PC 端语义进行领域映射：`user_playlist` 区分创建歌单、收藏歌单与收藏专辑，`user_follow` 区分关注歌手与关注好友。网络字段判断只能停留在 `:kugou-api` 或 Repository 映射层，Compose UI 只消费稳定的分类模型。

## 请求执行规范

统一执行器负责：

- 注入 `dfid`、`mid`、`uuid`、`appid`、`clientver` 和 `clienttime`。
- 登录后注入 `token` 与 `userid`。
- 根据 Endpoint 生成正确签名与 `key`。
- 处理 `x-router`、动态 base URL、User-Agent 和二进制响应。
- 合并响应 Cookie。
- 识别 SSA 风控信息并返回类型化错误。
- 记录脱敏诊断信息。

Endpoint 不重复实现上述公共逻辑。

## 类型与兼容策略

- 网络 DTO 使用 Kotlin Serialization，并允许明确记录的字段类型不一致。
- 动态容错停留在 DTO 层，映射后领域模型保持稳定。
- ID 在协议层可使用兼容序列化器，领域层统一为 String。
- 未知字段默认忽略；关键字段缺失返回 ProtocolError，不制造空业务对象。
- 服务端错误正文保留在脱敏诊断结构中，不直接作为 UI 文案。

## 对照测试

Node 与 Kotlin 使用相同的固定输入：

- 固定时间戳。
- 固定 GUID、MID、DFID、MAC 和设备代码。
- 固定虚构 token 与 userid。
- 固定 Query、Header 和 Body。

至少覆盖：

- MD5、SHA1。
- AES CBC 加密和解密。
- RSA 加密行为。
- `calculateMid`。
- `signKey` 与三种 signature。
- playlist AES。
- KRC 解码。
- 完整请求 Query/Header 快照。

含随机填充的 RSA 不能简单比较两次密文，应验证可解密结果或服务端接受行为。

## 上游升级

默认不自动跟随 KuGouMusicApi 最新提交。升级时：

1. 记录当前与目标提交。
2. 比较 `util/` 和已迁移的 `module/`。
3. 优先检查公共签名、平台常量和请求层变化。
4. 只迁移 App 使用接口的相关变化。
5. 更新 Node/Kotlin 对照测试。
6. 在 ADR 或变更日志记录行为影响。

## 许可证与声明

- 保留 KuGouMusicApi 原 MIT License 与作者声明。
- 在模块 README 或 NOTICE 中标注来源仓库与基准提交。
- 从 GPL 项目移植具体实现时遵守对应 GPL 条款。
- 接口研究仅用于学习与技术研究，不存储或分发音频资源。
