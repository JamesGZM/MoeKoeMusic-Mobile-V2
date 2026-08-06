# AGENTS.md

本文件只保留 MoeKoeMusic Mobile V2 的常驻硬门禁与 skill 路由。开始工作前阅读 `README.md`，再按任务调用下列仓库 skill；多个领域重叠时调用并集。

## 常驻硬门禁

- 优先小而可审查的改动；修改前明确目标文件、边界和验证方式，不夹带用户或其他任务的变更。
- 不虚构酷狗接口、字段、签名、常量或错误码；不确定时读取固定版本 KuGouMusicApi 的 Endpoint、调用层和消费层源码。
- 保持 Android 原生实现；禁止引入 Node、内嵌 HTTP 服务、JavaScript Runtime、额外网络请求、分析、遥测或广告，除非需求明确授权。
- 不提交 token、Cookie、手机号、设备标识、私钥、`.env`、真实抓包或完整服务端载荷；日志、测试向量和报告必须脱敏。
- Compose UI 不直连网络、数据库、DataStore、ExoPlayer 或 Service；ViewModel 只暴露不可变 `StateFlow` 与明确事件。
- `:playback` 独占 ExoPlayer；`MediaLibraryService` 不承载产品业务；`:kugou-api` 不依赖 Compose 或 Media3。
- DTO、Entity、Domain Model 与 UI Model 不混用；避免 `!!`、`GlobalScope`、无生命周期协程和字符串业务判断。
- 行为变更必须测试。开发验收只使用用户指定且已连接的真机，不创建或启动模拟器。
- 每个完成且通过相应验证的可审查切片立即形成原子提交；提交只包含该切片。

## Skill 路由

- 重大功能、权限、存储、后台任务、新依赖、公共接口或核心系统：先调用 `$moekoe-spec-audit`。
- 页面、导航、组件、Dialog/Sheet、交互状态、设计图、原型或 Compose UI：调用 `$moekoe-ui-compose`。
- 模块边界、依赖方向、Navigation、ViewModel、Repository、Room/DataStore 或 Hilt：调用 `$moekoe-architecture`。
- Endpoint、签名、加密、会话、登录、资料、歌词、动态 JSON 或真实酷狗服务：调用 `$moekoe-kugou-protocol`。
- Media3、ExoPlayer、Service、队列、播放模式、恢复、通知或播放器进度：调用 `$moekoe-playback`。
- SAF、URI、外部 Intent、本地音乐复制、去重、回滚或导入后播放：调用 `$moekoe-local-import`。
- 任何代码切片完成前必须调用 `$moekoe-validate-change`；纯分析或审查任务在交付前也用它核对证据与未执行项。

## Sub-agent 协作

- 对可独立开展的源码审计、代码库映射、测试矩阵、文档一致性和只读复核，优先委派 sub-agent；父 agent 负责决策、整合、修改、冲突处理和提交。
- 并行写入仅限父 agent 已划分互不重叠的文件所有权；否则 sub-agent 保持只读。
- Sub-agent 返回结论与文件证据，不把冗长日志注入父线程；涉及用户选择、敏感操作或外部状态变更时交回父 agent。

## 文档同步

- 模块边界更新 `docs/ARCHITECTURE.md`；协议迁移更新 `docs/API_MIGRATION.md` 和相关 ADR；UI 决策更新设计索引、阶段计划和 Design System；外部参考更新 `docs/REFERENCES.md`。
