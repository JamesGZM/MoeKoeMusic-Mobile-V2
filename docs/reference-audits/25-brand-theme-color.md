# 品牌主题色与全局配色参考审计

状态：Accepted。审计日期：2026-08-10。

## 产品定义

“主题色”是用户选择的全局**品牌色预设**，不是 ThemeMode、Android Monet 或播放器封面动态取色的别名。它立即替换应用 `MaterialTheme.colorScheme` 的 primary 角色组（`primary`、`onPrimary`、`primaryContainer`、`onPrimaryContainer`、`inversePrimary`）；切换后所有消费这些角色的已组合页面、Dialog、MiniPlayer、导航、按钮、选中态和控件焦点使用新的品牌色，进程重建后恢复。它不改变：

- `ThemeMode`（跟随系统 / 浅色 / 深色 / 纯黑）的选择和系统深浅色判定；
- success / warning / error / VIP 等语义色、文字/Surface 层级、排版、形状或间距；
- `:feature:player` 的封面派生 `PlayerPalette`、封面请求、歌词、播放队列、网络、Room 或 Media3；
- Android 12+ 系统 Monet；本能力始终使用 MoeKoe 自有的确定性 `ColorScheme`。

Android V2 冻结六档、稳定存储 ID 与行尾文案：

| 存储 ID | 设置文案 | 状态 |
| --- | --- | --- |
| `sky_blue` | 天空蓝 | 默认；精确保留当前蓝色 Light / Dark / AMOLED scheme |
| `sakura_pink` | 樱花粉 | 可选 |
| `star_purple` | 星紫 | 可选 |
| `mint_green` | 薄荷绿 | 可选 |
| `lake_cyan` | 湖水青 | 可选 |
| `sunset_orange` | 落日橙 | 可选 |

成功语义是：用户从设置“主题色”选择任一预设后，新的品牌角色在当前组合树立即生效；DataStore 成功持久化后，下一次启动、Activity 重建和 ThemeMode 切换仍使用同一预设。首次读、缺失、未知值和读取 `IOException` 均回退天空蓝；写失败必须回滚最后成功持久值并给出 Settings 全局 Retry 的精确重试入口。取消保持取消语义，不映射为普通失败或自动重放。

## 产品真值与参考审计

### MoeKoeMusic PC

- 固定版本：`../MoeKoeMusic@52c9833afe2e7fedcba8d5b23ff8d1f9731af73a`，GPL-2.0-only。
- `src/config/settings.js:26-38` 定义 `themeColor` 的 `pink / blue / green / orange` 四档，默认 `pink`；`src/views/Settings.vue:445-450` 将选择转发给 `applyThemeColor`；`src/utils/utils.js:24-58` 写入 CSS 品牌变量。
- 采用：主题色是独立于深浅外观的持久选择，并立即更新全局品牌角色。
- 不采用：PC 的四档集合、默认粉色、CSS 变量和 Electron / localStorage 实现；它们不是真正的 Android Design System 或 DataStore 真值。

### 旧 MoeKoeMusic Mobile

- 固定版本：`../MoeKoeMusic-Mobile@ab71195d4cf3297332490fd37704d1ae8973d4c5`，GPL-2.0-only。
- `src/constants/accents.ts:45-68` 定义 `pink / blue / purple / green / cyan / orange` 六个 accent preset，`src/hooks/use-palette.ts:16-19` 按 accent 和独立 scheme 合成 palette；`src/features/settings/store.ts:7-18,66-92` 将 `accentId` 与 `themeMode` 分开保存、读回并立即通知 UI；`src/app/settings.tsx:218-228` 以预设列表提供设置入口。
- 采用：六档集合、accent 与 ThemeMode 分离、未知存储值回退、组合时按当前预设生成全局 palette。
- 不采用：其默认 `pink`、SecureStore / 外部 store、Tamagui 组件与手写 RGB 派生公式。Android 把旧 Mobile 的“海空蓝”冻结为与确认稿一致的“天空蓝”。

### Now in Android 与 Android 官方

- 固定版本：`android/nowinandroid@7d45eae4f8720a0c77f507712ba2437ff974b6ed`，Apache-2.0；`core/datastore/src/main/kotlin/com/google/samples/apps/nowinandroid/core/datastore/NiaPreferencesDataSource.kt:26-57,87-112` 将持久化 theme brand 与 dark theme config 映射为类型化 `UserData`，并使用 `updateData` 写入。
- 采用：类型化偏好、未知 proto 值回退安全默认、Repository / DataStore 与 UI 主题消费分层；不采用 Proto、其 Android-brand 动态色开关和大型 user-data 范围。
- Android 官方 [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3) 将 color scheme、typography、shapes 作为 `MaterialTheme` 子系统，并按语义角色消费颜色；[Custom design systems in Compose](https://developer.android.com/develop/ui/compose/designsystems/custom) 允许在保留 Material 组件时扩展或替换颜色系统；[Preferences DataStore](https://developer.android.com/codelabs/android-preferences-datastore) 提供协程 / Flow 的异步、事务性键值存储。
- 采用：完整、可访问的 Material 颜色角色随全局主题重组；不采用官方动态色示例。系统 Monet 是可选能力，不覆盖已确认 MoeKoe 品牌基线。

### Android V2 决策

- **迁移事实**：PC 证明主题色是独立设置；旧 Mobile 证明六档和独立的 accent / scheme 生命周期。
- **Android 适配**：以已确认 `docs/design/mockups/09-settings.png` 和 `docs/DESIGN_SYSTEM.md` 为视觉优先级；当前 `core/designsystem/src/main/kotlin/cn/james/music/core/designsystem/Theme.kt:80-166,226-260` 的天空蓝 `#1677F2`、深色 `#8EC4FF` 及 AMOLED Surface 层级是默认 `sky_blue` 的逐像素兼容基线。默认蓝必须使当前 Light / Dark / AMOLED screenshot golden 零变化。
- **新决定**：新增主题色只替换 Material 的 `primary`、`onPrimary`、`primaryContainer`、`onPrimaryContainer` 与 `inversePrimary`；`secondary`、`tertiary`、Surface、error 和 MoeKoe extra semantic colors 固定。每一预设的三份 primary 角色表必须受测或以明确颜色表提供，不以随意 `copy(primary=...)` 破坏对比度。

## 技术设计、所有权与失败恢复

无新增模块、依赖、权限、网络、后台任务、日志或敏感数据。下个实现切片的最小所有权如下：

```text
:core:model：BrandThemeColorPreference + AppSettings 字段 + Repository setter
:data：PreferencesAppSettingsRepository 的独立 v1 string key 与 storage mapping
:app：AppThemeViewModel / MainActivity / MoeKoeApp 收集已成功 Settings snapshot
:core:designsystem：六档 × Light/Dark/AMOLED 的确定性品牌 ColorScheme 选择
:feature:settings：ThemeColor 行、现有选择 Dialog、独立 saving job/generation、回滚与 Retry
```

`core:model` 只保留语义 enum，不含 Compose `Color`、资源 ID、DataStore key 或 hex；`:data` 私有化 stable storage mapping；`:core:designsystem` 是颜色角色和 palette 的唯一所有者；`:app` 只作为全局主题组合根；`:feature:settings` 不依赖 app、data 或 designsystem 的实现细节。播放器动态色继续仅在 `:feature:player` 本地派生，不接收或覆盖该偏好。

读取 Flow 的初始值、缺失、未知值或 `IOException` 使用天空蓝并透传既有 read problem；写入只在 Repository 成功后成为持久值。Settings 为主题色单设 coroutine Job / generation：快速 A→B 选择的迟到 A 成功或失败不得覆盖 B；主题色任务不得取消主题模式、自动跳过、动态封面色、歌词附加文本、歌词字号或默认音质任务。最新完成失败成为全局 Retry 所属；Dismiss 清除全部 Retry 归属。用户切换主题色不会取消正在显示的 Dialog 以外的操作，也不会改变已启动的播放、封面取色或地址刷新。

## 视觉、状态与测试矩阵

`09-settings.png` 的“主题色”行和 `11-dialog-components.png` 的已确认 MoeDialog / RadioButton 是下一 UI 切片的结构依据。正常状态显示“天空蓝”等当前值；选择 Dialog 显示六档；保存中只禁用该行和 Dialog；失败回滚与 Retry 不要求另造页面。每个新 UI 状态开始前必须建立或更新 Settings contract，不能用全页 mask 或阈值放宽覆盖品牌角色变化。

- core/data JVM：默认、六档 round-trip、raw storage mapping、unknown / missing / read IOException、write failure、cancellation、与现有偏好互不覆盖。
- Settings JVM：snapshot 映射、成功、保存中禁用、失败回滚、精确 Retry、快速代际、与五类既有写入并发且最后失败所属正确。
- app/designsystem：六档 × Light/Dark/AMOLED 的 primary 角色表、天空蓝与当前三份 scheme 的精确等价、ThemeMode × BrandThemeColor 组合映射，以及所有非 primary 角色（secondary / tertiary / Surface / error / MoeKoe extra semantic colors）不变。产品层要求 app 不启用 Monet，主题色也不走 Monet；不预设删除现有 Design System 参数。
- Compose / AndroidTest：六项选择、Radio / selected / disabled 语义、恢复后的当前值；截图覆盖天空蓝现有 Light/Dark/AMOLED/1.5×/2.0× 零变化、每种非蓝预设至少一张代表状态，以及 Dialog 与大字体不裁切。
- 验收：先 screenshot validate（预期只有非蓝新增状态无 golden），再 design fidelity / side-by-side / overlay，最后受限更新授权 golden、lint、UiImpact / GoldenChange、Architecture、Skill / Agent governance。真实设备只验证用户指定设备上的 Activity 重建、系统深浅切换和全局色更新；本审计未启动 ADB、模拟器或真实服务。

## 原子实施顺序与非目标

1. 领域 + DataStore：类型化 enum、独立 key、读取/写入/取消测试。已完成于 030；尚未接入全局主题或设置 UI。
2. Design System：默认蓝零改动证明、六档 primary 角色表、对比度和 ThemeMode 组合单测；所有非 primary 角色保持不变。已完成于 031。
3. app 组合根：读取已成功 snapshot 并把纯 BrandThemeColor 输入交给 `MoeKoeTheme`；不把 DataStore 放进 UI。已完成于 031，MainActivity、AudioImportActivity 与 RiskCaptchaActivity 共用 app-level snapshot；Settings 选择 UI 仍未完成。
4. Settings：真实选择行、现有 Dialog、独立 generation、回滚与精确 Retry；完成 contract、语义和截图。
5. 全量视觉与指定真机：仅在离线验证全绿后验证重建/系统深浅切换，不启动真实酷狗服务。

非目标：系统 Monet、用户自定义取色器、逐页色覆盖、播放器封面色、ThemeMode 改造、语义色重定义、远端同步、导入/缓存/播放协议改变。产品、模块和视觉依据均无关键待定项，允许按以上顺序开始下一实现切片。
