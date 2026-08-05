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
