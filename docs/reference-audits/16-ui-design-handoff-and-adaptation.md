# UI 设计交付、屏幕适配与视觉验收审计

状态：Accepted。审计日期：2026-08-07。首个落地页面：登录。

## 决策点

如何在不改变已确认视觉设计的前提下，把固定尺寸设计画布映射到 Android 的不同密度、窗口、系统栏、字体倍率和宽屏环境，并用自动证据证明实现符合设计。

## 当前问题

- 已确认登录图同时被描述为“像素基座”和“不是固定像素切图”，缺少二者之间的转换契约。
- 现有实现可以独立选择 `dp`、间距和组件尺寸，导致局部看似合理、整页比例却不符合设计。
- 截图测试只比较 Compose 实现的旧/新结果，没有直接把确认稿作为期望值，更新基线后仍可能全部通过。
- API 29 真机通过只证明特定平台行为，不等于覆盖不同窗口尺寸和字体倍率。

## Android 官方约束

- [Support different display sizes](https://developer.android.com/develop/adaptive-apps/guides/support-different-display-sizes)：响应式判断应基于 App 当前可用窗口而非物理设备类型；嵌套组件应使用自身可用约束。
- [Use window size classes](https://developer.android.com/develop/adaptive-apps/guides/use-window-size-classes)：宽和高是独立维度，窗口类别会在运行时变化，尺寸类别用于高层布局决策而非识别设备。
- [Support different pixel densities](https://developer.android.com/training/multiscreen/screendensities)：布局使用密度无关单位，文字使用可响应用户设置的 `sp`，不能把原始屏幕像素直接硬编码为 Android 布局尺寸。
- [Grids and units](https://developer.android.com/design/ui/mobile/guides/layout-and-content/grids-and-units)：使用 `dp`/`sp` 保持跨密度一致性，并以响应式思维处理不同形态。
- [Compose constraints and modifier order](https://developer.android.com/develop/ui/compose/layouts/constraints-modifiers)：父约束决定子项可用边界，尺寸转换必须在明确的约束链中完成。

采用结论：设计图原始像素作为“设计单位”保存，不直接当作 `dp`；页面在当前 Compose 容器约束内用一个统一系数映射。高层宽屏行为使用窗口宽度类别，局部页面比例继续来自设计画布。

## 成熟项目 1：Now in Android

- 仓库：https://github.com/android/nowinandroid
- 固定提交：`7d45eae4f8720a0c77f507712ba2437ff974b6ed`
- 许可证：Apache-2.0。
- 文件：
  - `app/src/main/kotlin/com/google/samples/apps/nowinandroid/ui/NiaApp.kt`
  - `app/src/testDemo/kotlin/com/google/samples/apps/nowinandroid/ui/NiaAppScreenSizesScreenshotTests.kt`

采用：在组合根注入当前 `WindowAdaptiveInfo`，集中做高层布局决策；显式处理 safe drawing 与 IME Insets；截图测试分别强制宽和高，覆盖 compact/medium/expanded 的组合，而不是只用一台设备。

不采用：其导航形态和内容布局不是本项目登录设计；Roborazzi 也不是引入新依赖的理由。本项目继续使用现有 Compose Screenshot Testing，并额外增加设计稿叠加门禁。

## 成熟项目 2：Compose Samples

- 仓库：https://github.com/android/compose-samples
- 固定提交：`84788c81186acd5bf0d280100992c8a9c04120ad`
- 许可证：Apache-2.0。
- 文件：
  - `JetNews/app/src/main/java/com/example/jetnews/ui/JetnewsApp.kt`
  - `Reply/app/src/main/java/com/example/reply/ui/ReplyApp.kt`

采用：Jetnews 从 `currentWindowAdaptiveInfo()` 获取当前窗口类别，并只在 expanded 断点改变高层导航；Reply 将窗口宽度与折叠姿态用于单/双 Pane 决策。两者都没有按手机型号或物理分辨率分支。

不采用：登录没有已确认双 Pane 设计，不照搬 Reply 的 expanded 双栏；不把通用窗口断点用于逐项改变字段、按钮或字号。

## 候选方案

| 方案 | 结论 | 理由 |
| --- | --- | --- |
| 每个组件直接使用经验 `dp` | 拒绝 | 会破坏设计图中的相对比例和累计锚点 |
| 按设备物理分辨率建尺寸表 | 拒绝 | 分屏、自由窗口、密度和系统 Insets 会使设备表失效 |
| 宽高分别缩放整页 | 拒绝 | 产生形变，组件比例与圆角均失真 |
| 把整张设计图作为背景并覆盖点击区 | 拒绝 | 无真实文本、输入、主题、无障碍和动态状态能力 |
| 当前容器宽度统一缩放 + 高度滚动 + 宽屏居中画布 | 采用 | 保留设计几何，同时遵守 Android 当前窗口、Insets 和字体要求 |

## 最终规则

- `852 × 1846` 是登录设计坐标空间；Compact 页面按当前容器宽度使用一个统一缩放系数。
- 高度不参与二次缩放。高度不足滚动，高度富余不拉开内部间距。
- Medium/Expanded 没有新设计时使用最大 `480dp` 的居中单列画布，不擅自双栏。
- 正常字体 `1.0×` 匹配设计；系统大字体允许可访问性重排。触控区最小 `48dp`，但不改变视觉尺寸。
- 系统栏、cutout、导航栏和 IME 由外层 Insets 策略处理，不逐项修改设计尺寸。
- 先用设计稿叠加验证符合度，再建立/更新 Compose 回归截图。

完整页面契约见 [`../design/LOGIN_LAYOUT_SPEC.md`](../design/LOGIN_LAYOUT_SPEC.md)。

## 依赖与模块影响

- 不新增依赖、权限、模块、网络、持久化或公共业务接口。
- 后续实现应在 `:feature:login` 内集中页面测量和尺寸映射；共享的通用适配能力只有在第二个页面证明确有复用需求后才进入 `:core:designsystem`。
- Compose Screen 只消费当前约束和不可变 UI 状态，不读取物理设备型号或显示服务。

## 验收

- 设计稿归一化叠加：固定锚点偏差不超过 `2` 个设计单位，累计纵向漂移不超过 `3` 个设计单位。
- 显示矩阵：基准画布、目标手机、当前真机、紧凑短高/分屏、大字体和 Medium/Expanded。
- 平台矩阵：API、系统栏、导航模式、IME、旋转与进程恢复独立记录。
- 更新截图基准前必须附带设计符合度证据；不能以录制后的截图测试通过替代。
