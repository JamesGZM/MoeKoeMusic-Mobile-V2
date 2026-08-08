---
name: moekoe-visual-qa
description: 对照已确认设计稿和实现回归基线验收 MoeKoe Compose UI。处理 UI 复核、截图变更、设计符合度、叠加图、差异图、锚点、视觉债务、PreviewTest 产物，或准备宣布页面、组件、Dialog、Sheet、覆盖层和视觉修复完成时使用。
---

# MoeKoe 视觉验收

## 快速循环

1. 读取目标状态的 `docs/design/contracts/*.properties`；缺少 contract 时先调用 `$moekoe-design-contract`。
2. 运行受影响模块的 `validateDebugScreenshotTest`，只把 `build/outputs/screenshotTest-results/preview/debug/rendered/` 视为当前实现。
3. 运行 `./gradlew generateUiEvidence -Pmoekoe.uiContract=<id>` 生成并排、50% 叠加、差异图和结构化结果。
4. 读取全部产物并运行 `./gradlew verifyUiFidelity -Pmoekoe.uiContract=<id>`；超过锚点、累计漂移、边界或哈希阈值即失败。
5. 设计符合度通过后才能更新 Compose reference screenshot，再运行实现回归验证。

## 判定

- 设计符合度与实现回归是两种证据；reference screenshot 通过不能替代设计对比。
- 自动结果负责哈希、尺寸、裁切、遮罩和已登记锚点；人工复核负责字体栅格、阴影衰减、图标路径、摄影裁切和整体视觉节奏。
- 真机只验证 Layoutlib 不能证明的 Insets、IME、触控、厂商系统栏和真实系统覆盖层，不为普通几何微调反复截图。
- 失败时先按 [`references/failure-triage.md`](references/failure-triage.md) 分类并修实现；不得先放宽阈值、扩大遮罩或重录 reference。
- 历史债务只能由 contract 明确声明。范围被触碰或到期时必须转为失败，不能续期掩盖同一偏差。

## 完成输出

- 报告 contract ID、截图命令、设计符合度结果、回归结果、证据路径和人工复核结论。
- 明确未执行的主题、字体、宽屏、真机和行为验证，不能把它们描述为通过。
- UI 完成前调用 `$moekoe-validate-change`，并由 `verifyUiGoldenChange` 检查 reference 更新证据。
