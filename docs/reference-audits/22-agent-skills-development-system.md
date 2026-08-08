# Agent Skills 与可执行开发门禁审计

状态：Accepted。审计日期：2026-08-08。

## 决策点

如何复用持续更新的 Android/Compose Agent Skills，同时确保 MoeKoe 的架构、设计基线、真机限制和验收结果不会被通用上游规则覆盖。

## 当前问题

- 仓库规范已经定义模块边界、设计符合度和截图回归，但主要依赖 agent 阅读 Markdown 后自行执行。
- CI 只运行格式、单测、Lint、Compose 实现截图回归和构建，没有架构依赖图、设计源哈希、设计稿叠加或锚点阈值检查。
- Compose reference screenshot 可以与错误实现一起更新；测试重新变绿只能证明实现与新 reference 一致，不能证明符合已确认设计稿。
- 现有 skills 没有统一的输入契约、必产物、命令、失败语义和漏检回归集，规则容易继续增长但无法证明有效。

## 上游 1：Google Android Skills

- 仓库：https://github.com/android/skills
- 固定提交：`1e5e7ae6138bebd0835d0d5854b0b9adfeed3181`
- 许可证：Apache-2.0。
- 固定文件：
  - `testing/testing-setup/SKILL.md`
  - `jetpack-compose/adaptive/SKILL.md`
  - `jetpack-compose/theming/styles/SKILL.md`

采用：测试现状盘点、单元/UI/截图/端到端职责分离、不同窗口尺寸与字体倍率矩阵、行为测试不由截图替代，以及使用项目级 skill 固化团队工作流。

拒绝直接套用：

- `adaptive` 以 Navigation 3 为前置条件；本项目固定使用 Navigation Compose 2.9.8 类型安全目的地，未完成独立迁移审计前不得升级。
- `testing-setup` 在缺少既有方案时可建议 Roborazzi、Dropshots、Jacoco 等工具；本项目已使用 Compose Preview Screenshot Testing，首期不并存第二套截图基础设施，也不新增模拟器验收。
- 通用的九尺寸截图矩阵不能替代页面适配契约；MoeKoe 先覆盖确认画布、短高度、大字体和已登记宽屏行为，再按页面风险扩展。

## 上游 2：Compose Agent Skill

- 仓库：https://github.com/aldefy/compose-skill
- 固定提交：`954ef54ea32288fbc90745f012d09d7b791f0d8a`
- 许可证：Skill 指南为 MIT；`references/source-code/` 内 AndroidX/Compose 源码为 Apache-2.0。本项目不复制这些源码。
- 固定文件：主 `SKILL.md`，以及 state management、view composition、modifiers、side effects、navigation、design-to-compose、accessibility、performance 和 theming 参考。

采用：按任务信号只读取相关参考、状态提升与单向数据流、Modifier 顺序、受控副作用、Preview 可测试入口、语义优先、Design System token 和设计到 Compose 的语义拆解。

拒绝直接套用：

- 上游同时覆盖 Android、Desktop、iOS、Web 和 Compose Multiplatform；本项目只采用 Android 原生相关部分。
- 通用 navigation、atomic design 或 design-to-code 建议不能改变 ADR-0004 的 Feature 所有权，也不能把 Figma/截图节点逐项翻译成自由尺寸实现。
- 上游源码摘录只用于核对 API 行为，不进入 MoeKoe 源码、APK 或项目 skill 正文。

## 最终方案

采用三层治理：

1. `.agents/upstreams/agent-skills.properties` 固定上游仓库、提交、许可证和允许审计的文件集合。
2. MoeKoe 项目 skills 只保存经本项目改写的工作流、约束和上游采用/拒绝结论；运行任务时不自动下载或执行上游内容。
3. Gradle/CI 任务校验架构边界、UI contract、设计符合度证据和 reference screenshot 更新，实际命令结果高于聊天中的自报结论。

上游更新必须显式执行只读检查，生成提交差异并更新本审计。不得跟随浮动分支，不得自动改写正式 skill。第三方规则发生冲突时，优先级固定为：MoeKoe 常驻硬门禁、Accepted ADR/规格、项目 skill、固定上游参考。

## Skill 分工

- `moekoe-spec-audit`：重大变更、上游准入、豁免和历史债务分类。
- `moekoe-architecture`：模块所有权、依赖方向和可执行架构结果。
- `moekoe-design-contract`：确认稿到状态、画布、锚点、裁切和适配契约。
- `moekoe-ui-compose`：按已接受 contract 实现 Android Compose UI。
- `moekoe-visual-qa`：生成设计稿与当前渲染图的归一化证据并判定阈值。
- `moekoe-skill-evolution`：记录漏检、构造回归案例和候选 skill；正式规则仍需人工批准。
- `moekoe-validate-change`：只接受实际命令和产物，不以调用过 skill 代替验证。

## 安全、依赖与兼容性

- 首期不新增 App 依赖、权限、网络调用、遥测、运行时服务或持久化。
- 上游检查只在开发者显式调用或每周只读工作流中访问公开 Git 仓库；普通 PR 构建和 App 不联网获取 skill。
- 固定版本材料只学习流程和公开行为，不复制第三方实现。更新时重新核对许可证和文件清单。
- 视觉与架构门禁属于构建工具，不改变运行时公共 API，也不改变现有 Navigation、Hilt、Repository、Media3 或协议边界。

## 验收

- 固定版本锁可由 JDK Properties 读取，仓库、40 位提交、许可证和非空文件集合均可验证。
- 项目 skill 明确记录每项上游建议的采用或拒绝理由，不存在直接跟随最新版的入口。
- 上游检查只报告可用更新，不修改锁文件、skill 或源码。
- 后续可执行门禁、登录样板和 skill 回归集分别形成独立原子提交。

## 2026-08-08 局部视觉指标补充审计

个人主页曾在全页 `meanError`、`changedRatio` 和容器锚点均通过时，仍遗漏编辑按钮高度、栏目标题和歌单字体的局部偏差。该问题属于既有 ImageIO 设计符合度门禁的判定覆盖不足，不需要新增依赖或引入另一套截图工具。

采用：结构 contract 可登记 `region.<name>=x,y,width,height;meanErrorMax;changedRatioMax`，坐标继续使用 design crop 空间；区域与全页指标、锚点同时参与结果，active debt 也不能绕过区域失败。区域越界、阈值不在 `[0,1]`，或与 `mask.*` / `dynamic.*` 相交时直接拒绝。

拒绝：不通过持续收紧全页阈值碰运气，不用实现常量断言代替渲染结果，不允许局部通过覆盖全页失败，也不把关键文字或按钮改登记为 mask。该扩展沿用本审计已固定的 Google Android Skills 与 Compose Agent Skill 版本，只强化项目级设计门禁，不复制上游代码、不改变 App 运行时行为。
