# Agent 执行记录

每个由子智能体完成、等待父智能体验收和提交的切片使用一个 `.properties` 记录。记录只证明路径、Git diff 与声明的一致性；`identityProof=trace-only` 不证明也不能替代平台级父子身份隔离。

```properties
schemaVersion=1
task=/root/example-child
authorizedPaths=feature/example/src/main/kotlin,feature/example/src/test/kotlin,.agents/execution/example.properties
actualFiles=feature/example/src/main/kotlin/Example.kt,feature/example/src/test/kotlin/ExampleTest.kt,.agents/execution/example.properties
childChecks=./gradlew :feature:example:testDebugUnitTest
unverifiedItems=parent acceptance and commit pending
implementedByAgent=/root/example-child
committedByAgent=pending-parent-acceptance
identityProof=trace-only
```

子智能体在实现完成后由父智能体运行 worktree 校验：

```bash
./gradlew verifyAgentExecutionGovernance \
  -Pmoekoe.agentExecutionRecord=.agents/execution/example.properties \
  -Pmoekoe.agentExecutionPhase=worktree
```

子智能体在交付前根据父智能体提供的路径填写 `committedByAgent`；父智能体验收通过、精确暂存后使用 `staged`。该阶段只在 `build/reports/agent-execution/` 写入顺序收据，绑定 record 内容 hash、当时 `HEAD`、暂存文件和 `git diff --cached --binary` 的 patch hash。原子提交时在提交正文加入：

```text
Implemented-By-Agent: /root/example-child
Committed-By-Agent: /root
```

再以 `committed` 校验 HEAD：commit parent、文件集合和 `git show --format= --binary HEAD` 的 patch hash 必须与 staged 收据一致，再校验 trailer。worktree 阶段不写收据。收据只记录顺序与内容，不是身份认证；本任务不能鉴别伪造的 agent 名称、trailer 或 record。
