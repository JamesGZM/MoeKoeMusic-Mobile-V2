---
name: moekoe-spec-audit
description: Gate major MoeKoeMusic features before implementation. Use for new modules, permissions, storage, background work, dependencies, public APIs, protocol migrations, core UI systems, or changes under docs/plans and docs/reference-audits that establish implementation readiness.
---

# MoeKoe 功能规格与参考审计

## 工作流

1. 用 `git rev-parse --show-toplevel` 确定仓库根目录，并读取 `README.md` 的当前阶段。
2. 读取 `docs/ENGINEERING_STANDARDS.md` 的“功能开发门禁/依赖准入”、`docs/templates/FEATURE_SPEC_TEMPLATE.md` 和 `docs/PRODUCT_SCOPE.md`。
3. 按任务读取当前阶段计划、已有 `docs/reference-audits/`、`docs/REFERENCES.md` 和 ADR，避免重复已经固定的决策。
4. 明确产品目标、成功语义、非目标、Android 平台约束、模块与数据所有权、失败恢复和测试矩阵。
5. 以决策点组织审计：先 Android 官方约束，再检查至少两个成熟开源项目的固定提交、具体文件、采用点、拒绝点和许可证。
6. 把规格和审计写入仓库既有文档位置；不要把结论只留在聊天、Issue 或临时文件。

## 门禁

- 小型缺陷、文案和不改变行为边界的机械修改可豁免；新增依赖、权限、持久化、后台任务、公共接口或架构变化不可豁免。
- 本地已有固定参考源码时先读源码，不以真实服务探针代替；真实服务只用于实现后的兼容验收。
- 产品语义、技术栈、数据流、失败恢复或测试矩阵存在关键待定项时停止编码并指出阻塞项。
- 用户可见 UI 同时调用 `$moekoe-ui-compose`；模块边界变化同时调用 `$moekoe-architecture`；完成前调用 `$moekoe-validate-change`。
