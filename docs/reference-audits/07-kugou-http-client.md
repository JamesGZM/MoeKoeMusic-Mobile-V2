# 酷狗 HTTP 客户端选型审计

状态：Accepted。审计日期：2026-08-05。

## 协议需求

酷狗不是统一 REST API。客户端必须支持动态 HTTPS Host、严格参数与 Body 字节签名、自定义 Cookie 合并、JSON/二进制响应、非标准 Content-Type、接口级重试规则和动态 JSON 容错。任何 HTTP 库都不能替代签名、加密、设备身份、会话与协议错误模型。

## 候选方案

### OkHttp 5 + 手写传输

- 许可证：Apache-2.0。
- 优点：依赖最少、请求字节控制直接、已有测试覆盖。
- 缺点：现实现重复编写 callback 转协程、URL/Body 装配和响应读取；所有 JSON 先暴露为 `JsonElement`，扩展接口时 Decoder 样板持续增长。
- 结论：保留为底层 Engine，不继续维护手写通用传输层。

### Retrofit + OkHttp

- 许可证：Apache-2.0。
- 优点：稳定 REST API 的声明式接口和类型化 DTO 成熟，支持 kotlinx.serialization Converter。
- 缺点：酷狗请求会大量退化为 `@Url + @QueryMap + @HeaderMap + RequestBody/ResponseBody`；签名必须在最终参数和 Body 确定后执行，多 Host、动态结构和二进制协议削弱 Retrofit 的主要收益。
- 结论：不采用为酷狗统一客户端；未来接入独立、稳定的标准 REST 服务时可重新评估。

### Ktor Client + OkHttp Engine

- 许可证：Apache-2.0。
- 优点：请求 API 原生挂起；动态 URL、Header、Cookie、ByteArray 和 Content-Type 建模自然；ContentNegotiation 可直接解析稳定 DTO；仍复用 OkHttp 连接池、TLS 和拦截器。
- 成本：增加 Ktor Client/Core、OkHttp Engine、ContentNegotiation 与 kotlinx-json 构件；团队需约束插件边界，避免把协议重试错误地全局化。
- 结论：采用。Ktor 负责通用 HTTP 生命周期，酷狗协议层继续负责签名、会话、错误和接口级重试。

官方依据：

- [Ktor Client requests](https://ktor.io/docs/client-requests.html)
- [Ktor ContentNegotiation](https://ktor.io/docs/client-serialization.html)
- [Ktor OkHttp engine](https://ktor.io/docs/http-client-engines.html)
- [Ktor request retry](https://ktor.io/docs/client-request-retry.html)
- [OkHttp coroutine executeAsync](https://square.github.io/okhttp/5.x/okhttp-coroutines/okhttp3.coroutines/execute-async.html)
- [Retrofit](https://github.com/square/retrofit)

## Kreate

- 固定提交：`f02577e862318df26b13abb02d14d8d824a1b947`
- 文件：`extensions/kugou/src/main/kotlin/it/fast4x/kugou/KuGou.kt`
- 许可证：GPL-3.0；仅学习模式。

采用：`HttpClient(OkHttp)`、ContentNegotiation、kotlinx.serialization、ContentEncoding 和类型化 `body<T>()`。

拒绝：其酷狗能力只覆盖较简单的搜索/歌词请求，不能据此删减本项目的签名、注册、会话和播放地址协议。

## Metrolist

- 固定提交：`289ed45d0a429e6e4f19a2a07543b243a0ab2e8b`
- 文件：`innertube/src/main/kotlin/com/metrolist/innertube/InnerTube.kt`
- 许可证：GPL-3.0；仅学习模式。

采用：Ktor 与 OkHttp Engine 分层、统一超时和 ContentNegotiation、动态 Header 与签名仍留在业务协议函数。

拒绝：全局 `expectSuccess`、宽泛重试包装、响应正文日志及与本项目无关的代理和缓存配置。

## 本项目边界

```text
KugouEndpoint / KugouRequestSpec
        ↓
KugouRequestFactory（默认参数、签名、Cookie、Body 字节）
        ↓
KtorKugouTransport（Ktor Client + OkHttp Engine）
        ↓
HTTP/风控公共解码
        ↓
稳定响应 DTO 或 Endpoint 专属动态 Decoder
```

- 幂等重试继续由 `KugouRetryMode` 显式决定，不安装无条件全局重试。
- 登录、验证和写操作禁止自动重放。
- 稳定结构使用 `@Serializable` DTO；确有多形态字段的 Endpoint 才允许在其 Decoder 内使用 `JsonElement`。
- `JsonElement` 不得成为 Repository 的公共返回类型。
- Ktor Client 为可复用单例并由 Hilt 管理生命周期。

## 验收矩阵

- 既有请求快照、签名和 Node/Kotlin 对照测试保持不变。
- GET、JSON POST、二进制注册响应、Set-Cookie、动态 Host 和取消请求测试通过。
- Timeout、Offline、Connection 与 HTTP/协议错误映射不退化。
- 只有 `IdempotentRead` 对 Timeout 和 5xx 有限重试。
- 真实匿名注册、搜索和播放地址测试继续作为受控集成验证。

## 实施验证

2026-08-05 迁移后完成以下验证：

- 离线 Interceptor 覆盖 Unicode Query、Header、原始二进制 Body、大小写不敏感的响应 Header、异常映射和多段 `encodedPath`。
- 首次真实验证发现把完整注册地址误用为单一 Path Segment；补充复现断言后改用完整编码路径段列表，证明真实服务测试不能由单段 Fake 替代。
- 真实匿名“设备注册 → 搜索 → 领域映射”通过。
- 真实匿名“设备注册 → 权限形态 → HTTPS 播放地址”串行通过。
- 真实测试必须 `--no-parallel` 串行执行，避免并发注册触发上游限流；这不改变生产客户端的单飞初始化语义。
