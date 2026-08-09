---
name: moekoe-skill-evolution
description: 将 MoeKoe 开发流程中的重复漏检转成可评审的 skill 与质量门禁候选。检查误通过、skill 未触发或含糊、流程过慢、上游 skill 更新，或创建和复核 docs/quality/incidents 与 .agents/evals 文件时使用。
---

# MoeKoe Skill 演进

## 工作流

1. 保存原始失败证据，不先写预期答案；按 [`references/incident-fields.md`](references/incident-fields.md) 新建 incident。
2. 区分知识缺失、触发失败、流程缺口、不可执行门禁、错误阈值和验证覆盖不足。
3. 为同类问题增加最小回归任务与客观断言；每条拟晋级断言使用稳定 `assertionIds`，incident 与 eval 必须一一对应，防止问题描述更新后回归集仍停留在旧规则。单次偶发问题先登记，不立即膨胀正式 skill。
4. 在候选目录修改 skill 或门禁，使用当前正式版本与候选版本分别运行相同隔离任务，比较漏检率、误报、耗时和关键安全门禁。
5. 候选只有在目标回归通过且现有关键案例不退化时才可请求人工批准；正式 skill、阈值和锁文件不得自动改写或提交。
6. 批准后形成独立原子提交；只有候选规则和全部 assertion ID 都已由可执行证据覆盖时才把 incident 标记为 resolved，否则保留为 approved open，并明确剩余缺口。

## 隔离评测

- 对 sub-agent 只提供 skill 路径、用户式任务和原始仓库/artifact，不提供诊断、预期修复或答案。
- 合成违规必须明确标记 synthetic；历史案例必须链接实际 diff、截图或 evidence。
- 评测至少断言：是否触发正确 skill、是否停止在缺失门禁、是否运行正确命令、是否区分已验证与未验证、是否拒绝放宽关键约束。
- 架构、安全、隐私、协议和设计基线门禁任一退化时，候选直接失败，不能用平均分抵消。

## 自我修改边界

- 自动化可以生成 incident、候选补丁和评测报告，不能批准、覆盖或提交正式规则。
- 上游更新只比较 `.agents/upstreams/agent-skills.properties` 的固定提交；采用前重新执行 `$moekoe-spec-audit`。
- 两次以上同类漏检优先升级为可执行检查；仅靠增加提示文字必须说明为何无法自动验证。
