# UI 局部区域符合度门禁候选

状态：Approved。用户于 2026-08-08 明确批准，正式实现待提交后补录提交号。

## 原始失败

`user-profile.content.light` 在 `dcb97c3` 中通过全页像素指标和三个容器顶部锚点，但编辑按钮可见高度多约 `22px`，“听歌概览”字形宽高偏大，歌单标题也显著偏大。大面积背景相同使这些局部错误只占全页少量像素，最终被全局阈值稀释。

## 候选行为

- 允许结构 contract 显式登记 `region.<name>=x,y,width,height;meanErrorMax;changedRatioMax`。
- 区域坐标继续使用设计 crop 坐标，不建立第二套缩放公式。
- 每个区域独立计算像素均值误差和变化比例，并与全页指标、锚点同时参与 PASS。
- 区域必须位于 design crop 内，名称唯一，阈值为 `[0,1]`；不得借 region 扩大 mask 或豁免 dynamic 内容。
- 全页失败时，即使所有局部区域通过也必须失败。

## 拒绝方案

- 不继续收紧全页阈值来碰运气，因为局部错误占比随页面长度变化。
- 不为每个文字手写实现常量测试来替代渲染结果。
- 不扩大 probe 色块覆盖文字，因为 probe 应验证边界而不是遮挡真实字形。

## 拟实施范围

- `ImageComparator` 增加区域指标解析、计算和结果输出。
- `UiContractParser` 校验区域字段、边界和阈值。
- `user-profile.content.light` 登记 `editButton`、`overviewTitle`、`playlistTypography` 三个区域。
- build-logic 单元测试覆盖局部失败、全页失败不可被覆盖、越界和非法阈值。

## 批准后的固定边界

- `region.*` 与 `mask.*` 或 `dynamic.*` 相交时，contract 解析直接失败。
- active debt 不能绕过区域阈值；只有债务基线与全部区域同时通过时才输出 `APPROVED_DEBT`。
- `result.properties` 和 `result.json` 必须输出每个区域的均值误差、变化比例与通过状态，失败日志列出超限区域。
