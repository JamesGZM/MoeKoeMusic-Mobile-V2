# 歌词页状态候选 v1

状态：待用户确认，不是 Compose 实现基线。

本组候选严格复用已确认的 [`07-player-lyrics.png`](../../07-player-lyrics.png) 完整页面画布，只清理并替换歌词内容区域。顶部导航、歌词调节入口、分页点、歌曲信息、进度和播放控制均来自同一基座，不由生成工具重新解释。

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

本目录的 HTML/CSS/JS 只用于可复现地生成静态设计图，不是交互原型，也不进入 Android 构建或运行时。用户确认本组图片前，不得据此制作新原型、更新唯一页面映射或开始歌词 Compose。
