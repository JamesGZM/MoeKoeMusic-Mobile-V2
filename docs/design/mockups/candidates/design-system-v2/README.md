# Design System V2 候选稿

状态：已于 2026-08-07 获得用户确认并晋级正式基线；本目录只保留确认过程记录。

本组候选稿用于校准设置页与 Design System 图板。标准 Toolbar 统一以
[`10-user-profile.png`](../../10-user-profile.png) 为视觉基准：标题相对完整页面居中，
导航与尾部操作使用无可见容器的图标按钮，沉浸式圆形按钮只用于 Hero 或封面背景。

候选文件：

- `09-settings-v2.png`
- `14-design-foundations-v2.png`
- `15-toolbar-navigation-v2.png`
- `16-actions-inputs-v2.png`
- `17-music-content-components-v2.png`
- `18-mobile-states-overlays-v2.png`

本目录中的六张确认版本已经分别替换 `docs/design/mockups/09-settings.png` 与
`14-design-foundations.png` 至 `18-mobile-states-overlays.png`；Compose 实现只引用正式路径。

## 2026-08-07 反馈修订

- `09-settings-v2.png` 的 Toolbar 重新以 `10-user-profile.png` 为坐标母版：保留顶部安全区留白，不绘制系统状态栏示意图；使用共享无横杆 Chevron、同一居中标题基线和内容起点。
- `16-actions-inputs-v2.png` 的输入图标改以当前登录 Compose 确认实现为母版：Phone、VerifiedUser、AccountCircle、Lock 使用浅蓝圆角方形前导容器；密码可见性等尾部操作保持无可见底板和至少 `48dp` 语义触控区。
- `18-mobile-states-overlays-v2.png` 的 Dialog 改以登录风险确认与短信验证为母版：系统居中、白色 Surface、Bold 标题、`304/320dp` 两类宽度、`12dp` 双按钮间距、Tonal 取消和 Filled 确认。
- `16-actions-inputs-v2.png` 再次校正前导图标几何：`32dp` 可见方形背景独立居中于 `56dp` 输入框，上下保留约 `12dp`，不可见的 `48dp` 触控区不参与背景绘制。
- `09-settings-v2.png` 改为宽度不变、内容高度自然增长的长画布；全部设置 Item 复用一致行高与上下内边距，存储和其他分组不再为了塞进固定屏高而压缩。

本轮只修订候选图与规范文本，没有晋级正式基线，也没有修改 Compose。图板中的自动生成说明文字仍只作视觉辅助；组件数值与行为以 `DESIGN_SYSTEM.md`、`UI_COMPONENTS.md` 和已确认登录实现为准。
