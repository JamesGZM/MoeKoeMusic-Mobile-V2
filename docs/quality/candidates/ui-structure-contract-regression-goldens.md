# 多状态回归 Golden 映射候选

状态：Adopted。用户于 2026-08-08 明确批准，正式门禁实现见 `7630e66`。

## 原始失败

`user-profile.content.light` 的结构 fidelity 与截图回归均通过，但 `verifyUiGoldenChange` 拒绝 Dark、AMOLED、`1.5×`、`2.0×`、Empty、Offline、长文本和宽屏 reference，因为正式门禁只能把 `screenshot.golden` 与 `probe.golden` 映射回 contract。

## 候选行为

- 在单一结构 contract 中增加 `regression.golden.<state>=<repository-relative-path>`。
- `verifyUiContracts` 校验每个登记路径位于仓库内且文件存在。
- `verifyUiGoldenChange` 将这些路径映射到所属结构 contract；reference 变化时仍要求该 contract 当轮 `result.properties` 的 `passed=true`。
- 未登记 reference、重复映射、路径越界和缺少当轮 fidelity 继续失败。
- 回归 golden 不成为新的结构设计源，不单独获得像素阈值或 debt。

## 拒绝方案

- 不为 Dark、大字体或 Empty 复制指向浅色正常稿的假结构 contract。
- 不把主题差异通过 `pixel.meanError.max=1` 伪装成设计符合度。
- 不删除多状态截图，也不把生产页面在测试中改回旧几何。

## 已实施范围

- `build-logic/src/main/kotlin/UiContract.kt`
- `build-logic/src/main/kotlin/VerifyUiContractsTask.kt`
- `build-logic/src/main/kotlin/VerifyUiGoldenChangeTask.kt`
- 对应 build-logic 单元测试
- `docs/design/contracts/user-profile.content.light.properties` 的多状态 golden 映射

正式单测覆盖显式映射、未登记路径保持拒绝和跨 contract 重复映射拒绝；个人主页集成验证继续证明缺少当轮 fidelity 时不会放行。
