# 首页真实内容与缓存参考审计

状态：Accepted。审计日期：2026-08-06。

## 产品定义

本审计开放首页真实数据纵向切片。首页在 Android 系统 Splash 后立即显示应用壳，并以 stale-while-revalidate 读取真实轮播、每日推荐和推荐歌单；搜索只提供已有独立搜索页入口。排行榜、新歌、新专辑和分类歌单继续归发现页，不因 Mobile 参考实现曾在首页聚合这些内容而提前迁入。

成功语义是：有缓存时首帧直接展示最后一次完整成功快照并在需要时后台刷新；无缓存时展示首次加载，至少每日推荐或推荐歌单形成可展示内容后进入内容态。远端刷新失败不得清空旧快照或退回全屏 Loading。首页不等待跨业务启动状态，不增加权限、后台任务、WorkManager、遥测或额外业务网络请求。Compose 真机验收确认工程原有 Coil 3 只有 UI artifact，按 Coil 官方要求在 `:app` 补齐同版本 `coil-network-okhttp`；这只恢复既有 HTTPS 封面加载能力，不向 Feature 暴露网络客户端或增加业务 Endpoint。

非目标：本切片不实现发现页、歌单详情、听歌识曲、任意服务端跳转链接、登录闭环、收藏/更多菜单和新播放器能力。首页歌曲点击只复用既有在线播放与队列端口；未落地详情目的地的“更多”入口不得用占位页面伪装完成。

## Android 官方约束

- [Android offline-first 指南](https://developer.android.com/topic/architecture/data-layer/offline-first)要求网络 Repository 同时具有本地数据源，并由 Repository 同步本地与远端；UI 不直接读取网络。首页因此以 Room 快照作为持久来源，由 `:data` 负责刷新和提交。
- [Data layer 指南](https://developer.android.com/topic/architecture/data-layer)要求 Repository 定义单一事实来源并保持 main-safe。首页 ViewModel 只消费 Repository 的 `Flow`/挂起刷新结果，不持有 DAO、Client 或 Dispatcher 细节。
- [Room migration 指南](https://developer.android.com/training/data-storage/room/migrating-db-versions)要求保留 schema 历史并测试迁移。新增首页快照表使用显式 `4→5` 迁移，提交 v5 schema，并同时验证 `4→5` 与完整 `1→2→3→4→5`；禁止 destructive fallback。
- `minSdk 26` 至 `targetSdk 36` 不需要新增版本分支或运行时权限。网络能力沿用现有 `INTERNET`；页面刷新是前台、可取消读取，不建立后台调度。

## 固定源码证据

### KuGouMusicApi

- 仓库：`MakcRe/KuGouMusicApi`；固定提交：`6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb`；MIT。
- 本地文件：`../MoeKoeMusic/api/module/yueku_banner.js`、`everyday_recommend.js`、`top_playlist.js` 与 `util/request.js`。

采用：目标 Host、Path、Method、Android 签名/加密、`x-router`、固定请求参数和显式 Cookie/用户身份。只迁移这三个 Endpoint 所需常量与字段，保留 MIT 来源。

拒绝：不迁移 Node/Express、整份平台配置、无关凭据、响应正文日志或首页未消费的 Endpoint。

### MoeKoeMusic Mobile

- 固定提交：`ab71195d4cf3297332490fd37704d1ae8973d4c5`；API submodule：`283f1e97b110726b208a64b486a657c0fc0a6126`；GPL-2.0-only。
- 本地文件：`../MoeKoeMusic-Mobile/src/features/home/load-home-data.ts`、`src/app/(tabs)/index.tsx`，以及 API submodule 同名 Endpoint 文件。

采用：并发加载独立区块、关键字段缺失时过滤单项、请求代际隔离、已有内容刷新失败时保留内容，以及图片尺寸占位符归一化规则。

拒绝：不复制 Expo/Tamagui、弱类型 `Record` 解析、随机打乱每日推荐、硬编码个人歌单 Banner、服务端错误字符串直出 UI、仅内存缓存，以及把榜单和新歌重复放入首页。

### MoeKoeMusic PC

- 固定提交：`52c9833afe2e7fedcba8d5b23ff8d1f9731af73a`；GPL-2.0-only。
- 本地文件：`../MoeKoeMusic/src/components/home/HomeRecommendations.vue`。

采用：MoeKoe Radio、排行榜与歌单作为首页高层入口的产品语义，以及歌曲加入既有播放队列的边界。

拒绝：不复制 Vue/localStorage、静态角色资产、固定歌单 ID、页面内播放器逻辑或桌面三列布局。Android 首页的信息层级以已确认移动设计图为准。

## Endpoint 与类型边界

| 能力 | 固定请求 | 关键响应路径 | Kotlin 稳定结果 |
| --- | --- | --- | --- |
| 轮播（可选） | `POST /ads.gateway/v3/listen_banner`，Android 加密；Body 保留 `plat=0`、`channel=201`、`operator=7`、`networktype=2`、`apiver=5`、`ability=2`、`mode=normal` 等固定字段，`userid` 来自会话否则为 `0` | `data.ads[]`；图片读取 `img_url/image`，标题读取 `title/extra.title`，ID 缺失可用规范化图片 URL | `HomeBanner`；图片必填，标题可使用本地通用文案；首切片不暴露远端跳转 URL。2026-08-06 当前服务返回 `31136`，因此不作为首版完整快照的必需区块 |
| 每日推荐 | `POST /everyday_song_recommend?platform=ios`，Android 加密，`x-router=everydayrec.service.kugou.com` | `data.song_list[]`；`hash` 必填，标题按 `ori_audio_name/songname/filename`，歌手、封面、专辑、时长和权益字段按固定 Mobile 消费层容错 | 复用稳定 `Song`，附加首页推荐说明只进入 Home 领域模型；不得把 DTO 直接交给 UI |
| 推荐歌单 | `POST /v2/special_recommend`，Android 加密，`x-router=specialrec.service.kugou.com`；固定 `platform=android`、分页、签名 `key` 与 `special_recommend` Body | `data.special_list[]`；`global_collection_id` 与 `specialname` 必填，封面读取 `flexible_cover/cover`，播放量只作展示映射 | `HomePlaylist`；GID、标题必填，封面和播放量可空 |

三者都是幂等读取，可复用现有有限重试策略；取消必须直接传播。关键容器不是 Object、必需列表不是 Array、歌曲/歌单全部因关键字段无效而被过滤时返回类型化 `Protocol`，不能制造空成功。HTTP、网络、会话、服务拒绝和协议错误继续使用现有稳定错误分类；服务端正文和字段值不进入日志或 UI 文案。

真实服务只在固定快照、Decoder 和 Fake Transport 测试通过后运行受控兼容检查。首次实现时分别断言 Endpoint 被接受、类型化响应可解码和至少一个必要字段成立；不得保存响应正文、设备身份、Cookie、歌曲内容或图片 URL。

## 快照与刷新决策

`:core:database` 新增单行快照表 `home_content_snapshots`：

- `cache_key TEXT PRIMARY KEY`：`home:v1:anonymous` 或 `home:v1:user:<userid>`；退出登录后不读取用户分区，匿名与不同账号不得互相覆盖。
- `payload_json TEXT NOT NULL`：`:data` 定义的版本化缓存 DTO JSON，只保存已验证并映射后的 Banner、Song 与 Playlist 展示字段；不保存原始响应、Cookie、token、播放 URL 或协议错误。
- `schema_version INTEGER NOT NULL`、`updated_at_epoch_ms INTEGER NOT NULL`。

采用单行版本化 JSON，而不是把网络 JSON 原样落盘或为首页展示建立多组永久关系表。首页是服务端组合快照，没有本地编辑与跨页关系写入；单行事务能保证三个区块同时替换。缓存 DTO 与网络 DTO、Room Entity、领域模型、UI Model 保持分层，损坏 JSON、未知 schema 或非法关键字段会删除当前分区并按无缓存处理。

刷新规则：

1. Repository 先读取当前身份分区的 Room 快照并立即暴露。
2. 快照未超过 15 分钟时，普通页面重新订阅不重复请求；无缓存、过期或用户显式刷新时启动远端刷新。15 分钟只控制自动刷新频率，不是展示硬过期时间；有效旧快照在离线时可继续显示。
3. 三个 Endpoint 并发读取。每日推荐和推荐歌单成功、映射后至少一个必需区块非空，才在一个 DAO 事务中替换持久快照；轮播成功时一并保存，类型化不可用时保存空轮播且不伪造数据。
4. 任一必需区块失败时不得覆盖旧快照。有旧快照则继续暴露旧内容并返回非阻断刷新问题；无旧快照时可暴露本次内存中的有效区块和问题标记，但不持久化部分结果。每日推荐与推荐歌单均不可展示时返回首次加载错误；可选轮播失败不单独触发页面错误。
5. 每个 `cache_key` 只允许一个刷新 single-flight。显式刷新开始新代际；较旧结果即使更晚完成也不得提交或覆盖新状态。调用方取消时不转换成普通失败，也不写入半成品。
6. 会话身份变化后切换缓存分区并启动该分区读取/刷新；不复制、合并或删除其他账号快照。缓存清理属于后续设置能力，不在首页首切片增加全局清理任务。

## 模块、接口与数据流

```text
HomeRoute / HomeViewModel (:feature:home)
        │ HomeUiState + refresh/play/navigation events
        ▼
HomeRepository (:core:model)
        │ observe current snapshot / refresh current identity
        ▼
KugouHomeRepository (:data)
        ├── HomeContentSnapshotDao (:core:database)
        ├── KugouOnlineClient (:kugou-api)
        └── current Kugou session scope
```

- `:kugou-api` 增加三个类型化 Endpoint 和 Decoder；动态字段容错止于协议层。
- `:core:model` 拥有稳定首页领域模型、类型化刷新问题和 `HomeRepository` 端口，不依赖 Room/Ktor/Compose。
- `:core:database` 拥有 Entity、DAO、v5 Schema 与迁移，不解析网络 DTO。
- `:data` 拥有缓存 DTO、映射、身份分区、完整快照事务、single-flight 与代际隔离。
- `:feature:home` 拥有 Route、ViewModel、UI Model 和页面事件；Screen 不访问网络、DAO、DataStore、播放器或 NavController。播放事件通过既有应用组合根端口接入，不把 ExoPlayer 下沉到 Feature。

不建立 `HomeUseCase`、`AppStartupRepository`、跨 Feature 依赖或新的 `api/impl` 模块；当前规则由单一 Repository 足够表达。

## 视觉状态门禁

| 状态 | 页面语义 | 已确认基线 |
| --- | --- | --- |
| 正常内容 | 搜索入口、Hero/轮播、三个高层入口、每日推荐、推荐歌单；MiniPlayer 与底部导航仍归应用壳 | `01-home-material3-v2.png`、`17-music-content-components.png` |
| 首次加载 | 无缓存时显示页面内骨架，不阻断应用壳 | `18-mobile-states-overlays.png` 的加载态，复用首页完整页面基座 |
| 空数据 | 请求成功但没有可展示歌曲/歌单时给出可重试空态 | `18-mobile-states-overlays.png` 的空状态 |
| 首次错误/离线无缓存 | 全页可恢复错误，提供重试；不宣称网络状态一定可用 | `18-mobile-states-overlays.png` 的可恢复错误与离线无缓存态 |
| 离线或刷新失败且有缓存 | 保留完整内容，使用一条非阻断 `MoeSnackbar`；不回到 Loading | `18-mobile-states-overlays.png` 的离线有缓存态、`12-feedback-components-v2.png` |
| 部分结果 | 无旧快照时只显示有效区块并提供弱反馈；不得使用假数据补齐 | `01-home-material3-v2.png` 页面基座 + `12-feedback-components-v2.png` 反馈层 |
| 刷新/恢复 | 下拉刷新或自动刷新只更新 `isRefreshing`；成功原位替换，取消静默保留现状 | `01-home-material3-v2.png` 页面基座 |

首页公开读取不需要权限；匿名态可用，不建立登录门槛。账号受限只在真实 Endpoint 返回相应类型化错误时以刷新问题表达，不用颜色或服务端字符串单独判断。现有确认图已覆盖正常结构和通用状态语言，不新增视觉形态，因此无需再次生成候选图；Compose 落地仍必须建立浅/深色、`1.5×`/`2.0×` 字体截图基准并检查 48dp 触控与可读语义。

## 实施切片与自动验收

1. 协议：三个 Request 快照、Decoder fixture、错误/取消/Fake Transport 测试；受控真实兼容测试独立显式启用。2026-08-06 自动真实测试确认每日推荐返回 30 项、推荐歌单返回 11 项；轮播固定请求被服务端以 `31136` 拒绝，最新上游源码仍与固定提交一致，因此只保留类型化可选能力，不宣称当前可用。
2. Room：已完成 v5 Entity/可观察 DAO、单行原子替换与删除、脱敏输出、导出 Schema、`4→5` 与完整 `1→5` 迁移测试；2026-08-06 在 ELE-AL00 / API 29 真机的 13 项数据库测试全部通过。损坏 payload 的识别与定向删除归 Repository 切片。
3. Repository：已完成稳定领域模型与端口、版本化缓存 DTO、身份分区自动切换/刷新、cache-first、15 分钟刷新门槛、部分失败不落盘、single-flight、显式刷新与会话双代际隔离、取消传播和类型化问题；15 项定向 JVM 测试覆盖旧缓存首发、自动刷新问题、TTL 边界、损坏删除、会话恢复、`A→B→A` 竞态及可选轮播失败。
4. Feature：已完成 ViewModel 首次/缓存/刷新/部分/空/错误/身份切换状态与显式刷新代际取消；9 项定向 JVM 测试覆盖首次会话失败顺序及账号切换与手动刷新并发，UI Model 不泄漏 DTO、Room 或播放器类型。
5. Compose：已按确认稿替换临时首页并复用 Design System；7 组浅色、深色、加载、空、错误、`1.5×` 与 `2.0×` 字体截图基线通过，ELE-AL00 / API 29 的 7 项 `MainActivityTest` 与真实内容自动截图通过；真机诊断同时确认 Coil 3 网络 artifact 已补齐，Room 中的 HTTPS 封面可实际渲染。需要用户手动判断的轮播手势、列表滚动体验和真实歌曲点击播放留作最终人工复测门槛。

首批自动命令按受影响范围逐步执行：

```bash
./gradlew :kugou-api:test :data:testDebugUnitTest
./gradlew :core:database:compileDebugAndroidTestKotlin :feature:home:testDebugUnitTest
./gradlew --no-configuration-cache :feature:home:validateDebugScreenshotTest
./gradlew spotlessCheck testDebugUnitTest lintDebug assembleDebug
```

真实 Endpoint 当前可用性必须在协议切片完成后再验证；Room 迁移的真实 SQLite 证据可在用户已连接且解锁的指定真机上自动运行。只有要求用户亲自扫码、登录、点击或主观观察的场景才构成人工门槛。

## 门禁结论

产品范围、Android 约束、三个 Endpoint、字段容错、身份分区、缓存 Schema/TTL、失败恢复、模块所有权、视觉状态和自动测试矩阵均无关键待定项。允许按上述顺序开始首页协议、缓存、Repository、ViewModel 与 Compose 原子切片；每日推荐与推荐歌单构成首版完整快照，轮播恢复前保持可选且不使用假数据。不得把发现页内容、任意远端跳转或假数据夹带进首页。
