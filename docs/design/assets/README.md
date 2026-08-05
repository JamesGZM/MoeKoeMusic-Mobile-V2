# 生产视觉资产

本目录保存从已确认设计图派生、可供 Android 实现处理的源资产。这里的文件不是新的页面设计，也不能覆盖 Mockup、Design System 或字符串资源的权威性。

## `login-hero.png`

- 状态：已确认设计的生产派生资产。
- 来源：[`../mockups/13-login-phone-immersive.png`](../mockups/13-login-phone-immersive.png)。
- 生成日期：2026-08-05。
- 工具：`frontend-design` 确定视觉约束，内置 ImageGen 以原图为编辑参考生成。
- 用途：登录页顶部 16:9 Hero 背景；Android 使用 `drawable-nodpi` 压缩副本并通过 `ContentScale.Crop` 适配。
- 保留：夏日蓝白色调、海岸天空、音乐线条、右侧吉他少女和照片卡片构图。
- 移除：状态栏、返回按钮、标题、副标题、登录面板、控件、文字和水印。上述内容必须由 Compose 原生绘制。
- 限制：不得从该位图反推布局尺寸、文案、图标或协议字段；页面结构仍以 `13` 号确认设计图和 `DESIGN_SYSTEM.md` 为准。

最终生成提示要求重建无 UI、无文字的独立宽幅插画，保留确认设计的主体、风格、构图和色彩，并在左侧提供原生标题的视觉负空间。
