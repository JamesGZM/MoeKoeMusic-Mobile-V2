---
name: moekoe-ui-compose
description: Design, implement, or review MoeKoeMusic user-visible UI. Use for Compose screens and components, feature/* UI, navigation, dialogs, bottom sheets, overlays, visual states, mockups, prototypes, accessibility, or files under docs/design, core/designsystem, and docs/UI_*.md.
---

# MoeKoe UI 与 Compose

## 读取路由

1. 先读取 `docs/UI_DESIGN_BRIEF.md`、`docs/DESIGN_SYSTEM.md`、`docs/design/mockups/README.md` 和 `docs/DEVELOPMENT_PLAN.md` 的页面映射。
2. 只读取任务相关设计资产；覆盖层/反馈再读 `docs/UI_COMPONENTS.md`，复杂交互再读相应 prototype README，确认每张资产是“候选 / 已确认 / 已废弃”。
3. 涉及模块、状态所有权或导航时同时读取 `docs/ARCHITECTURE.md` 并调用 `$moekoe-architecture`。

## 设计门禁

- 列出正常、加载、空数据、错误、离线、权限、未登录、账号受限、取消和恢复等适用状态，并逐项关联设计图。
- 缺少状态图时，先使用 `frontend-design` 在既有 MoeKoe Air 视觉内生成静态候选图；获得用户明确确认前不得制作原型或编码布局。
- 同页新状态必须复用已确认完整页面基座。Dialog 和 Bottom Sheet 使用“原页面画布 → 规范遮罩 → 独立前景层”确定性合成，不重新生成底层页面。
- 原型只验证已确认设计的滚动、转场、手势和响应式行为；原型图标、示例文案和临时装饰不是实现真值。

## Compose 实现

- Route 获取 ViewModel 并收集状态；Screen 接收不可变状态和事件，不接收整个 ViewModel 或自行导航。
- Token 来自 `:core:designsystem`；使用固定 Material Icons 或已批准资源，提供可访问性语义、稳定列表 key，并隔离高频状态更新。
- 发现未设计状态时停止布局实现，先补设计并重新确认。
- 设计确认后同步 Mockup 索引、唯一页面映射、阶段计划和 Design System；完成前调用 `$moekoe-validate-change`。
