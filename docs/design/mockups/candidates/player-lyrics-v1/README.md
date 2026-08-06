# 歌词页状态候选 v1

状态：页面结构与状态表达已于 2026-08-06 确认；实际色板按当前歌曲封面动态派生。

本组设计严格复用已确认的 [`07-player-lyrics.png`](../../07-player-lyrics.png) 完整页面画布，只清理并替换歌词内容区域。顶部导航、歌词调节入口、分页点、歌曲信息、进度和播放控制均来自同一基座，不由生成工具重新解释。

每个状态独立输出为 `853 × 1844` PNG：

- `23a-lyrics-loading.png`：首次进入歌词页的内容区加载态；
- `23b-lyrics-empty.png`：无候选、非酷狗来源的内联空态；
- `23c-lyrics-offline.png`：离线且无成功缓存；
- `23d-lyrics-error.png`：超时、连接、服务或协议失败；
- `23e-lyrics-original-only.png`：只有原文；
- `23f-lyrics-phonetic.png`：原文加音译；
- `23g-lyrics-font-150.png`：`1.5×` 字体密度；
- `23h-lyrics-font-200.png`：`2.0×` 字体密度。

已确认的 `07-player-lyrics.png` 继续作为“原文加翻译”内容态，不重复生成。离线但缓存命中也直接复用内容态，只在交互实现中提供弱提示，不改变歌词主体。

本目录的 HTML/CSS/JS 只用于可复现地生成静态设计图，不是交互原型，也不进入 Android 构建或运行时。确认结论锁定布局、密度和各状态的内容层级；图中的蓝紫背景、高亮和播放按钮颜色只是示例。Compose 必须按 [`13-player-artwork-palette.md`](../../../../reference-audits/13-player-artwork-palette.md) 复用当前歌曲封面派生的 `PlayerPalette`，不得把示例色写死，也不得为歌词页建立独立取色器。
