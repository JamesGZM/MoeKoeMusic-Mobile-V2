# ADR-0001：原生直连酷狗接口

- 状态：Accepted
- 日期：2026-08-04

## 背景

PC 端通过本地 Node/Express 服务调用 KuGouMusicApi。React Native Mobile 没有启动 Express，而是把 API 模块作为 JavaScript 函数打包，并注入移动端 HTTP 实现。

V2 的目标是 Android 原生 Compose，并希望不依赖自建服务器。

## 决定

以 `KuGouMusicApi@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb` 为基准，将 App 所需协议逐步迁移到 Kotlin `:kugou-api` 模块。

正式 App 不包含：

- Node Runtime。
- Express 或 localhost API 服务。
- WebView API 桥。
- JavaScript/Hermes Runtime。
- 自建远程代理服务依赖。

## 理由

- 获得完整的 Kotlin 类型、安全存储和调试体验。
- 避免 Node ABI、包体和后台生命周期复杂度。
- 保持无需服务器即可运行。
- 公共签名层迁移完成后，大部分 Endpoint 只是参数和 DTO 工作。

## 代价

- 初期需要严谨迁移加密、签名和会话处理。
- 上游协议变化需要人工比较并同步。
- 必须维护 Node/Kotlin 对照测试。

## 后果

- KuGouMusicApi submodule 只作为参考与测试基准，不参与 Android 构建。
- API 模块保持 UI 和播放器无关。
- 优先完成匿名搜索播放闭环，再扩大接口范围。

