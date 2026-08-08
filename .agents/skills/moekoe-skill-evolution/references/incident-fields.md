# Incident 字段

每个 incident 使用独立 `.properties` 文件：

- `schemaVersion`、`id`、`status=open|candidate|resolved`
- `detectedAt`、`taskType`、`affectedSkill`
- `missedBy`、`evidence`、`rootCause`
- `candidateRule`、`expectedAssertion`、`evalCase`
- `owner`、`approvedBy`、`resolvedCommit`

禁止写入 token、Cookie、手机号、设备标识、真实载荷或未脱敏日志。
