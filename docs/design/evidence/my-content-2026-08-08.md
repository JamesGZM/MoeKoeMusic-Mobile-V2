# “我的首页”确认稿设计符合度

- 权威设计：[`04-my-v4.png`](../mockups/04-my-v4.png)、[`04-my-anonymous.png`](../mockups/04-my-anonymous.png)。
- 实现截图：`MyAuthenticatedScreenshot`、`MyAnonymousScreenshot`，另保留匿名 Dark 与匿名/已登录 `1.5×` 回归基准。
- 状态：简体中文、Light、`1.0×` 字体、已登录与匿名主态。
- 画布：确认稿 `853 × 1844px`；Compose Preview `390 × 844dp`，截图 `1024 × 2216px`。对照时截取确认稿 `y = 0..1595` 的页面内容区并归一化为 `1024 × 1917px`，与实现同范围比较；MiniPlayer 和 NavigationBar 属于应用壳，由公共组件切片独立验收。

## 证据文件

- 已登录：[`side-by-side`](my-authenticated-content-2026-08-08-side-by-side.png)、[`overlay`](my-authenticated-content-2026-08-08-overlay.png)、[`diff`](my-authenticated-content-2026-08-08-diff.png)。
- 匿名：[`side-by-side`](my-anonymous-content-2026-08-08-side-by-side.png)、[`overlay`](my-anonymous-content-2026-08-08-overlay.png)、[`diff`](my-anonymous-content-2026-08-08-diff.png)。

## 复核结论

- 账户卡、圆形头像、设置入口、Chevron、签到/VIP 双操作、快捷入口、收藏与关注及创建歌单保持确认稿顺序，没有增加页面标题或等待业务接入的占位结构。
- 账户卡和快捷/收藏容器已按确认稿重新校准顶部起点、视觉圆角、按钮浅色容器、Section 标题层级、四列图标槽和纵向密度；触控目标仍不小于 `48dp`。
- 登录态截图 fixture 保留确认稿统计和三行歌单，只用于视觉测试；运行时仍消费 Repository 的真实资料与明确空值，没有写入假业务数据。
- 匿名空态不再用通用 `LibraryMusic` 图标方块替代稿内插画。`my_playlist_empty.png` 由内置 ImageGen 以确认稿为视觉参考生成，仅复现浅蓝音乐单、圆点与叶片；资源经透明背景与去绿边处理后独立落盘，没有裁切设计截图。
- 匿名态继续把账号资产事件上抛到唯一登录入口，本地音乐与设置保持原行为；事件由 `MyAction` 在 Route 层统一映射，没有修改 ViewModel、Repository 或协议。
- Material Icons 与稿内定制图标存在轻微笔画差异；生成插画的纸张细节与稿内示例不是逐像素复制，但语义、槽位、色调和视觉重量一致，归为 P3。

## 2026-08-09 结构与契约复核

- 新增可执行契约 `my.authenticated.default`，覆盖正常截图、独立布局探针、4 个回归状态、6 个结构锚点和 6 个关键视觉区域；动态头像与歌单封面单独遮罩。
- `MyScreen` 收敛为页面编排壳，账户、快捷入口、收藏、歌单、空态/错误态拆为页面私有组件；页面只接收不可变状态与 `MyAction`，Route 负责 ViewModel 和导航映射。
- 对照确认稿修正快捷入口、收藏卡片和歌单行的纵向节奏，并校准入口文字、辅助文字与 Section 标题的字号、行高和字重。
- 最终自动对照：全局平均误差 `0.0387`、变化像素比例 `0.0741`；6 个锚点均在 `3px` 内，累计漂移 `1.886px`，所有视觉区域通过。

final result: passed
