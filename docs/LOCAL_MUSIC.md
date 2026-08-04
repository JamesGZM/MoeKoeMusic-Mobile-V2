# 本地音乐规格

状态：Accepted。本文定义第一版本地音乐的产品语义、架构边界和验收标准。

## 产品语义

MoeKoe 的“导入”表示把用户选择的音频复制到 App 专属音乐目录并登记到本地音乐库，而不是长期引用外部文件。

- App 内文件选择：复制导入，完成后进入本地音乐。
- 设备音乐扫描：只展示候选项，用户选择后复制导入。
- `ACTION_VIEW` 打开方式：复制导入，成功后立即播放。
- `ACTION_SEND`：复制导入，完成后进入本地音乐。
- `ACTION_SEND_MULTIPLE`：批量复制导入并显示汇总，不自动播放整个批次。
- 相同内容已导入：复用现有记录；`ACTION_VIEW` 直接播放现有副本。
- 导入失败：不得临时播放外部 URI。

导入副本属于 App 数据，卸载时删除。第一版不把副本发布到系统公共 `Music` 目录；未来“导出到系统音乐库”作为独立功能设计。

## 统一管线

```text
外部 Intent / 文件选择 / MediaStore 候选项
                    │
                    ▼
          解析、授权与格式初检
                    │
                    ▼
       App 专属目录中的 .partial 文件
                    │
                    ▼
     流式复制 + SHA-256 + 元数据探测
                    │
                    ▼
        重复检查、格式验证、原子改名
                    │
                    ▼
              Room 事务提交
                    │
                    ▼
        打开本地库 / 播放导入歌曲
```

文件成功落盘但数据库失败时删除新副本；中断、取消和异常时清理 `.partial`。数据库提交前不允许播放器获得新媒体项。

## 领域与平台边界

计划中的稳定类型：

```kotlin
data class LocalImportRequest(
    val sources: List<ExternalAudioSource>,
    val source: LocalImportSource,
    val completionAction: ImportCompletionAction,
)

enum class LocalImportSource {
    DocumentPicker,
    MediaStore,
    ExternalView,
    ExternalShare,
}

enum class ImportCompletionAction {
    OpenLocalLibrary,
    PlayImportedTrack,
}
```

`ACTION_VIEW` 固定映射为 `PlayImportedTrack`。领域模型以不透明来源标识和本地媒体 ID 表达文件，不直接暴露 Android `Uri` 或绝对路径；ContentResolver、URI 授权和文件目录限制在 Android 数据源，播放 URI 由平台适配层交给 `:playback`。

`LocalMusicRepository` 提供观察本地库、扫描候选项、导入、删除、检查可用性和解析可播放来源的能力。Room 保存元数据、存储键、大小、哈希、导入来源和时间，不保存音频二进制。

## 外部入口

使用独立导出的音频入口 Activity，分别声明：

- `ACTION_VIEW` + `CATEGORY_DEFAULT` + `audio/*`。
- `ACTION_SEND` + `CATEGORY_DEFAULT` + `audio/*`。
- `ACTION_SEND_MULTIPLE` + `CATEGORY_DEFAULT` + `audio/*`。

入口 Activity 只解析、校验并转交请求，不直接访问数据库或 ExoPlayer。接受可读取的本地 `content://` URI；拒绝 HTTP、HTTPS、空 URI、目录和不可读内容。发送方的 MIME 只作为初检，最终能否导入由媒体探测和 Media3 支持能力决定。

冷启动和 `onNewIntent` 热启动必须产生等价请求。外部临时授权只用于完成复制，不能成为长期播放来源。

## 存储、权限与格式

- 副本位于 App 专属外部 Music 目录；不申请公共目录写权限。
- App 专属外部存储不可用或空间不足时终止导入并反馈。
- Android 13 及以上扫描使用 `READ_MEDIA_AUDIO`。
- Android 12 及以下扫描使用 `READ_EXTERNAL_STORAGE`，Manifest 设置 `maxSdkVersion=32`。
- 文件选择使用 Storage Access Framework，不要求完整媒体库权限。
- 不申请 `MANAGE_EXTERNAL_STORAGE`。
- 第一批验证 MP3、M4A/AAC、FLAC、Ogg/Opus 和 WAV；实际支持仍受容器、编码和设备解码器影响。

目标文件使用内部生成的稳定名称，展示名仅作为元数据，不能直接拼接为路径。复制前检查空间，复制期间计算 SHA-256，以“大小 + SHA-256”判断完全重复。

## UI 状态

“我的 → 本地音乐”提供导入文件、扫描设备音乐、排序、搜索和删除入口。正式实现前需补齐：

- 本地音乐列表与空状态。
- 设备音乐候选列表和多选。
- 单个/批量导入进度。
- 外部打开导入页。
- 成功、部分成功、重复和失败汇总。
- 权限未授予、空间不足、文件损坏、格式不支持。
- 删除确认 Dialog。

导入是持续状态，使用页面内进度；阶段结果可使用 `MoeSnackbar`。不得用 Toast 连续刷新进度。所有页面沿用已批准的浅色蓝色 Material 3 体系，并提供深色、纯黑和大字体状态。

## 删除与播放

删除本地歌曲同时删除 Room 记录和 App 副本。若歌曲位于播放队列，先协调播放器移除对应媒体项；删除当前歌曲时安全切换到下一首，无下一首则停止。文件系统或数据库只完成一侧时进入可恢复错误，不静默留下不可见孤儿文件。

## 验收场景

1. 从文件管理器选择音频并使用 MoeKoe 打开。
2. App 显示真实复制进度。
3. 文件落盘和数据库提交成功后加入队列并立即播放。
4. 歌曲出现在“我的 → 本地音乐”。
5. 删除源文件后仍能播放副本。
6. 再次打开相同内容时不复制，直接播放已有记录。
7. URI 失效、空间不足、损坏或不支持格式时给出明确错误且不播放原 URI。
8. 批量导入正确汇总成功、重复和失败数量。

测试矩阵与自动化边界见 [`TESTING_STRATEGY.md`](TESTING_STRATEGY.md)。
