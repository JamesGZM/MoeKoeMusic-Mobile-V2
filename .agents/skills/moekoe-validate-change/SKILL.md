---
name: moekoe-validate-change
description: 验证并完成 MoeKoeMusic 代码切片或有证据的审查。在宣布实现完成或提交前，以及处理测试、Gradle 检查、Lint、截图、真机验证、真实酷狗兼容性、文档同步、发布准备、命令结果和未验证限制时使用。
---

# MoeKoe 变更验证与提交

## 选择验证

1. 读取 `docs/TESTING_STRATEGY.md`、`docs/ENGINEERING_STANDARDS.md` 的测试、Git 和 Definition of Done，以及 `README.md` 的当前构建命令。
2. 检查 `git status --short` 和差异，区分本切片与用户已有变更；不得修改、暂存或提交无关文件。
3. 从最快相关检查开始，再运行受影响模块的完整测试；不要用无关的全量命令替代针对性验证。
4. 行为变化补相应单元、集成或 UI 证据；UI 必须通过相关 screenshot validation 与设计符合度检查，Preview 只用于反馈，指定真机只补 host 无法证明的场景。

## 特殊矩阵

- 签名、加密和请求参数：固定虚构向量的 Node/Kotlin 对照测试。
- Repository、队列和播放模式：单元测试覆盖状态与失败恢复。
- 截图任务按 README 对具体命令关闭 Configuration Cache，不全局禁用。
- Screenshot reference 有变更时运行 `verifyUiGoldenChange`；没有对应 contract、符合度证据或明确批准时不得更新基线。
- 真机验收只使用用户指定且已连接的设备；不得创建或启动模拟器，也不得把历史模拟器结果冒充本轮证据。
- 真实酷狗服务测试与普通 CI 隔离，限制调用并脱敏；固定向量、Fake Transport 和真实兼容性分别报告。

## 完成门禁

- 核对安全日志、额外网络调用、依赖许可证、`verifyArchitecture`、UI contract、设计符合度、文档和 ADR 同步。
- 报告实际执行的命令、结果、未执行原因和剩余限制；不要把未验证内容描述为通过。
- 每个完成且验证通过的可审查切片立即形成只包含该切片的原子提交；提交前再次核对 diff 与暂存范围。
