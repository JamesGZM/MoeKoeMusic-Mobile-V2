---
name: moekoe-kugou-protocol
description: Migrate, implement, debug, or review KuGou protocol behavior in MoeKoeMusic. Use for kugou-api/, endpoints, request signing, encryption, sessions, cookies, authentication, profile, lyrics, dynamic JSON, network DTOs, error mapping, retries, or controlled live-service compatibility checks.
---

# MoeKoe 酷狗协议迁移

## 证据顺序

1. 读取 `docs/API_MIGRATION.md`、`docs/decisions/0001-native-direct-api.md`、`docs/ARCHITECTURE.md` 的 API/Data 边界及 `docs/TESTING_STRATEGY.md` 的相关测试矩阵。
2. 按领域读取相关审计：在线闭环 `04`、HTTP `07`、登录会话 `09`、资料 `10`、歌词 `12`。
3. 按具体审计指向的位置读取固定 KuGouMusicApi 的 Endpoint、调用层和消费层源码；不要硬编码单一参考仓库路径，也不要根据记忆或真实服务响应猜测字段、签名、常量、错误码或业务分支。
4. 只有固定源码缺失、相互冲突或有服务漂移证据时才设计最小探针，并记录原因、范围和脱敏结论。

## 实现边界

- `:kugou-api` 只负责协议、会话、签名、加密、传输和网络 DTO，不依赖 Compose 或 Media3。
- 动态 JSON 容错限制在网络层；对上层返回稳定领域类型和类型化错误，不以字符串判断业务状态。
- 请求设置超时；只对幂等操作做有限重试，登录和写操作不得无条件重放。
- 不记录或提交 token、Cookie、手机号、设备标识、播放签名、真实响应正文和服务端验证载荷。
- 保留 KuGouMusicApi MIT 来源与归属；协议或依赖边界变化先调用 `$moekoe-spec-audit`。

## 验证

- 签名、加密和参数迁移必须有固定虚构向量的 Node/Kotlin 对照测试。
- Fake Transport 证明客户端编排；真实服务仅在固定测试通过后做受控兼容验收，不能替代源码审计。
- UI、播放或数据层消费变化同时调用相应领域 skill；完成前调用 `$moekoe-validate-change`。
