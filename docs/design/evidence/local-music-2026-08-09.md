# 本地音乐与设备扫描设计确认

状态：已确认。确认日期：2026-08-09。

## 已确认设计源

- `docs/design/mockups/local-music-v2/01-library-content.png`：本地库内容态；Toolbar 右侧仅保留无文字导入图标。
- `docs/design/mockups/local-music-v2/02-import-permission.png`：未授予媒体权限的全页解释态。
- `docs/design/mockups/local-music-v2/03-import-scanning.png`：授权后自动进入的逐条扫描只读态。
- `docs/design/mockups/local-music-v2/04-import-selection.png`：扫描完成后的多选导入态。

## 状态机真值

```text
PermissionRequired
        │ 授权成功
        ▼
Scanning(read-only, candidates append one by one)
        │ scan completes
        ▼
Selection(candidates selectable)
        │ import selected
        ▼
Importing → library/result feedback
```

- `Scanning` 与 `Selection` 互斥。扫描期间不得显示 Checkbox、全选、选择计数或导入按钮，也不得用禁用控件占位。
- 权限成功回调必须直接启动扫描，不提供第二个“扫描”按钮。
- 扫描结果由 Repository 冷 `Flow` 逐条发射；页面在每次发射后追加一个稳定 ID 的候选项。禁止先得到完整列表后仅用动画伪装扫描。
- 扫描完成后一次性切换到 `Selection`，此时 Checkbox 通过通用音乐 Item 的尾部参数出现。
- 权限拒绝停留在 `PermissionRequired`；扫描异常进入独立错误状态，不暴露半可用选择操作。

## 通用音乐 Item

本地库、设备扫描、搜索、歌单和队列的歌曲视觉母体统一为 `MoeSongRow`。页面只能通过封面、标题、副标题、元数据、播放态、徽标与尾部操作参数表达差异；不得重新实现歌曲行布局。设备扫描只读态尾部为空，完成态尾部为 Checkbox。

## 适配与验证

- 基准画布约 `390 × 844dp`，浅色、`zh-CN`、`fontScale=1.0`。
- Toolbar 与底部主操作为固定区域；候选列表为唯一固有滚动区域。
- 大字体允许歌曲行增高；底部主操作不得被列表挤出视口。
- 每个结构状态提供独立 Preview，状态互斥由 `LocalMusicViewModelTest` 验证。

final result: approved
