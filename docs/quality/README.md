# 开发系统反馈账本

`incidents/` 保存实际漏检、触发失败和流程瓶颈；`.agents/evals/` 保存不携带预期答案的隔离回归任务。`verifySkillGovernance` 校验二者的映射、字段和 skill 路径。

系统可以自动生成 incident、候选规则和评测报告，但不能自动批准、覆盖或提交正式 skill、阈值和上游锁文件。候选需通过目标回归且关键安全案例不退化，再由人工批准形成独立提交。

固定上游只读检查：

```bash
./gradlew checkAgentUpstreamUpdates
```

该任务只报告候选 HEAD，不会下载、合并或改写 `.agents/upstreams/agent-skills.properties`。
