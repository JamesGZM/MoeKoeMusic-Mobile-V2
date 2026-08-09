# 缓存管理与清理范围参考审计

状态：Accepted（仅 `ClearCache`）。审计日期：2026-08-10。

## 准入结论

“清理缓存”可以作为一个独立、真实的 Android 纵向闭环开始；“缓存上限”不能随之开启，状态为 **Deferred**。两者的产品目标、持久化语义、淘汰策略和失败边界不同：前者是用户明确发起、白名单内的可重试清除；后者需要长期容量预算、逐类 LRU/淘汰与空间不足策略。不能用一个可点但不限制任何存储的开关来伪装后者。

PC 与旧 Mobile 均没有此功能可迁移。固定 `../MoeKoeMusic@52c9833afe2e7fedcba8d5b23ff8d1f9731af73a`（GPL-2.0-only）的 `src/views/Settings.vue`、`src/config/settings.js` 只含设置与快捷键清理，不含缓存清理/上限；固定 `../MoeKoeMusic-Mobile@ab71195d4cf3297332490fd37704d1ae8973d4c5`（GPL-2.0-only）的 `src/app/settings.tsx`、`src/features/settings/store.ts`、`src/features/settings/storage.ts` 也不含该能力。因此以下均是 **Android 新决策**，不是旧端迁移事实。

## 冻结产品语义

设置页的“清理缓存”是一次确认后的异步动作，不显示或伪造“426 MB”等总容量。成功仅表示：截至该动作完成时，白名单内的持久内容缓存及 Coil 活跃缓存均已收到清除；它不保证正在显示的图片/歌词立即从内存对象消失，也不影响播放和用户数据。

清理白名单：

1. Room `home_content_snapshots` 的所有身份分区快照；
2. Room `lyrics_cache` 的全部 KRC 原文缓存；
3. 当前 Coil 3.4 singleton `ImageLoader` 的 memory cache 与 disk cache。

明确禁止：

- `MoeKoeDatabase` 的 `PlaybackSnapshotEntity`、队列项、`LocalMusicEntity`、导入批次/条目及任何其他表；数据库实体全集见 `core/database/src/main/kotlin/cn/james/music/core/database/MoeKoeDatabase.kt:17-37`。
- 本地音乐的 `externalFilesDir/Music` 与 `filesDir/local-artwork/*`。前者和本地库行由 `data/src/main/kotlin/cn/james/music/data/local/LocalMusicRepositoryImpl.kt:182-187` 删除，后者由 `data/src/main/kotlin/cn/james/music/data/local/LocalImportWorker.kt:359-362` 写入，均是用户可播放内容而非可随意丢弃的缓存。
- DataStore、登录会话/Cookie、Room 数据库文件本身、播放 Service 的运行时队列/位置、下载/导入中间状态及 `filesDir` 根。
- `context.cacheDir` 根目录的递归删除。它只是 Android app-specific cache 容器；本项目当前生产代码未定义可由设置页删除的额外根级目录。Coil 必须由自己的 `diskCache.clear()` 管理目录和并发编辑，不能绕过其文件系统直接删目录。debug `test-audio/` 不属于发布产品语义。

`CacheLimit` 仍保持“尚未开放”。后续单独审计必须先定义：容量究竟覆盖图片、首页 JSON、歌词、下载文件中的哪些项；每项字节统计、淘汰顺序、网络/离线回填、系统已经回收 cacheDir 时的行为，以及对本地音乐零影响。它不得复用本次的“清除成功”结果或新增假容量。

## 当前实现事实与失效边界

### Room 与首页

`HomeContentSnapshotDao.observe(cacheKey)` 是可观察的单行查询，删除会发出 `null`，见 `core/database/src/main/kotlin/cn/james/music/core/database/home/HomeContentSnapshotDao.kt:9-20`。`KugouHomeRepository` 订阅该 Flow 后第一次才自动刷新一次（`refreshStarted`），见 `data/src/main/kotlin/cn/james/music/data/home/KugouHomeRepository.kt:98-108`；`HomeViewModel` 收到 `null` 会清空歌曲映射并进入 missing/Loading（除非当前保留失败态），见 `feature/home/src/main/kotlin/cn/james/music/feature/home/HomeViewModel.kt:58-79`。

因此清除后，当前可见首页应**立即成为 missing/Loading，但不得由“清除”本身另起网络刷新**。这使离线清除也诚实可用；下一次页面新订阅的既有自动刷新或用户手动刷新才获取新内容。不得在清除完成后继续显示已被声明删除的旧 Home snapshot。

现有首页已有 `flightMutex` 与 per-cache-key generation，并在写入前校验 generation，见 `data/src/main/kotlin/cn/james/music/data/home/KugouHomeRepository.kt:129-164,222-251`；但清除动作目前无法递增该 generation 或删除所有 key。因此首个实现必须在同一 repository/共享失效器内使清除与 `persistIfCurrent` 串行：先使所有 home cache generation 失效，再以 Room transaction 删除行；任何先前网络 flight 的迟到成功必须返回 Superseded，不能在清除后重新 upsert。

### 歌词与当前可见内容

`KugouLyricsRepository` 以 source key single-flight，并在网络解析成功后写 `lyrics_cache`，见 `data/src/main/kotlin/cn/james/music/data/lyrics/KugouLyricsRepository.kt:41-70,99-123`。它也没有当前全表清除/失效代际；实现必须让清除提升 lyrics cache epoch，并使清除前起飞的结果仍可返回给原调用者、但**不得**在 epoch 改变后落盘。下一次 repository 请求必为 disk miss 并按现有逻辑取网络。

不清空 `AppPlayerLyricsViewModel.successfulContent`。它是当前 `itemId + source` 的单条 UI 内存内容，不是无界持久歌词缓存，且仅在同一 key/歌词页可见时复用，见 `app/src/main/kotlin/cn/james/music/AppPlayerLyricsViewModel.kt:35-73,97-103`。清除时让正在阅读的歌词闪回 Loading 或强制网络请求既不释放其 UI 已引用文档，也破坏“清理缓存不打断播放/阅读”的语义。离开该 item 后它会按原有逻辑失效；重新进入同 item 的短暂 UI 内存复用允许存在，但 disk 已不会命中旧文本。

### Coil 与 app cacheDir

工程锁定 `io.coil-kt.coil3` 3.4.0，见 `gradle/libs.versions.toml:28,80-81`。`MoeKoeApplication` 当前未配置自定义 Coil factory，见 `app/src/main/kotlin/cn/james/music/MoeKoeApplication.kt:9-16`，故 `AsyncImage` 使用 Coil 默认 singleton。固定 Coil `51638b0`（Apache-2.0）表明：`SingletonImageLoader.get(applicationContext)` 返回唯一实例，Application 可作为 factory（`coil/src/commonMain/kotlin/coil3/SingletonImageLoader.kt:11-18,100-118`）；其 `MemoryCache.clear()` 和 `DiskCache.clear()` 是公开 API（`coil-core/src/commonMain/kotlin/coil3/memory/MemoryCache.kt:40-44`、`coil-core/src/commonMain/kotlin/coil3/disk/DiskCache.kt:49-57`）。

首个实现由 `:app` 建立一个只暴露“清除当前 singleton image caches”的 adapter；它必须对同一个 singleton 调用 API，不能另建 ImageLoader、调用 `SingletonImageLoader.reset()`，或直接删除默认 `cacheDir/image_cache`。后两者会与活动 Snapshot/Editor 冲突；Coil 明确要求同一目录不能有两个活跃 DiskCache 实例（同文件:112-116）。

## 并发、取消与结果

清除是单飞操作：Settings 只启动独立 clear job/generation，不能取消主题、自动跳过、动态色、歌词、字号、音质或品牌色保存。重复点击在确认后禁用该行/确认按钮并复用当前 job；Dismiss 仅关闭反馈，不取消已经开始的 I/O。

034 已在 `data/src/main/kotlin/cn/james/music/data/cache/RegenerableContentCache.kt` 使 Room 两张白名单表通过一次 `RoomDatabase.withTransaction` 删除，并以 singleton generation 与同一 `Mutex` 串行清理和落盘；`:data` 仅显式增加已锁定的 `androidx.room.ktx` 直接依赖，以使用该 Room Kotlin API，不引入新的依赖版本或能力。Home 在 clear 前起飞的成功映射为 `Superseded`，Lyrics 仍返回原调用者的解析结果但不再写盘。Coil 与 Room 之间无法组成跨存储事务，结果因此为类型化而非乐观 Boolean：

- `Cleared`：Room transaction 与 Coil memory/disk 都成功；显示“缓存已清理”。
- `PartiallyCleared(failedScopes)`：已完成 scope 保留完成状态，显示“部分缓存未清理，可重试”；重试为幂等的整次白名单清除，不能回滚已删除数据。
- `FailedBeforeAnyClear`：显示可重试错误；不伪报成功。
- `CancellationException`：原样传播、不显示普通失败；已经完成的 scope 可保持已清，下一次重试完成剩余 scope。

统计首片只记录类型化 scope 结果，**不显示总字节数或持久化历史**。Room payload、Coil memory、Coil disk 与 app cacheDir 不能在无统一原子快照时组成可靠总数；显示一个不准确的聚合数字比不显示更差。CacheLimit 或“已释放空间”未来另有审计后才能建立统计口径。

## 所有权与接口草图

不新增依赖、网络、权限、Worker、日志或后台常驻任务。建议保留现有模块方向：

```text
:feature:settings SettingsViewModel
  -> :core:model CacheMaintenanceRepository (typed clear/result port)
  -> :data CacheMaintenanceRepositoryImpl
       -> Room whitelist + home/lyrics invalidation coordinator
       -> :core:model ImageCachePort
:app CoilImageCachePortImpl -> SingletonImageLoader.get(applicationContext)
```

- `:core:model` 仅定义 `CacheMaintenanceRepository.clearCaches()`、不可变 `CacheClearResult`/scope；不含 Room、Context、File、Coil 类型或可显示容量。
- `:data` 拥有 Room whitelist、`withTransaction`、home/lyrics epoch 与错误到 typed result 的映射；`RegenerableContentCache` 仅枚举 Home/歌词两种 scope，不能调用 `clearAllTables()`，也不得依赖 Compose/Media3。
- `:app` 是 Coil singleton 的唯一适配与 Hilt binding 点；adapter 可注入 application context，但不把 ImageLoader 交给 UI 或 data model。
- `:feature:settings` 只呈现确认、saving、success/partial/failure + Retry state，不读取数据库、DataStore、Context 或 Coil。现有静态行位于 `feature/settings/src/main/kotlin/cn/james/music/feature/settings/SettingsUiModels.kt:275-281`。
- `:feature:home` 只消费既有 `HomeRepository` Flow；`AppPlayerLyricsViewModel` 不新增清除入口。

## 参考采用与拒绝

- **Coil 3.4.0 / `51638b0`，Apache-2.0**：采用 singleton 生命周期和 API-owning cache clear；不复制其默认容量作为产品“缓存上限”，也不手删其 directory。
- **AntennaPod / `d39bf05d21bddd10c844357e1253f7fab530bfaf`，GPL-3.0-only**：`net/download/service/src/main/java/de/danoeh/antennapod/net/download/service/episode/autodownload/EpisodeCleanupAlgorithm.java:10-57` 把 episode cache limit 定义为下载媒体的“腾出多少条目”策略。仅采用“容量淘汰是独立策略”的分片原则；不采用代码、数据库读取、媒体删除或 GPL 实现。MoeKoe 的本地音乐必须继续排除。
- **Android 官方**：[app-specific storage](https://developer.android.com/training/data-storage/app-specific) 将 `cacheDir` 与 `filesDir` 分开、说明系统可能回收 cache files 且 app 应维护自己的缓存；[Room asynchronous queries](https://developer.android.com/training/data-storage/room/async-queries) 说明 observable query 在底层表变化时发出新值。采用目录白名单、异步 DAO/观察失效；不调用全设备 `ACTION_CLEAR_APP_CACHE`，不申请 `MANAGE_EXTERNAL_STORAGE`。

## 原子实施顺序与测试矩阵

1. **数据库与 data 失效边界**：已完成（034）。两 DAO 增加无参数 `deleteAll`，`RegenerableContentCache` 通过同一 Room transaction 清除两表，并在 transaction 成功后推进 generation；Home/Lyrics 在写入前持有同一 generation，迟到 Home 返回 `Superseded`、迟到 Lyrics 只返回调用者且不写盘。JVM 覆盖白名单 scope、串行、Room failure、取消和迟到写入；DAO AndroidTest 增加 all-key 删除断言（本切片只编译，未运行设备）。清除本身没有调用 Home/Lyrics 网络 API。
2. **typed maintenance port + Coil composition**：已完成（035）。`:core:model` 公开纯 `CacheMaintenanceRepository`、结果和 scope，以及供 `:app` 实现的纯 `ImageCacheClearer` 端口；`:app` 的 `CoilImageCacheClearer` 在可注入的 `Dispatchers.IO` 上只从 `SingletonImageLoader.get(applicationContext)` 取得当前 singleton，并依序调用其 memory / disk clear，不 reset、不新建 loader、不删除目录。singleton 获取的普通失败会如实报告 memory / disk 均未清除；`CancellationException` 原样传播。`:data` 的 `AndroidCacheMaintenanceRepository` 单飞组合 Room 白名单 clear 与 image port；除 `CancellationException` 外两侧均尝试，按已完成与失败 scope 返回 `Cleared` / `PartiallyCleared` / `FailedBeforeAnyClear`。JVM 覆盖全成功、Room/image 各种部分失败、全失败、同飞、取消、singleton 获取失败和同一 Coil operations 实例的 memory→disk 顺序；不会产生容量或持久化清除历史。
3. **Settings 真实闭环**：036 已完成编码前契约。`ClearCache` 是不展示容量或历史值、保留 Chevron 的 Action；以同一 Settings 页面与约 32% 黑色遮罩呈现 `MoeAlertDialog` 危险操作。正文只列首页、歌词、图片和重新下载后果，并明确排除本地音乐、账号、播放记录。提交前 Back/遮罩可关闭，提交中两个按钮禁用、主按钮加载且不允许关闭；成功关闭 Dialog 并显示无 action 的成功 Snackbar，Partial/Failed 关闭 Dialog 并显示带 Retry 的错误 Snackbar，取消不显示普通失败。清理与 CacheLimit 不共用动态区域：前者有独立局部 region，后者继续 Deferred / Unavailable。下一代码切片接入独立 saving/retry；JVM 覆盖重复点击单飞、partial/retry、dismiss、其他设置写入不互相取消；Compose 覆盖确认/禁用/错误语义与取消。
4. **视觉与门禁**：补确认/保存/部分失败设计 contract、截图和局部 fidelity；不改 CacheLimit 行，也不放宽阈值或遮罩。最后运行受影响 core/data/app/settings 的 unit/compile/lint、contracts/fidelity/impact/golden、architecture/skill/agent governance 与 diff check。

必测竞态：Home refresh A 在 clear 前发起、clear 后 A 返回不得回写；lyrics fetch A 同理；清除时当前歌词仍显示、离开后下一次请求不读旧 disk；Coil disk 失败但 Room 成功的 Partial 和幂等 retry；取消前/后各 scope；播放队列、本地音乐、会话、DataStore 与 Room 非白名单表均保持不变。

## 未验证项

036 已建立 Settings 确认/进行中/成功/Partial/Failed 的编码前契约，但没有创建 Compose、测试、截图或 golden。本审计与契约未运行模拟器、ADB、真机或真实服务；Coil 默认 disk 目录在目标设备上的实际大小、系统低空间回收、图片 request 与 clear 同时发生的厂家文件系统表现，均留给实现后的指定真机验收。下一切片必须在编码后补 Dialog 与行为测试、截图、fidelity 和完整门禁；不存在需要用户重新决定的产品或依赖阻塞。
