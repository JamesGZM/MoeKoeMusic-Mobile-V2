# 测试策略

状态：第一版工程测试基线。本文定义单元测试、组件测试、设备测试、截图测试和 CI 的职责边界。

## 总体规则

- 新行为与测试同一提交；缺陷修复先增加能复现问题的测试。
- 测试验证行为和边界，不为数字覆盖率编写无断言测试。
- 普通单元测试不访问真实酷狗服务、不读取用户真实文件、不依赖执行顺序。
- 使用固定的虚构设备、账号、Cookie 和文件数据；日志与失败输出不得包含敏感信息。
- 先执行受影响模块的快速测试，再执行跨模块或设备测试。
- 当前开发会话的设备验收只使用用户指定且通过 `adb devices` 确认在线的真机，不启动、创建或用模拟器替代本轮验收；历史 AVD 结果保留为历史证据，不代表当前提交已经完成设备验证。

## 模块单元测试

### `:core:model` 与 `:core:common`

- 领域模型、分页、时间和播放模式规则。
- 类型化错误的创建、转换与可恢复性。
- 不依赖 Android Framework；使用纯 JVM 测试。

### `:kugou-api`

- MD5、SHA、AES、RSA 固定测试向量。
- 请求参数排序、签名、Header、Cookie 与会话序列化。
- 动态 JSON 缺字段、字段类型漂移和错误响应映射。
- 与固定版本 KuGouMusicApi 生成的脱敏 fixture 逐项对照。
- 真实酷狗服务测试单独标记为手动或定时集成测试。
- Endpoint 首次迁移和兼容性修复必须运行真实服务测试；只有固定向量或 Mock/Fake 通过，不得宣称接口已经可用。
- RequestFactory 使用固定时钟、虚构身份和精确 Body 字节生成 Node/Kotlin 快照；Fake Transport 验证 Endpoint 不依赖真实网络。
- Cookie 覆盖属性剥离、值中等号、删除和 Header 注入；响应覆盖 HTTP、SSA 风控、非 Object JSON、服务端拒绝及脱敏 `toString()`。
- Ktor Transport 使用预配置 OkHttp Engine 的离线 Interceptor 验证 URL 编码、Header、二进制 Body、挂起执行与 IOException 映射；协程取消由 Ktor 传播到底层 Call，不启动真实酷狗请求。
- 重试测试使用结果队列和虚拟 Delayer，精确断言超时/5xx 的次数与 250ms、500ms 退避，并验证其他错误不重放。
- 首批四个 Endpoint 对路径、Host、Router、JSON Body、响应格式、重试属性和固定 `song_url` 签名做快照断言。
- `LiveKugouIntegrationTest` 通过 `MOEKOE_RUN_LIVE_KUGOU_TESTS=true` 显式启用，覆盖纯协议层的真实匿名设备注册与匿名歌曲搜索。

### `:data`

- DTO、Entity、Domain 映射。
- Repository 的数据源选择、缓存、会话恢复和错误转换。
- 本地音乐复制、哈希去重、原子提交、取消与回滚。
- 空间不足、源不可读、格式错误、数据库失败和重复内容。
- 通过 Fake 传输、文件源、存储空间检查器和时钟保持确定性。
- 加密会话覆盖往返、明文不落盘、损坏密文、key 丢失、清除和协程取消；AES-GCM 与 Android Keystore 的真实组合使用设备测试，不能只用 JVM Fake Cipher 代替。
- `LiveKugouSearchRepositoryTest` 使用同一显式环境变量，覆盖真实注册、搜索、DTO 解码和领域映射；失败信息只包含类型化错误，不输出会话值、响应正文或歌曲内容。
- `LiveKugouPlaybackSourceResolverTest` 在源码与 fixture 测试完成后验证当前 `privilege_lite` 结构和 `song_url` 安全地址；字段格式以固定 PC/Mobile 源码为依据，真实测试不用于探索已知结构。最终播放由真机 Media3 的 Playing 状态、递增位置、缓冲和媒体元数据验收。

### `:playback`

- 队列添加、删除、移动、下一首播放和恢复。
- 顺序、随机、单曲循环、列表循环。
- 当前歌曲被删除、播放错误跳过和地址失效刷新。
- 本地与在线播放源解析。
- 高频进度流不触发无关页面状态更新。

### `:feature:*`

每个 ViewModel 至少覆盖初始、加载、内容、空数据、错误、重试和取消。搜索与切歌验证旧请求不能覆盖新状态；Snackbar、Dialog、Toast 与导航 effect 验证唯一消费。

重点测试：首页、发现、搜索、我的、用户主页、本地音乐、导入进度、播放器和队列。

## 外部 Intent 与导入测试

- `ACTION_VIEW` 单文件的冷启动和热启动。
- `ACTION_SEND` 与 `ACTION_SEND_MULTIPLE`。
- URI、MIME、读取授权或流缺失。
- HTTP/HTTPS 与不支持的 Scheme 被拒绝。
- 已导入内容复用现有记录。
- 只有文件落盘和 Room 提交都成功后才发送播放命令。
- 失败时不回退播放外部临时 URI。

Intent 解析使用独立纯函数配合 Robolectric/Android 测试验证平台差异；复制状态机通过抽象输入流和文件系统边界在 JVM 测试。

格式 fixture 由 `scripts/generate-local-music-fixtures.sh` 使用 440Hz、1 秒合成音调生成 MP3、M4A/AAC、FLAC、Ogg/Opus 和 WAV。默认输出位于被 Git 忽略的 `build/local-music-fixtures/`；设备回归使用同一脚本生成并提交到 `app/src/debug/assets/local-music-fixtures/` 的 Debug-only 副本。不得使用第三方歌曲代替。

阶段 3 的 `LocalImportWorkerTest` 必须从 ContentProvider 经 ContentResolver、WorkManager、文件系统和 Room 走完整公开管线，覆盖五种格式、损坏输入、重复内容、部分成功、复制中取消和遗留 `.partial` 恢复。Debug 探针只读取可观察结果，不绕过导入实现；Release 不包含 Provider、探针或 fixture。

## Compose UI 与截图测试

主要页面覆盖加载、内容、空数据、错误、离线、浅色、深色、纯黑和大字体。导航测试明确底部只有“首页、发现、我的”，搜索不是一级 Tab。

截图基准覆盖：首页、发现、搜索、我的、用户主页、歌单详情、播放封面、歌词、队列、本地音乐、Dialog 和 Snackbar。搜索当前已覆盖未搜索、内容和 `1.5×` 字体；其余页面随阶段落地补齐。至少验证标准手机宽度、深色主题与 `1.5×` 字体；关键控制另验证 `2.0×` 字体不消失。

截图用于发现排版和 token 回归，语义、点击和滚动行为仍由 Compose UI 测试断言。

## 集成与设备测试

设备矩阵至少包含 API 26、32、33、36，覆盖：

- 媒体权限分界、拒绝与再次授权。
- Storage Access Framework 单选和多选。
- 文件管理器外部打开、系统分享和 URI 授权。
- 后台播放、通知栏、锁屏、耳机和音频焦点。
- 进程重建、队列恢复和源文件删除。
- 深色、横屏、TalkBack 与系统字体缩放。

本地音乐核心场景：文件管理器选择 MoeKoe 后显示导入进度，副本与数据库均成功后立即播放；删除源文件仍可播放；再次打开相同内容直接复用；任何导入失败都不播放原始 URI。

## CI 门槛

工程建立后，PR 按顺序执行：

1. Kotlin/Gradle 格式与静态检查。
2. 受影响模块单元测试。
3. 全部 Debug 单元测试。
4. Lint 与 Debug 构建。
5. 稳定的截图测试。

设备矩阵和真实接口验证可在专用工作流运行，但合并前必须有对应结果。签名、导入、队列或播放模式测试失败时不得跳过合并。

真实酷狗验证使用：

```bash
MOEKOE_RUN_LIVE_KUGOU_TESTS=true \
  ./gradlew :kugou-api:test :data:testDebugUnitTest \
  --tests '*LiveKugou*Test' \
  --rerun-tasks
```

该命令不是普通 CI 的替代品；提交前仍需先通过默认离线测试。若真实服务失败，记录日期、Endpoint、HTTP/协议错误分类和是否能由固定基准复现，不记录正文或身份数据。
