# 多账号选择三态设计符合度

状态：通过。复验日期：2026-08-09。

## 对照范围

- 确认稿：[`22a`](../mockups/candidates/login-v2/22a-multi-account-unselected.png)、[`22b`](../mockups/candidates/login-v2/22b-multi-account-selected.png)、[`22c`](../mockups/candidates/login-v2/22c-multi-account-failure.png)，均为 `852 × 1846`。
- Compose 基准：`LoginMultipleAccountsUnselectedScreenshot`、`LoginMultipleAccountsScreenshot`、`LoginMultipleAccountsFailureScreenshot`，`390 × 844dp`，输出 `1024 × 2216` 后归一化为 `852 × 1846`。
- 三态叠加图：[`login-multi-account-states-overlay-2026-08-08.png`](login-multi-account-states-overlay-2026-08-08.png)。
- 三态差异图：[`login-multi-account-states-diff-2026-08-08.png`](login-multi-account-states-diff-2026-08-08.png)。
- 选中态并排图：[`login-multi-account-selected-side-by-side-2026-08-08.png`](login-multi-account-selected-side-by-side-2026-08-08.png)，左侧确认稿、右侧 Compose。

## 结论

- 三态复用登录 Hero、Card 边界和页面设计坐标；标题、说明、三行账号列表、安全提示与操作区保持固定顺序。
- 未选中态不默认选择账号，主按钮使用全局 Filled 浅蓝禁用态；“选择其他账号”保持启用的主蓝描边和文字。
- 选中态只改变对应行的浅蓝容器、Radio 状态和主按钮可用性，不移动列表与操作锚点。
- 失败态保留原选择上下文，在安全提示下增加错误容器，并把主操作切换为“重新验证手机号”。
- 账号等级信息恢复为列表内的 `VIP` 描边标识；用户 ID 按确认稿保留前三位与后四位。
- 截图头像使用登录 Hero 中相同角色的确定性资源裁切，仅作为离线预览模型；正式运行仍优先加载协议返回的账号头像 URL。
- `login.multi-account.selected` 结构探针测得 Card 顶边 `566.46`、账号列表顶边 `782.22`，相对确认稿最大误差 `0.46`、累计纵向漂移 `0.24`，均低于固定 `2` / 累计 `3` 设计单位阈值；原账号列表偏低约 `12` 单位的视觉债务已关闭。
- 提交所选账号时，账号行与 ViewModel 同时锁定选择，避免画面选择与请求快照不一致。

## 可变与遮罩项

- 系统状态栏图标不属于 Compose Preview，对照时遮罩。
- Hero 使用仓库已确认的正式登录素材，不以不同生成批次的角色像素内容判定布局失败。
- 按钮使用已经确认的全局 `MoeButton` / `MoeOutlinedButton` 形状和状态色；状态图中的旧圆角示意不反向覆盖公共组件。
- Material Radio、Shield 与 Error 图标允许库级笔画差异，但选中、保护与错误语义必须一致。

## 回归门禁

- 截图覆盖未选中、选中、失败、`1.5×` 和 `2.0×` 失败态大字体。
- 视觉切片不改变“不默认选择”“失败保留上下文”和“重新验证手机号”的既有状态机行为。
- 截图基准只能在与 `22a–22c` 的归一化并排复核后更新。
