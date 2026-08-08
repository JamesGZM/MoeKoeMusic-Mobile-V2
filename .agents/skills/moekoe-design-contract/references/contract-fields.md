# UI contract 字段

每个 `.properties` 文件只描述一个稳定 UI 状态。

## 身份与来源

- `schemaVersion`、`id`、`module`、`state`
- `design.path`、`design.sha256`、`design.width`、`design.height`
- `approval.status`、`approval.evidence`

## 渲染入口

- `screenshot.testId`、`screenshot.source`、`screenshot.golden`
- `screenshot.rendered`、`viewport.widthDp`、`viewport.heightDp`
- `locale`、`theme`、`fontScale`

## 对比规则

- `design.crop` 与 `render.crop`：`x,y,width,height`；省略时使用完整图片。
- `anchor.<name>`：`x,y,tolerance`；坐标使用设计单位。
- `mask.<name>`：`x,y,width,height,reason`；原因不能为空。
- `tolerance.fixed` 与 `tolerance.cumulativeY`：设计单位阈值。

## 历史债务

- `debt.status=active|none`
- active 时必须有 `debt.reason`、`debt.owner`、`debt.expiresAt` 和逗号分隔的 `debt.scopePaths`。
- 新增状态不得设置 active；范围被修改或到期后验证必须失败。
