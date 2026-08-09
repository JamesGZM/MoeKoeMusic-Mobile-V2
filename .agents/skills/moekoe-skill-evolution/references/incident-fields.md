# Incident 字段

每个 incident 使用独立 `.properties` 文件：

- `schemaVersion`、`id`、`status=open|candidate|resolved`
- `detectedAt`、`taskType`、`affectedSkill`
- `missedBy`、`evidence`、`rootCause`
- `candidateRule`、`expectedAssertion`、`evalCase`
- 拟晋级为正式规则时增加逗号分隔的稳定 `assertionIds`；对应 eval 使用完全相同的 ID、顺序和断言数量
- `owner`、`approvedBy`、`resolvedCommit`

禁止写入 token、Cookie、手机号、设备标识、真实载荷或未脱敏日志。
