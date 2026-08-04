# 工程规范

## 技术基线

- Application ID：`cn.james.music`。
- minSdk：API 26；compileSdk/targetSdk：API 36。
- Kotlin。
- Jetpack Compose + Material 3。
- Coroutines + Flow。
- Navigation Compose。
- Hilt。
- OkHttp + Kotlin Serialization。
- Room + DataStore。
- Media3 ExoPlayer + MediaLibraryService。
- Coil。
- Gradle Kotlin DSL + Version Catalog。

具体版本在创建工程时以稳定版和兼容矩阵为准，不在设计文档中提前写死。

## 构建约定

- 所有依赖版本集中在 `gradle/libs.versions.toml`。
- 公共 Gradle 配置使用受控 convention plugin，避免复制粘贴。
- Debug 与 Release 使用不同日志和诊断策略，但业务行为保持一致。
- Release 禁止输出敏感网络日志。
- CI 首先执行格式、静态检查和受影响模块测试，再执行完整构建。

## 包与命名

- 代码命名空间以 `cn.james.music` 为根；Debug 变体可使用 `.debug` 后缀。
- 类型使用业务语义命名，避免 `Manager`、`Helper`、`Utils` 泛化命名。
- 网络类型以 `Dto` 结尾，数据库类型以 `Entity` 结尾，UI 专用类型以 `UiModel` 结尾。
- Repository 接口按领域命名，例如 `PlaylistRepository`，不按页面命名。
- Composable 页面使用 `XxxRoute` 与 `XxxScreen` 区分容器和纯 UI。

## 状态与并发

- ViewModel 内部使用 `MutableStateFlow`，外部只暴露 `StateFlow`。
- 使用 `stateIn` 时明确 `SharingStarted` 和初始值。
- 搜索、切歌等可过期请求必须使用取消或序列号防止旧结果覆盖新状态。
- 不在 Composable 中直接启动不受生命周期管理的协程。
- 播放进度、歌词行和常规页面状态拆分更新频率。

## 网络与缓存

- 所有请求设定超时和明确错误映射。
- 重试只用于幂等操作，并限制次数和退避。
- 登录、歌单编辑等写操作不能无条件自动重放。
- 缓存键必须包含影响响应的身份、分页和质量参数。
- 播放 URL 视为短期资源，不长期持久化为可靠地址。

## 本地文件与外部 Intent

- 本地音乐必须复制到 App 专属音乐目录后才能进入本地库。
- `ACTION_VIEW` 表示“导入 + 播放”，播放命令只能在文件与数据库提交成功后发出。
- 不持久化依赖外部 URI 的播放记录，不把绝对文件路径暴露到领域层。
- 文件复制使用临时文件、流式哈希和原子提交；失败与取消必须清理残留。
- 不申请 `MANAGE_EXTERNAL_STORAGE`，也不为第一版导入申请公共目录写入权限。

## Compose 性能

- Lazy 列表提供稳定 key 和合理 contentType。
- 避免在组合期间进行 JSON 解析、图片取色、数据库或网络操作。
- 派生值使用 `derivedStateOf`，但不滥用 `remember` 掩盖错误状态设计。
- 大封面使用适配尺寸加载，不解码不必要的原图。
- 动态背景取色与模糊在后台计算，并提供静态回退。
- 性能结论以 Layout Inspector、Compose metrics 和 Macrobenchmark 为依据。

## 可访问性

- 文字与背景满足合理对比度。
- 点击目标不小于平台建议尺寸。
- 进度条、播放模式、收藏状态有可读语义。
- 不只依赖颜色表达播放、选中或错误状态。
- 支持系统字体缩放，关键控制不能因大字体消失。

## 测试要求

### 单元测试

- 签名、加密和解析。
- DTO/Entity/Domain 映射。
- ViewModel 状态变化。
- 播放队列和播放模式状态机。
- 本地音乐复制、去重、回滚和外部 Intent 解析。

### UI 测试

- 首页、搜索、详情、音乐库和播放器关键状态。
- 深色、浅色、纯黑与大字体。
- 手机宽度与至少一种宽屏布局。

### 集成测试

- 设备注册到匿名播放闭环。
- 登录和会话恢复。
- 后台播放、通知栏和进程重建。
- 歌单写操作的成功、失败与重试。

涉及真实酷狗服务的测试不得成为普通单元测试的硬依赖，应明确标记并手动或定时执行。

完整模块矩阵、设备版本和 CI 顺序见 [`TESTING_STRATEGY.md`](TESTING_STRATEGY.md)。

## Git 约定

- 分支和提交保持单一目的。
- 推荐使用 Conventional Commits：`feat`、`fix`、`refactor`、`test`、`docs`、`build`、`chore`。
- 不提交生成目录、IDE 用户配置、真实凭据和本地抓包。
- PR 或变更说明包含：目标、主要实现、测试结果、风险和截图。
- 架构决定通过 ADR 记录，不把关键理由只留在 Issue 或聊天中。

## 依赖准入

增加依赖前检查：

- 是否能由 AndroidX/Kotlin 标准能力完成。
- 最近维护状态和已知兼容问题。
- 许可证是否与项目兼容。
- 是否引入遥测、远程服务或闭源二进制。
- 对 APK、启动、编译和混淆的影响。

## 日志与隐私

- 统一日志门面，Release 默认关闭详细协议日志。
- token、Cookie、手机号、设备标识只允许显示脱敏摘要。
- 崩溃信息不上传第三方，除非未来经明确讨论和用户选择加入。
- 不记录完整搜索和播放历史到非必要位置。

## Definition of Done

功能完成至少满足：

- 行为实现并覆盖异常状态。
- 相关测试通过。
- 无新增敏感日志和未经批准的网络调用。
- UI 符合 Design System 与可访问性约束。
- 文档和 ADR 与实现一致。
- 已说明验证命令和仍存在的限制。
