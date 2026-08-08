# UI 设计符合度契约

每个 `.properties` 文件只描述一个稳定状态，并绑定已确认设计源、固定 fixture、`@PreviewTest`、正常 screenshot、布局 probe、裁切、遮罩、锚点和阈值。字段语义以 [contract-fields.md](../../../.agents/skills/moekoe-design-contract/references/contract-fields.md) 为准。

执行顺序：

```bash
./gradlew verifyUiContracts
./gradlew verifyUiFidelity -Pmoekoe.uiContract=<state-id> --no-configuration-cache
./gradlew verifyUiGoldenChange --no-configuration-cache
```

`debt.status=active` 只用于接入系统前已经存在且有原始证据的偏差。必须记录原因、负责人、到期日和触碰范围；新页面不得以 debt 绕过门禁。
