# 阶段 3：本地音乐闭环

状态：Accepted，等待实现。开发前门禁已由 [`../reference-audits/03-local-music.md`](../reference-audits/03-local-music.md) 完成。

## 目标

完成不依赖在线服务的本地音乐产品闭环，并把外部“打开方式”实现为“导入 + 播放”。

## 实现范围

- “我的 → 本地音乐”列表、空状态、导入文件和扫描设备音乐入口。
- Storage Access Framework 单选、多选；MediaStore 候选项扫描和权限处理。
- `ACTION_VIEW`、`ACTION_SEND`、`ACTION_SEND_MULTIPLE` 的独立入口 Activity。
- 统一复制、SHA-256 去重、媒体探测、原子落盘、Room 提交和失败清理。
- 进度、取消、空间不足、损坏、不支持、重复与批量结果反馈。
- 本地歌曲播放、排序、搜索和删除；删除时同步播放队列。
- 补齐本地音乐页面视觉规格，沿用当前 Material 3 token 和反馈组件。
- 使用 WorkManager 2.11.2 全局串行执行导入，AndroidX Hilt Work 1.3.0 注入 Worker，Coil 3.4.0 加载 App 管理的封面。
- 建立首页、发现、我的三项正式导航；本阶段只让“我的 → 本地音乐”形成业务闭环。
- 增加基础 MiniPlayer 和基础播放队列 Bottom Sheet；全屏播放器仍属于阶段 5。

详细语义见 [`../LOCAL_MUSIC.md`](../LOCAL_MUSIC.md)。

## 测试

- 导入状态机与 Repository JVM 单测。
- Intent 冷启动、热启动、缺失参数、错误 MIME、非法 Scheme 测试。
- 文件成功/失败、数据库回滚、取消、空间不足和重复内容测试。
- API 26、32、33、36 权限和端到端设备测试。
- 本地列表、导入进度、错误和大字体 Compose UI/截图测试。

## 完成标准

- App 内单个、批量和设备扫描导入可用。
- 文件管理器选择 MoeKoe 后，只有复制和 Room 提交成功才立即播放。
- 删除原始文件不影响播放；重复打开不产生副本。
- 导入失败绝不播放外部临时 URI。
