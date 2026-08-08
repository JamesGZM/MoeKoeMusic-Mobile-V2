# 多状态回归 Golden 候选隔离评测（2026-08-08）

## 正式版本基线

- 输入：`user-profile.content.light` 结构 contract，以及同一页面因几何修复更新的 Light、Dark、AMOLED、`1.5×`、`2.0×`、Empty、Offline、长文本和宽屏 reference。
- 命令：`./gradlew :feature:profile:validateDebugScreenshotTest verifyUiFidelity verifyUiGoldenChange -Pmoekoe.uiContract=user-profile.content.light --no-configuration-cache`。
- 结果：截图回归通过，结构 fidelity 通过；`verifyUiGoldenChange` 因九张状态 reference 没有独立 contract 而失败。
- 结论：正式门禁能拒绝未登记 golden，但无法表达“一份结构真值服务多个功能状态回归”的已确认规则。

## 候选版本

- 候选规则：`regression.golden.<state>` 只扩展同一结构 contract 的回归 golden 映射；主结构设计源、probe、锚点、阈值和当轮 fidelity 要求保持不变。
- 隔离命令：`./gradlew :build-logic:test --tests 'candidates.RegressionGoldenMappingCandidateTest'`。
- 结果：3 项通过；显式登记的状态 golden 被映射，未登记 golden 保持拒绝，缺少当轮 fidelity 时保持拒绝。
- 全量命令：`./gradlew :build-logic:test`。
- 结果：通过；现有 contract、图像比较、架构与 golden 质量门禁测试未退化。

## 正式采用

- 用户于 2026-08-08 明确批准，正式实现提交为 `7630e66`。
- `UiContractParser` 统一索引 canonical、probe 和显式登记的 regression golden；`verifyUiContracts` 校验仓库相对路径与文件存在，`verifyUiGoldenChange` 拒绝重复归属并继续要求主结构当轮 fidelity PASS。
- 集成命令：`./gradlew :feature:profile:validateDebugScreenshotTest verifyUiFidelity verifyUiGoldenChange -Pmoekoe.uiContract=user-profile.content.light --no-configuration-cache`。
- 集成结果：通过；九张本轮变化的状态 reference 均由 `user-profile.content.light` 显式授权，未登记 reference 的拒绝逻辑保持不变。

## 边界

- 未放宽个人主页 contract 的固定锚点、累计漂移或像素阈值。
- regression golden 只承担同结构状态回归，不新增设计真值，也不继承独立阈值或视觉债务。
