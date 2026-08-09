# 默认播放音质与受控降级参考审计

状态：Accepted。审计日期：2026-08-10。

## 产品定义

设置页“默认音质”不是显示用的假值：它保存用户的**偏好上限**，只在下一次需要解析酷狗在线歌曲地址时参与登录态候选选择。当前已经在播放的 URI、队列顺序、位置、`playWhenReady`、本地/演示来源和既有“一次地址刷新”规则均不变。

- **迁移事实**：PC 固定七档顺序为 `128 < 320 < flac < high < viper_atmos < viper_clear < viper_tape`，默认 `128`，并从用户档位向低档尝试；见 `../MoeKoeMusic/src/config/settings.js:148-163` 与 `../MoeKoeMusic/src/components/player/songQueue/OnlineMusicQueue.js:4-19,51-69`，固定提交 `52c9833afe2e7fedcba8d5b23ff8d1f9731af73a`、GPL-2.0-only。
- **迁移事实**：旧 Mobile 对在线歌曲始终以 `free_part: 1` 请求地址，并把无 URL 的 `status=3` 映射为无版权、其余映射为 VIP；见 `../MoeKoeMusic-Mobile/src/features/player/song-url.ts:27-62`，固定提交 `ab71195d4cf3297332490fd37704d1ae8973d4c5`、GPL-2.0-only。它没有把默认质量偏好接入移动播放器。
- **迁移事实**：固定 `KuGouMusicApi@6efe84e1971c15b11a5cf1a210c5e8e0cc9d7ddb`（MIT）的 `song_url` 有质量参数，`privilege_lite` 返回基础及 `relate_goods` 候选；Android 已以同一参数形态实现于 `kugou-api/src/main/kotlin/cn/james/music/kugou/api/endpoint/KugouEndpoints.kt:155-234`，并已有七档 decoder 于 `kugou-api/src/main/kotlin/cn/james/music/kugou/api/endpoint/KugouPlaybackDecoders.kt:15-66`。
- **Android 适配决定**：偏好默认且读失败/未知值均回退 `Standard(128)`；可持久化，但匿名解析继续固定 `song_url(quality=128, freePart=true)`，不请求 `privilege_lite`，不因设置增加账号相关网络调用。这保持旧 Mobile 路径及既有阶段 4 决策。
- **新决定**：登录态只在下一次 `PlaybackSource.Kugou` 地址解析开始时读取最后成功的偏好；读取/保存偏好不重连、不中断或换流当前项。登录期间切换偏好不取消已开始的候选请求；该请求使用开始时的快照，下一次解析才读取新值。

非目标：逐曲手动质量菜单、下载/离线缓存质量、音效/响度、全局主题、额外权限/依赖/后台任务、静态 UI 假成功，以及将短期 URI 或实际质量持久化到 Room。

## 已有链路与决策

024a 已使 `AppSettings`/Repository 持有纯语义的七档 `PlaybackQualityPreference` 与独立 v1 storage value；设置行仍不可提交，见 `core/model/src/main/kotlin/cn/james/music/core/model/settings/AppSettingsModels.kt`、`data/src/main/kotlin/cn/james/music/data/settings/PreferencesAppSettingsRepository.kt` 与 `feature/settings/src/main/kotlin/cn/james/music/feature/settings/SettingsUiModels.kt`。024b 将 `:data` 的偏好映射与候选编排同 `:kugou-api` 的 `KugouPlaybackQuality`、Endpoint 和 decoder 分开；不把 core enum 或 AppSettings 放进协议模块。

后续实现采用以下最小数据流，不新增模块或依赖：

```text
Settings UI -> AppSettingsRepository (:core:model port, :data DataStore)
KugouPlaybackSourceResolver (:data) --next logged-in resolve-->
  privilege_lite + selected/lower song_url candidates (:kugou-api)
  -> typed resolved source (:playback) -> Media3 runtime state -> :app mapper -> Player UI
```

- `:feature:settings` 只写类型安全偏好和显示保存/重试状态，不依赖 `:data`、`:playback` 或协议。
- `:data` 拥有偏好 DataStore、登录态判断、候选规划与协议结果到既有 `PlaybackSourceError` 的映射；不得让 `:playback` 直接读取 DataStore。
- `:kugou-api` 保持 Endpoint、DTO 和 decoder 边界，`KugouOnlineClient` 只接收类型安全质量参数和返回类型化结果，不向 UI 泄漏 JSON、Cookie、候选 hash 或服务端文字。
- `:playback` 仍独占 Media3。实际解析到的质量是短期运行时 source-result 元数据，不属于 `PlaybackItem`、队列快照或持久 `AppSettings`。它只能经不可变播放状态和 app mapper 作为纯显示值进入 `:feature:player`。

### 登录态候选、回退与停止边界

1. **现有 session/auth 事实**：登录态判定为 `token` 非空且 `userId` 可解析为正数；现有消费实现正是 `userId?.takeIf { !token.isNullOrBlank() && it.toLongOrNull()?.let { value -> value > 0 } == true }`，见 `data/src/main/kotlin/cn/james/music/data/kugou/KugouUserProfileRepository.kt:63-64`。会话字段来自 `kugou-api/src/main/kotlin/cn/james/music/kugou/api/session/KugouSessionModels.kt:15-29`，不能通过 UI 登录标志或字符串错误推断。
2. **024b Android 适配决定**：空白 hash 在初始化会话或读取偏好前立即映射 `InvalidSource`。匿名会话不读取 AppSettings、不查 `privilege_lite`，只请求一次 `Standard(128)`、`freePart=true` 的 `song_url`；这保持旧 Mobile 行为。已登录会话在每次 resolve 的候选请求开始前只读取一次偏好快照，再调用一次 `privilege_lite`。
3. **迁移事实 + Android 适配决定**：PC 按所选档位建立向低档候选链；024b 复用 decoder 对 `level=0`、空 hash、未知质量的过滤和同质量去重，按“用户偏好到 128”的降序筛选已有候选。若该链中没有任何候选，才以原歌曲 hash 构造完整降级链；这是把 PC 回退规则收束到 Android 已有类型化协议边界，而非新增档位。
4. **迁移事实 + Android 适配决定**：PC 会跳过 `mp4` 或没有 URL 的候选再尝试下一档。Android 当前 `KugouPlaybackAddressDecoder` 先把 HTTP URL 升级为 HTTPS、再过滤非安全 URL；若原始 URL 为空或过滤后为空，只会映射为 `Unavailable(NoCopyright|VipRequired)`，不能区分其原始原因，见 `kugou-api/src/main/kotlin/cn/james/music/kugou/api/endpoint/KugouPlaybackDecoders.kt`。024b 因而只将成功结果中的 `extension=mp4` 与 decoder 已归类的 `VipRequired` 视为当前候选可继续降级；第一个非 mp4 的安全 URI 成功即停止。若链耗尽且至少一个结果为 VIP，稳定返回 `VipRequired`；只有全为 mp4（或无可用成功地址）才返回 `Protocol`。
5. **Android 新决定**：`NoCopyright` 立即终止，不以低档重试掩盖版权反馈；`VerificationRequired` 与 `AuthenticationRequired` 立即终止，不降级以绕过风控或已失效登录；候选请求或 `song_url` 的离线、超时、连接、服务和协议问题保持原类型并立即终止，不把临时网络问题误判为“换低音质即可”。这些是 Android 的类型化失败恢复边界，不声称为 PC 原样行为。
6. **Android 适配决定**：`CancellationException` 原样抛出；每次地址解析以现有 item/source-refresh 生命周期代际隔离，旧候选、旧 URL 或旧质量不得替换新歌曲、已重试地址或已切换的会话。既有 Media3 HTTP/IO 一次地址刷新仍只重新解析当前项，且会读取刷新开始时的最新成功偏好；不增加第二套刷新或无限候选重试。

### 实际质量的显示真值

PC 会保存 `resolvedQuality` 与候选 hash；见 `../MoeKoeMusic/src/components/player/songQueue/OnlineMusicQueue.js:196-217`。Android 目前 `RemotePlaybackSourceResult.Resolved` 只携带 URL，且全屏角标硬编码“标准”；见 `playback/src/main/kotlin/cn/james/music/playback/PlaybackModels.kt:76-84` 与 `feature/player/src/main/kotlin/cn/james/music/feature/player/PlayerControls.kt:257-271`。

因此后续实现必须把 `resolvedQuality` 添加到短期、类型安全的 resolved-source/runtime-state 边界，并使 Player 仅在该值存在时显示对应标签。不得把偏好值直接当作当前播放质量，亦不得在 High/无损已成功时继续显示“标准”；本地、演示、未解析和错误状态显示无质量角标而不是猜测值。

## Android 约束、失败恢复与视觉准入

- 保持现有 HTTPS、签名、会话加密和幂等读取有限重试；不新增 Endpoint、权限、DataStore 文件、Room 表或网络日志。`privilege_lite` 与 `song_url` 已是既有 Endpoint，首次实现仍须在离线 fixture 通过后单独受控真实兼容验证，且不得记录 URL、Cookie、token、候选 hash 或正文。
- 设置读取失败安全回退 128；保存失败回滚最后持久值并精确重试最后失败的质量选择。质量写入拥有独立 job/generation，不取消主题、自动跳过、动态色、歌词附加文本或字号写入。
- `09-settings.png` 已确认“默认音质”行和现有 `MoeDialog`/RadioButton 是可复用视觉语言，但尚未确认具体 Android 选项文案与 Dialog 状态。实现前必须用本审计七档产品事实登记设置 dialog 的 state/contract、语义、正常/保存失败状态及截图；不得放宽 `settings.content.light` 阈值或把真实行改动遮罩。实际质量角标变化同时更新 Player contract/evidence，只有设计符合度通过后才更新受影响 golden。

## 原子实施顺序与测试矩阵

1. 领域：`PlaybackQualityPreference`、`AppSettings`、Repository setter、独立 v1 string key；已完成。core 只定义七档语义顺序；data 私有地映射稳定 storage value。default/round-trip/unknown/read/write/cancellation、每档 raw storage value 及与既有偏好互不覆盖均由 core/data JVM 测试覆盖；尚未开放设置 UI 或消费解析偏好。协议字符串到 `KugouPlaybackQuality` 的映射留给下一协议切片。
2. 协议与数据：类型化候选计划、client 质量参数、登录/匿名分支、逐档回退、停止边界、取消和迟到结果；已完成，并以固定虚构 DTO/Transport 覆盖七档顺序、匿名、候选、VIP/mp4、停止错误、取消和偏好快照；未碰真实服务。
3. 播放：resolved-source/runtime-state 只携带实际质量；覆盖地址刷新后质量替换、不持久化 URI/质量及本地/演示不展示角标。
4. 设置：真实选择行、复用确认 Dialog、独立保存代际、回滚和精确 Retry；在写入中仅禁用该行/Dialog。
5. UI/证据：Settings dialog/语义、Player 实际标签/无标签状态、现有浅深/AMOLED/大字体截图与定向 fidelity；再进行受控真实登录/匿名地址解析和指定真机播放验证。

JVM 必测：七档顺序与未知值；匿名不查候选且始终 128；登录优先档/缺候选/逐级回退；VIP、无版权、风控、登录失效、离线、超时、连接、服务、协议；`mp4`、空/过滤后 URL 映射到既有 `Unavailable` 的行为；取消、A→B 切歌迟到、地址刷新；偏好并发写入和当前流不换流。Compose/截图必测：选择语义、保存中禁用、失败回滚/Retry，以及 badge 只反映 resolved quality。设备与真实服务均留给实现后验收，本审计未运行。

## 准入结论

产品语义、协议基础、模块所有权、失败恢复、显示真值和测试矩阵均无关键待定项。该审计允许后续“默认音质真实消费者”按上述五个原子步骤实施；缓存、语言和淡入淡出仍不随本切片开启。
