# 用户主页设计符合度证据

- 确认稿：`docs/design/mockups/10-user-profile.png`
- 实现截图：`feature/profile/src/screenshotTestDebug/reference/cn/james/music/feature/profile/UserProfileScreenshotTestKt/UserProfileContentLightScreenshot_ContentLight_5bbd60bc_0.png`
- 比较范围：确认稿顶部至应用级 MiniPlayer 上边界 `853 × 1672px`，按宽度归一化为实现截图 `1024 × 2006px`；MiniPlayer 与系统安全区由 App 壳负责，不重复画入 Feature 截图。

## 证据文件

- `user-profile-2026-08-08-side-by-side.png`
- `user-profile-2026-08-08-overlay.png`
- `user-profile-2026-08-08-diff.png`

## 结论

- Toolbar、资料 Hero、头像与徽标、三项关系统计、编辑按钮、听歌概览、创建歌单标题及三行歌单的比例、顺序和首屏密度已在同一画布复核。
- 页面复用已经确认的全局居中 Toolbar；共享组件尺寸优先于稿件中的平台字体栅格差异，返回、分享与更多均保留独立 `48dp` 触控区。
- Light、Dark、AMOLED、Loading、Empty、Error、`1.5×`、`2.0×` 共 8 组截图基线已建立；大字体时 Hero 与概览纵向重排，页面继续使用单一纵向滚动容器。
- “我的”只上抛资料页事件，`:app` 注册类型安全子页面并负责返回栈、一级底栏与 MiniPlayer；`:feature:profile` 不反向依赖 `:feature:my`、数据层或播放层。
- 当前计数、签名、时长和歌单仅是确认稿 UI fixture；分享、编辑、关系和歌单操作只保留事件端口，未伪装真实业务完成。
- 保留的 P3 差异为原创图片内容、平台字体字形与 Material 图标笔画；组件边界、圆角、间距、行高和信息层级没有据此豁免。
