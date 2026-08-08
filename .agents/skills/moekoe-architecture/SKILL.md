---
name: moekoe-architecture
description: 设计或复核 MoeKoeMusic Android 架构。涉及 Gradle 模块、App/Feature 所有权、依赖方向、Navigation Compose、Route/Screen/ViewModel、Repository、Room、DataStore、Hilt、StateFlow、并发、公共接口，或 app、core、data、feature、build-logic 下的变更时使用。
---

# MoeKoe Android 架构

## 工作流

1. 读取 `docs/ARCHITECTURE.md`、`docs/ENGINEERING_STANDARDS.md` 的状态、Feature 与导航约束，以及 ADR-0002、ADR-0004；Feature 所有权以较新的 ADR-0004 为准。
2. 导航或模块化任务再读取 `docs/reference-audits/06-navigation-architecture.md` 和 `08-feature-modularization.md`。
3. 先标出当前模块、数据所有者、公共接口、依赖方向、主数据流、事务/并发边界和错误恢复，再提出最小变更。
4. 重大边界、新依赖或公共接口变化先调用 `$moekoe-spec-audit`。
5. 修改前运行 `./gradlew verifyArchitecture` 建立基线；修改后再次运行并报告实际结果，不能用架构说明代替检查。

## 不变量

- `:app` 只负责组合根、应用壳、应用级导航和系统入口；业务状态归所属 Feature。
- Feature 拥有自身导航键、Route、Screen、ViewModel 和测试，不依赖 `:app` 或其他 Feature 实现。
- Compose UI 不直接访问网络、数据库、DataStore、ExoPlayer 或 Service；Route/Screen/ViewModel 职责分离。
- ViewModel 对外只暴露不可变 `StateFlow` 和明确事件；可过期请求可取消或按序列隔离，已有内容恢复不得退回首次全屏加载。
- DTO、Entity、Domain Model 和 UI Model 分层；UseCase 只用于跨 Repository 或有明确业务规则的操作。
- 保持最小可见性和单一文件职责；不为形式机械引入 Feature `api/impl` 拆分。

## 输出与同步

- 说明采用方案、拒绝方案、接口/数据流影响、依赖增量、数据所有者和兼容策略。
- 新增例外必须进入集中 allowlist 并有 Accepted 审计；不得用源文件排除项绕过模块边界。
- 模块边界变化同步 `docs/ARCHITECTURE.md`，长期决策写 ADR；完成前调用 `$moekoe-validate-change`。
