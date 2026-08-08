# UI 设计符合度契约

每个 `.properties` 文件描述一个页面、Dialog 或 Sheet 的结构设计基线，并绑定已确认设计源、结构 `@PreviewTest`、布局 probe、动态区域、锚点和阈值。启禁、加载、错误等功能状态复用结构基线，通过状态规格、行为测试与 Design System 组件截图验证，不要求逐状态出图。字段语义以 [contract-fields.md](../../../.agents/skills/moekoe-design-contract/references/contract-fields.md) 为准。

执行顺序：

```bash
./gradlew verifyUiContracts
./gradlew verifyUiFidelity -Pmoekoe.uiContract=<state-id> --no-configuration-cache
./gradlew verifyUiGoldenChange --no-configuration-cache
```

`debt.status=active` 只用于接入系统前已经存在且有原始证据的偏差。必须记录原因、负责人、到期日和触碰范围；新页面不得以 debt 绕过门禁。
