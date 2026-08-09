# 数据驱动 UI 架构迁移计划

状态：Accepted（仅 `MoeSongRow` 生产迁移后置）。创建日期：2026-08-09。

## 目标

在不改变任何已确认 UI 像素、交互、导航或播放行为的前提下，建立不可变 Feature UI Model、mapper 与 typed action，并直接迁移已批准的 MiniPlayer、Snackbar/AlertDialog、Standard/Search TopBar 生产接口和消费者。只有 `MoeSongRow` 的最终 API 和 legacy consumer 一次迁移属于后续独立 UI 阶段。

完整边界、legacy 冻结、图片 renderer 例外、十项所有权和 trailer 规则见 [`../reference-audits/23-data-driven-ui-architecture.md`](../reference-audits/23-data-driven-ui-architecture.md)。本阶段不授权设计稿、契约、Golden、mask、截图或生产视觉变更；第 8 至 10 步是唯一获准的公共组件 API 迁移。

## 用户批准的十步顺序

1. 父子执行权治理 incident/eval：已批准且已完成的 `parent-mutation-boundary` 六个 assertion ID，作为本阶段执行权边界；不另建组件视觉 incident/eval。
2. 审计、目录与迁移清单：仅更新授权的架构/计划/目录/索引文档。
3. Build Logic 门禁：在独立授权下建立 SongRow legacy 冻结与 data-component 参数门禁，阻止新增 style/Slot/视觉覆写和禁止的裸视觉参数。
4. Search/Home UI Model：建立 `SearchSongUiModel`、Home UI Model、mapper 与 typed action；移除 Search 的 `songs + decorator Maps`，生产 Home UI Model 移除 `preview*`，fixture 留 screenshot/debug source set；继续调用冻结 legacy wrapper。
5. LocalMusic/DeviceScan UI Model：将映射、排序、过滤、格式化移出 Composable；只读/多选继续调用冻结 legacy wrapper。
6. Queue/App 边界：`:app` 映射 `PlaybackState`，`feature:player` 只接收 Queue UI Model/typed action，不接收 `PlaybackItem` 或路径；继续调用冻结 legacy wrapper。
7. Settings/My/Discover Feature 模型：建立 sealed `Row`/`Entry`/`Group`、稳定 id 与 typed action，移除平行 nullable 计数、selected index 和重复业务列表；保留 Feature 布局。
8. MiniPlayer：`:app` 把 `PlaybackState` 映射为 `MoeMiniPlayerUiModel`，直接实现 `MoeMiniPlayerEvent` 并一次迁移组件和消费者；播放归 `:playback`。
9. Snackbar/AlertDialog：直接实现 `MoeSnackbarUiModel/Event`、`MoeAlertDialogUiModel/Event` 并一次迁移所有消费者；复杂风险 Dialog 保持 Feature 组合。
10. Standard/Search TopBar：直接实现受控 action data/action ID 并一次迁移所有消费者，保留确认的 Tencent Close 图形。

完成第十步后，才可另行申请独立 UI 阶段，用一次性迁移实现 `MoeSongRow` 的最终数据模型、固有 `More(id)` 和所有 legacy consumer 替换。无 badge 自然不显示的语义属于该后续阶段的目标，不是本阶段生产变更。

## 各切片共同完成标准

- Feature mapper 不泄漏 DTO、Entity、Repository、Service、`PlaybackState`、`Dp`、`Shape`、`Color`、内部 Padding 或任意视觉 Slot。
- 图片只通过固定容器内 renderer 填充像素；不新增图片加载依赖或让 renderer 获得布局控制权。
- 当前阶段冻结所有 `MoeSongRow` 调用、style、Slot、视觉覆写与 API 增长；Screen 继续调用 legacy wrapper，现有 Compact/Comfortable/Queue 等像素完全不变。MiniPlayer、Snackbar/AlertDialog、Standard/Search TopBar 按第 8 至 10 步直接生产迁移，仍须像素不变。
- 禁止更新设计图、设计 contract、Golden/reference、mask、probe 或视觉阈值。任何截图差异都是回归，除非用户单独批准视觉切片。
- 每个实现提交使用 `Implemented-By-Agent: /root/<child-task>`、`Committed-By-Agent: /root` 并记录实际验证命令；trailer 仅用于追溯，不能证明密码学身份或验收。
- 后续实际代码切片执行受影响 mapper/Feature 测试，再执行 `verifyArchitecture`、`verifyUiContracts`、`verifyUiImpact`、相应 `verifyUiFidelity`、`verifyUiGoldenChange`、截图测试、Lint 和 Debug 构建；如未执行须如实记录。

## 非目标

- 不改变 `:playback` 对 ExoPlayer、队列运行态和 `PlaybackState` 的所有权。
- 不让 Design System 访问 Repository、数据库、DataStore、Service、网络、文件或导航。
- 不建立万能 Card、Toolbar、歌曲行 Slot 或为推测性消费者预留 Boolean/变体。
- 不将登录 Hero/Card、二维码、风险状态机、发现页专属卡片或单页布局搬入公共组件。
