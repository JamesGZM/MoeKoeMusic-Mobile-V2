---
name: moekoe-design-contract
description: 将已确认的 MoeKoe UI 设计转成可实现、可机器校验的契约。新增或实质修改页面、状态、Dialog、Bottom Sheet、覆盖层、自适应布局、截图基线或 docs/design/contracts 文件时，在 Compose 编码前使用。
---

# MoeKoe 设计复刻契约

## 工作流

1. 读取 `docs/design/mockups/README.md`、任务相关设计稿、页面 Layout Spec、Design System 和状态组件规范，确认资产是已确认基线。
2. 列出全部适用状态，区分结构设计基线与功能状态规则；不要求启用、禁用、加载、错误等每个状态单独出图。
3. 每个页面、Dialog 或 Sheet 至少绑定一个已确认结构设计源和结构 `@PreviewTest`；其余状态绑定固定 fixture、行为/组件测试和稳定状态 ID。
4. 按 [`references/contract-fields.md`](references/contract-fields.md) 登记画布、视口、主题、字体倍率、裁切、固定锚点、动态区域、状态规则、滚动和适配行为。
5. 计算设计源 SHA-256。设计结构或阈值变化必须重新确认；功能状态变化按产品规格、状态机和 Design System 评审，不反向要求设计图表达所有状态。
6. 运行 `./gradlew verifyUiContracts`；缺字段、路径无效、哈希漂移、状态规则无测试或未批准设计必须停止 Compose 实现。

## 硬门禁

- 候选、废弃、原型或历史横向总览图不得登记为正式实现源；已确认但仍位于历史目录的资产必须明确记录其正式资格。
- 设计坐标按当前 Composable 可用宽度使用唯一等比系数；禁止逐组件经验缩放、横纵分别拉伸或按设备型号分支。
- Dialog/Sheet contract 分开登记底层页面、遮罩和前景 Surface；不得重新生成或任意替换底层页面。
- 动态文案、输入值、组件状态、系统内容和派生插画必须分类登记；只豁免对应像素内容，组件边界、位置和固定间距仍需锚点验证。
- 通用 mask 只用于系统不可控内容；Card、Toolbar、Selector、操作区和固定间距不得遮罩。
- 标准视口声明零滚动时必须提供零滚动行为断言；条件滚动必须记录触发、边界和复位。
- 新页面和新状态不得使用历史债务；迁移债务必须有范围、原因、负责人和到期日，触碰范围后立即失效。

## 输出

- 在 `docs/design/contracts/` 创建或更新单状态 `.properties` contract。
- 报告设计源、状态 ID、固定/可变区域、阈值、未覆盖状态和阻塞项。
- 完成 contract 后调用 `$moekoe-ui-compose` 实现；视觉交付前调用 `$moekoe-visual-qa`。
