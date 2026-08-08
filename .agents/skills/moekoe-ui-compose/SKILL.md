---
name: moekoe-ui-compose
description: 设计、实现或复核 MoeKoeMusic 用户可见 UI。涉及 Compose 页面和组件、feature/* UI、导航、Dialog、Bottom Sheet、覆盖层、视觉状态、设计稿、原型、无障碍，或 docs/design、core/designsystem、docs/UI_*.md 下的文件时使用。
---

# MoeKoe UI 与 Compose

## 读取路由

1. 先读取 `docs/UI_DESIGN_BRIEF.md`、`docs/DESIGN_SYSTEM.md`、`docs/design/mockups/README.md` 和 `docs/DEVELOPMENT_PLAN.md` 的页面映射。
2. 只读取任务相关设计资产；覆盖层/反馈再读 `docs/UI_COMPONENTS.md`，复杂交互再读相应 prototype README，确认每张资产是“候选 / 已确认 / 已废弃”。
3. 编码前调用 `$moekoe-design-contract` 并读取目标状态 contract；涉及模块、状态所有权或导航时同时读取 `docs/ARCHITECTURE.md` 并调用 `$moekoe-architecture`。

## 设计门禁

- 列出正常、加载、空数据、错误、离线、权限、未登录、账号受限、取消和恢复等适用状态，并逐项关联设计图。
- 缺少状态图时，先使用 `frontend-design` 在既有 MoeKoe Air 视觉内生成静态候选图；获得用户明确确认前不得制作原型或编码布局。
- 同页新状态必须复用已确认完整页面基座。Dialog 和 Bottom Sheet 使用“原页面画布 → 规范遮罩 → 独立前景层”确定性合成，不重新生成底层页面。
- 原型只验证已确认设计的滚动、转场、手势和响应式行为；原型图标、示例文案和临时装饰不是实现真值。

## Compose 实现

- Route 获取 ViewModel 并收集状态；Screen 接收不可变状态和事件，不接收整个 ViewModel 或自行导航。
- Token 来自 `:core:designsystem`；使用固定 Material Icons 或已批准资源，提供可访问性语义、稳定列表 key，并隔离高频状态更新。
- 发现未设计状态时停止布局实现，先补设计并重新确认。
- 设计确认后同步 Mockup 索引、唯一页面映射、阶段计划和 Design System；实现后调用 `$moekoe-visual-qa`，完成前调用 `$moekoe-validate-change`。

## 多状态视觉落地门禁

- 编码前按确认稿建立“状态 → 文案 → typography role → 图标/资源 → 固定锚点 → 可变区域”矩阵；默认态不能替代加载、成功、错误、过期和恢复态的逐图审查。
- 相同语义层级的瞬时成功与错误反馈必须复用同一个组件、字号、行高和固定占位；状态切换不得推动主操作。长文案若超出已确认内联区域，必须使用已确认 Dialog/Sheet/独立状态，不能任意撑高页面。
- 设计稿中的品牌图标、返回图标和状态图形必须使用批准资源或按测量值绘制；Material Icons 和字符占位只有视觉一致且经叠加确认后才能采用。
- `48dp` 最小触控区域与可见图形分别建模；扩大点击区不能改变图标粗细、尺寸或视觉中心。
- 页面以等比设计画布作为最小内容高度，窗口只作为视口。禁止用 `maxHeight` 反向缩短 Card、Hero 或间距；仅当设计画布、字体重排或 IME 后的内容真实超过视口时启用唯一外层滚动。
- Compose Preview 至少覆盖每张确认状态图，便于桌面调整；Preview 是快速反馈，不替代设计稿归一化叠加、截图回归和真机 Insets/触控验证。
- 摄影、插画和人物资产默认保持原始宽高比；禁止使用 `FillBounds` 强行填满不同宽高比容器。若设计框比资产高，必须明确登记裁切、对齐或背景延续方式，并单独核对人物比例。
- 方向图标若与 Material 图标在线宽、端点、路径或视觉中心上不一致，必须先跨确认稿审计复用语义：同一路径进入 `:core:designsystem`，以产品前缀 + 动作语义命名 SVG/VectorDrawable，并统一自动 RTL；只有路径确实属于单一页面时才留在 Feature。`48dp` 触控区、圆形底板和矢量图形分别登记，图形复用不等于强行复用整套按钮组合。
- 设计稿中的短分隔线、连接线和装饰线必须记录实际端点或长度；禁止用 `weight`、`fillMaxWidth` 把固定装饰自动拉满。悬浮 Surface 必须同时记录顶部覆盖锚点、底部边界和画布底部间距，并检查应用壳是否以空 BottomBar、MiniPlayer 或 Insets Padding 截短页面视口。
- 页面几何微调以 Compose Preview 为第一反馈回路；系统栏场景增加显式 Insets Preview 参数，不依赖每轮 ADB 截图。真机只验证 Preview 无法证明的真实 Insets、IME、触控和厂商系统栏。Hero 与悬浮 Surface 的重叠必须让底层资产实际绘制到覆盖区之后，不能只让两个边界相接后声称已经覆盖。
- 带滑动选中块的 Tab/Segmented Control 必须把控件本体放在跨状态稳定的组合位置，页面分支只替换下方内容；禁止在每个分支复制一套 selector，导致 `animate*AsState` 随分支重建而失效。行为测试应冻结动画时钟并验证选中块的中间位置。
- 同一路由内的 Tab/Segmented Control 默认只切换可见分支，不得因此重建整份业务状态或取消页面级任务。输入草稿、分支反馈、选择上下文和密码可见性应按分支隔离并在往返后恢复；倒计时、轮询和会话绑定 Route/ViewModel 生命周期，切走后继续或按已确认规则暂停，返回时不得隐式重置。至少用一个往返测试验证状态保留、反馈不串页、计时不重置和会话不重复创建。
- 设计稿若使用带品牌色的柔光阴影，必须分别测量页面底色、Surface 色、边框、阴影颜色和边缘衰减；不得仅凭 `Surface.shadowElevation` 生成默认黑灰阴影。颜色、透明度和 elevation 固化为页面或 Design System Token，并在浅色真机像素中复核。
