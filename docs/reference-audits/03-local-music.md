# 阶段 3：本地音乐参考审计

状态：Accepted。审计日期：2026-08-05。

## Kreate

- 固定提交：`f02577e862318df26b13abb02d14d8d824a1b947`；GPL-3.0，仅学习思想。
- 文件：`me/knighthat/utils/OnDeviceMedia.kt`、`app/kreate/android/themed/rimusic/screen/home/onDevice/OnDeviceSongs.kt`。
- 采用：MediaStore 投影查询、版本化权限、稳定媒体 ID、列表排序。
- 不采用：`DATA` 绝对路径、全局异步作用域、非空断言和直接播放原始 URI。

## Metrolist

- 固定提交：`289ed45d0a429e6e4f19a2a07543b243a0ab2e8b`；GPL-3.0，仅学习思想。
- 文件：`app/src/main/kotlin/com/metrolist/music/ui/screens/library/LibrarySongsScreen.kt`。
- 采用：Compose Activity Result、多文件选择和窄范围 URI 授权。
- 不采用：将完整音频 `readBytes()` 载入内存；本项目始终流式复制。

## OuterTune

- 固定提交：`e82794f3a059ad30f7c1bfda52051b03e718d614`；GPL-3.0，仅学习思想。
- 文件：`LocalMediaScanner.kt`、`MediaStoreExtractor.kt`、`MetadataScanner.kt`。
- 采用：扫描与元数据提取分层、异常文件隔离。
- 不采用：绝对路径、FFmpeg 依赖、复杂数据库耦合和直接管理系统媒体。

## Symphony

- 固定提交：`dd04b872b8b4e6dd56172c053a5776c4d56ad080`；AGPL-3.0，仅学习思想。
- 文件：`services/database/store/ArtworkCacheStore.kt` 及本地歌曲模型。
- 采用：内嵌封面独立存储、限制解码尺寸和缺失封面回退。
- 不采用：任何具体代码、AGPL 实现和与本项目数据模型绑定的缓存接口。

## 独立实现结论

上述项目均未提供“外部打开 = 流式复制 + 原子提交 + Room 成功后播放”的完整语义。本项目依据 Android SAF、MediaStore、WorkManager 和 Media3 官方约束独立实现；领域层不暴露 URI，原始 URI 永不成为长期播放来源。

## 逐条扫描 UI 决策补充（2026-08-09）

- Android MediaStore `Cursor` 天然支持逐行遍历；Repository 用冷 `Flow<DeviceAudioCandidate>` 在 IO dispatcher 上逐条发射，Feature 只追加领域候选，不接触 `Cursor`、`ContentResolver` 或 URI。
- Kreate 的 MediaStore 投影与版本化权限继续作为扫描字段和权限分支参考；OuterTune 的扫描/元数据分层继续作为异常项隔离参考。两者均只学习思想，不复制 GPL-3.0 代码。
- 拒绝保留 `suspend fun scanDevice(): List<...>` 后再用动画逐条展示：这会把已完成查询伪装成仍在扫描，并让取消、失败和完成边界失真。
- 拒绝扫描期间开放多选：不断增长的候选集合会让全选语义和导入边界不稳定。扫描态严格只读，收集正常完成后才进入选择态。
- 不新增依赖、权限、存储表或后台任务；复制导入与外部 Intent 事务保持不变。
