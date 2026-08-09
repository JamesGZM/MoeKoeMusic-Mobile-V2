# Core 页面局部视觉误通过评测（2026-08-09）

## 原始误通过证据

- 本地音乐：`local-music.content.light` 在歌曲行、播放态、导入图标和纵向位置明显偏离确认稿时仍为 `PASS`，`meanError=0.07873`、`changedRatio=0.15351`；contract 只有搜索框和列表两个顶部中心点，固定误差阈值为 `8`、累计阈值为 `12`。
- 搜索：`search.content.light` 在输入文字以及返回、清除、麦克风图形明显过大时仍为 `PASS`，`meanError=0.04870`、`changedRatio=0.10029`；toolbar 只验证外层顶部中心，没有文字可见边界或图标区域。
- 首页：旧 contract 使用 `fixed=8`、`cumulativeY=24`，而已确认规格要求 `2/3`。恢复 `2/3` 后，旧实现立即因区块误差 `3.08…7.19`、累计漂移 `11.25` 失败。
- 搜索长文本：原 Content fixture 只覆盖短查询，输入框整体锚点通过时，长查询曾把返回、输入框、清除和语音操作整体挤出画布；新增 `SearchLongQueryClearScreenshot` 后才稳定复现并拦截。
- 设置 Switch：`SettingsItem` 曾把所有 Switch 固定为 `enabled=false`，因此 `checked=true` 仍显示灰色；截图只证明分组位置，没有验证可交互开启态的主题色和 `checked/enabled` 组合。

可复核左右图位于：

- `build/reports/ui-evidence/local-music.content.light/side-by-side.png`
- `build/reports/ui-evidence/search.content.light/side-by-side.png`
- `build/reports/ui-evidence/home.content.light/side-by-side.png`

## 根因与候选评测

区域比较能力已在 `151247b` 后正式存在，但页面 contract 没有最低覆盖要求，因此关键固定文字和重复 item 可以完全没有 region。现有 probe 又只在任意容器顶部中心画一个 `8px` 色块，不能证明搜索文字没有裁切，也不能证明歌曲行的封面、文字和尾部图形边界正确。仅有短查询 fixture 也无法对输入槽位施压；仅有 checked 值则无法证明 Switch 是可交互的真实开启态。本地音乐的 `list` 锚点还把设计封面顶部坐标用于实现行容器顶部，测量对象并不一致。

候选评测新增两条客观断言：长查询与尾部清除操作必须同屏且不裁切；业务开启态必须同时满足 `checked=true`、`enabled=true` 和主题强调色，尚未开放能力必须展示明确占位文案，不能伪装成禁用开关。

候选规则需要让以上历史版本失败，并保持以下反例：动态封面内容仍可 mask；页面专属布局不因此进入公共组件；全页失败不能被局部通过覆盖；未获人工批准前不接入正式 Skill 或全局构建门禁。
