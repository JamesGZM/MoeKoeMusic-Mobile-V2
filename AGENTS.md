# AGENTS.md

本文件约束 MoeKoeMusic Mobile V2 中的人类贡献者和编码代理。开始修改前先阅读 `README.md` 与任务相关的 `docs/` 文档。

## 工作原则

- 优先小而可审查的改动，不做未经要求的大规模重构。
- 修改前明确目标文件、边界和验证方式。
- 不虚构酷狗接口、字段、签名常量或错误码；不确定时回到固定版本的 KuGouMusicApi 查证。
- 保持 Android 原生实现，不引入 Node、内嵌 HTTP 服务、WebView API 桥或 JavaScript Runtime。
- 新依赖必须说明用途、维护状态、许可证和无法使用现有依赖完成的原因。
- 不添加分析、遥测、广告或额外网络请求，除非需求明确要求。

## 架构边界

- Compose UI 不直接访问网络、数据库、DataStore、ExoPlayer 或 Android Service。
- ViewModel 只暴露不可变 `StateFlow` UI 状态和明确的用户事件方法。
- `:playback` 是唯一能够创建和持有 ExoPlayer 的模块。
- `MediaLibraryService` 不承担搜索、登录、收藏、歌词解析等产品业务。
- `:kugou-api` 只负责酷狗协议、会话、签名、加密、传输和网络 DTO，不依赖 Compose 或 Media3。
- 网络 DTO、数据库 Entity、领域 Model 和 UI Model 不混用。
- UseCase 仅用于跨 Repository 或具有明确业务规则的操作，不为每个简单读取机械创建 UseCase。

## Compose 规范

- Route 负责获取 ViewModel 和收集状态，Screen 尽量保持无状态。
- 可复用组件不得自行导航或读取全局单例。
- Composable 参数优先传值和事件 lambda，不传整个 ViewModel。
- 列表项必须提供稳定 key；高频进度状态不得导致整个页面重组。
- 所有可交互图标提供无障碍语义或 `contentDescription`。
- 新页面同时考虑加载、空数据、错误、离线和正常状态。
- UI token 统一来自 `:core:designsystem`，禁止在业务页面散落品牌色和重复尺寸常量。

## Kotlin 规范

- 使用 Kotlin 官方代码风格和显式空值处理。
- 避免 `!!`、全局可变状态和不受控的 `GlobalScope`。
- 协程作用域必须有明确生命周期；IO 与计算调度器可注入以便测试。
- 对可恢复错误使用明确的领域错误类型，不使用字符串判断业务状态。
- 公共 API 优先返回稳定领域类型；动态 JSON 容错限制在网络层。
- 注释解释意图和协议怪异点，不复述代码表面行为。

## 安全与协议

- 不提交账号 token、Cookie、设备标识、私钥、`.env` 或真实抓包数据。
- 测试向量使用固定的虚构身份数据，并在文档中说明来源。
- 登录凭据和敏感会话使用 Android Keystore 支持的加密存储。
- 日志必须脱敏，不输出完整 token、手机号、Cookie、播放签名或服务端验证载荷。
- 保留 KuGouMusicApi MIT 许可证和迁移来源说明。

## 测试与验证

- 加密、签名和请求参数迁移必须有 Node 与 Kotlin 对照测试。
- Repository、队列状态机和播放模式变化必须有单元测试。
- UI 行为变化至少验证对应 Preview、截图测试或设备测试之一。
- 先运行最快的相关检查，再运行受影响模块的完整测试。
- 提交结果时说明执行过的命令、通过情况和未执行原因。

## 文档同步

- 改变模块边界时更新 `docs/ARCHITECTURE.md`。
- 改变接口迁移策略时更新 `docs/API_MIGRATION.md` 和相关 ADR。
- 确定 UI 设计决策时更新 `docs/UI_DESIGN_BRIEF.md`。
- 增加重要外部参考时更新 `docs/REFERENCES.md`，记录借鉴点和不采用点。
