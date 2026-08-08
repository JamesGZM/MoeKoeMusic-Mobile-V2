# 个人主页局部排版误通过评测（2026-08-08）

## 原始正式版本

- 版本：`dcb97c3`。
- 设计源：`docs/design/mockups/10-user-profile.png`，使用 contract 的 `0,52,853,1620` crop。
- 实现源：该提交的 canonical screenshot golden，归一化至 `853 × 1620px`。
- 正式结果：`meanError=0.0338979`、`changedRatio=0.0610097`、三个容器锚点和累计漂移均通过。

## 用户发现的局部偏差

| 区域 | 设计字形/边界 | `dcb97c3` 实现 | 偏差 |
| --- | --- | --- | --- |
| 编辑按钮可见描边 | `y=560…642`，约 `83px` | `y=561…665`，约 `105px` | 高约 `22px` |
| 听歌概览字形 | `133 × 31px` | `150 × 37px` | 宽约 `17px`、高约 `6px` |
| 首行歌单标题字形 | `77 × 24px` | `113 × 37px` | 宽约 `36px`、高约 `13px` |
| 昵称字形 | `88 × 40px`，顶部 `218` | `98 × 44px`，顶部 `236` | 下移约 `18px` 且偏大 |

测量使用 Java ImageIO 对设计 crop 与 normalized rendered 的固定矩形进行颜色阈值扫描；表中坐标全部位于同一 `853 × 1620px` 设计空间。原始 screenshot 可由 `git show dcb97c3:<golden-path>` 重现。

## 候选隔离评测

- 测试：`candidates.RegionalUiFidelityCandidateTest`。
- 目标场景：`100 × 100` 全页只有一个 `10 × 10` 局部区域完全错误；全页均值仍低于旧阈值，局部区域必须失败。
- 安全反例：全页失败不能被局部区域 PASS 反向放行；区域越界必须拒绝。
- 隔离命令：`./gradlew :build-logic:test --tests 'candidates.RegionalUiFidelityCandidateTest'`，3 项通过。
- 现有回归命令：`./gradlew :build-logic:test`，通过；现有 parser、图像比较、架构与 golden 映射测试未退化。
- 修复页面集成结果：全页 `meanError=0.0275341`、`changedRatio=0.0497430`，9 个结构/局部边界锚点全部通过；候选尚未接入正式区域指标。
- 候选阶段未改变正式门禁，并在目标回归与现有 build-logic 测试通过后请求人工批准。

## 人工批准与正式化

- 用户于 2026-08-08 明确批准区域视觉门禁。
- 正式单测迁移至 `UiContractParserTest` 与 `ImageComparatorTest`，覆盖局部错误被全页稀释、局部通过不能覆盖全页失败、区域越界、非法阈值及与 mask/dynamic 相交。
- `user-profile.content.light` 登记三个 design crop 区域：`editButton=54,548,745,110`、`overviewTitle=32,690,240,100`、`playlistTypography=190,1060,330,400`。
- 修复版实测值依次为 `0.04023/0.09636`、`0.05907/0.11158`、`0.03593/0.05669`；正式 `meanError/changedRatio` 阈值依次为 `0.05/0.11`、`0.07/0.13`、`0.045/0.07`。
- 正式集成命令：`./gradlew spotlessApply spotlessCheck :build-logic:test :feature:profile:testDebugUnitTest :feature:profile:compileDebugAndroidTestKotlin verifyArchitecture verifySkillGovernance verifyUiContracts :feature:profile:validateDebugScreenshotTest verifyUiFidelity verifyUiGoldenChange :app:assembleDebug -Pmoekoe.uiContract=user-profile.content.light --no-configuration-cache`。
- 正式集成结果：`BUILD SUCCESSFUL`，405 个任务；架构、Skill 治理、5 个 UI contract、截图回归、设计符合度、golden 变更授权和 Debug APK 均通过。区域结果全部为 `passed=true`，全页指标与累计漂移保持 `0.0275341`、`0.0497430`、`1.3830334`。
- 正式实现提交：`151247b`。
