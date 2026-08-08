# 用户主页设计符合度证据

- 确认稿：`docs/design/mockups/10-user-profile.png`
- 可执行契约：`docs/design/contracts/user-profile.content.light.properties`
- 实现截图：`feature/profile/src/screenshotTestDebug/reference/cn/james/music/feature/profile/UserProfileScreenshotTestKt/UserProfileContractContentLightScreenshot_ContractContentLight_18c38141_0.png`
- 比较范围：确认稿使用 `0,52,853,1620` crop，Compose 使用 `390 × 843dp` canonical viewport 和 `0,0,1024,1945` render crop；MiniPlayer 与系统安全区由 App 壳负责，不重复画入 Feature 截图。

## 证据文件

- `build/reports/ui-evidence/user-profile.content.light/side-by-side.png`
- `build/reports/ui-evidence/user-profile.content.light/overlay.png`
- `build/reports/ui-evidence/user-profile.content.light/diff.png`
- `build/reports/ui-evidence/user-profile.content.light/result.properties`

## 结论

- Toolbar、资料 Hero、头像与徽标、三项关系统计、编辑按钮、听歌概览、创建歌单标题及三行歌单的比例、顺序和首屏密度已在同一画布复核。
- 页面复用已经确认的全局居中 Toolbar：标题按母版实测使用 `15sp / 22sp`，共享 Chevron 为 `16dp` 可见画布，分享与更多为 `20dp` 可见图标；三类内容分别进行光学校正，分享图形在自身触控区内向尾端校正 `8dp`，两个独立 `48dp` 触控区不移动。Hero 紧接 Toolbar，不叠加额外顶部留白。
- 2026-08-08 用户目测复核发现首版机器 PASS 仍遗漏局部排版误差；本轮重新量取后，昵称改为 `titleLarge`，分区标题统一为 `titleSmall / SemiBold`，关系与概览说明使用 `bodySmall`，歌单标题按设计画布收至 `13sp / 18sp`。编辑操作拆分为 `38dp` 可见描边与 `48dp` 语义触控区，不再混用两个高度。
- 修正后的结构 fidelity 通过：像素均值误差 `0.02753`、变化像素比例 `0.04974`，均优于首版的 `0.03390 / 0.06101`。Hero、编辑按钮上下边界、概览标题上下边界、概览卡、歌单容器及第二/三行共 9 个锚点误差为 `0.08–1.93px`，累计纵向漂移 `1.383px`，均低于 contract 阈值。
- canonical、probe、Light 短视口、Dark、AMOLED、Loading、Empty、Error、Offline、`1.5×`、`2.0×`、长文本和宽屏共 13 组截图基线已建立；大字体时 Hero 与概览纵向重排，页面继续使用单一纵向滚动容器。
- “我的”只上抛资料页事件，`:app` 注册类型安全子页面并负责返回栈、一级底栏与 MiniPlayer；`:feature:profile` 不反向依赖 `:feature:my`、数据层或播放层。
- 当前计数、签名、时长和歌单仅是确认稿 UI fixture；分享、编辑、关系和歌单操作只保留事件端口，未伪装真实业务完成。
- 保留的 P3 差异为原创图片内容、平台字体字形与 Material 图标笔画；组件边界、圆角、间距、行高和信息层级没有据此豁免。
- `:feature:profile` 单元测试、截图测试和 Android Compose 测试编译已通过；因本轮未指定物理设备，没有执行 `connectedDebugAndroidTest`，不声称完成真机 Insets、TalkBack 或触控验收。
