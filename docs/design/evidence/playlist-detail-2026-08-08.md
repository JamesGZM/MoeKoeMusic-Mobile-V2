# 歌单详情设计符合度证据

- 确认稿：`docs/design/mockups/05-playlist-detail.png`
- 实现截图：`feature/playlist/src/screenshotTestDebug/reference/cn/james/music/feature/playlist/PlaylistDetailScreenshotTestKt/PlaylistDetailContentLightScreenshot_ContentLight_5bbd60bc_0.png`
- 比较范围：确认稿顶部至应用级 MiniPlayer 上边界 `853 × 1672px`，按宽度归一化为实现截图 `1024 × 2006px`；MiniPlayer、系统状态栏与导航栏由 App 真机测试独立验收。

## 证据文件

- `playlist-detail-2026-08-08-side-by-side.png`
- `playlist-detail-2026-08-08-overlay.png`
- `playlist-detail-2026-08-08-diff.png`

## 结论

- 歌单封面与资料区、播放全部、随机播放、收藏、下载、更多、歌曲标题、排序入口、紧凑歌曲行和当前项 Surface 的锚点、比例与首屏密度已经同画布复核。
- 页面 Toolbar 使用已经确认的 `10-user-profile` / `15-toolbar-navigation` 全局居中样式；没有复制 `05-playlist-detail.png` 旧稿中偏小的标题，这是已确认公共规范对单页旧稿的有意覆盖。
- Light、Dark、AMOLED、Loading、Empty、Error、`1.5×`、`2.0×` 共 8 组截图基线已建立；大字体时资料区纵向重排，操作区分行，列表仍保持单一纵向滚动容器。
- 指定 ELE-AL00 / API 29 真机已从发现页进入歌单详情：一级底栏隐藏、MiniPlayer 保留、五个操作完整可见且无横向裁切，系统返回恢复发现页来源位置。
- 当前内容和本地图片仅是确认稿 UI 预览；未接入歌单协议、收藏、下载、排序或播放业务，也不以示例 ID、Hash 或状态冒充功能完成。
- 保留的 P3 差异为原创图片内容、平台字体字形与 Material 图标笔画；槽位比例、裁切、信息层级和色彩语义符合确认稿，不阻塞本纯 UI 切片。
