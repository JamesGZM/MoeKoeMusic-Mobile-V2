# UI contract 字段

每个 `.properties` 文件描述一个页面、Dialog 或 Sheet 的结构设计基线，并关联适用的功能状态规则。

## 身份与来源

- `schemaVersion`、`id`、`module`、`state`
- `contract.kind=structure`、`state.spec`、`state.testId`
- `design.path`、`design.sha256`、`design.width`、`design.height`
- `approval.status`、`approval.evidence`

## 渲染入口

- `screenshot.testId`、`screenshot.source`、`screenshot.golden`、`screenshot.goldenSha256`
- `screenshot.rendered`、`viewport.widthDp`、`viewport.heightDp`
- `locale`、`theme`、`fontScale`
- 强制 contract 另有 `probe.testId`、`probe.source`、`probe.golden`、`probe.goldenSha256`、`probe.rendered`。

## 对比规则

- `design.crop` 与 `render.crop`：`x,y,width,height`；省略时使用完整图片。
- `anchor.<name>`：`x,y,tolerance,r,g,b`；坐标使用设计单位，RGB 是 probe 的独占色标。
- `mask.<name>`：`x,y,width,height;reason`；原因不能为空。
- `dynamic.<name>`：`x,y,width,height;type;reason`，type 只能是 `text`、`input`、`component-state`、`system` 或 `derived-artwork`；仅豁免内容像素，不豁免结构锚点。
- `region.<name>`：`x,y,width,height;meanErrorMax;changedRatioMax`；坐标使用 design crop 空间，阈值必须位于 `[0,1]`。关键标题、操作区或局部排版需要独立于全页指标验收时登记；region 与全页指标必须同时通过，且不得与 `mask.*` 或 `dynamic.*` 相交。
- `drift.anchors`：按纵向顺序列出需要计算累计漂移的锚点。
- `pixel.deltaThreshold`、`pixel.meanError.max`、`pixel.changedRatio.max`：归一化像素指标阈值。
- `tolerance.fixed` 与 `tolerance.cumulativeY`：设计单位阈值。

同一结构基线可服务多个功能状态。启用、禁用、加载、错误、倒计时和状态保留由 `state.spec` 指向的规格以及 `state.testId` 指向的行为/组件测试证明，不要求为每个状态生成独立设计图。

## 历史债务

- `debt.status=active|none`
- active 时必须有 `debt.reason`、`debt.owner`、`debt.expiresAt` 和逗号分隔的 `debt.scopePaths`。
- 新增状态不得设置 active；范围被修改或到期后验证必须失败。
- active contract 首次登记可以与债务范围同提交；后续修改 contract 不能重新获得该豁免。
