# 阶段 3：本地音乐闭环

状态：进行中。开发前门禁已由 [`../reference-audits/03-local-music.md`](../reference-audits/03-local-music.md) 完成。

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

## 当前实施结果

- Room 已由 v1 显式迁移到 v2，新增本地音乐、导入批次和导入条目，并用大小与 SHA-256 唯一索引去重。
- Hilt WorkManager 全局唯一工作链串行复制；使用 `.partial`、流式哈希、250ms 节流进度、媒体探测、元数据降级、原子改名和失败回滚。
- SAF、MediaStore 扫描、`ACTION_VIEW`、`ACTION_SEND`、`ACTION_SEND_MULTIPLE` 已接入统一管线；HTTP/HTTPS 与缺失 URI 会在复制前拒绝。
- `ACTION_VIEW` 真机测试确认只有 Room 提交成功后才调用 `playNow`；重复内容复用已有记录，不播放原始 URI。
- 正式三项底部导航、本地列表、搜索、排序、设备多选、基础 MiniPlayer 与队列 Bottom Sheet 已建立；播放工程实验台只在 Debug 可达。
- Room Migration 与唯一约束测试已加入；本地音乐空状态、内容状态和 `1.5×` 字体导入状态已有稳定截图基准。
- 外部导入完成动作现由播放控制器统一切换到 Media3 应用线程；Service 初始快照恢复增加顺序屏障，避免冷启动恢复覆盖新播放命令。
- 阶段仍保持进行中，直到 API 26 当前代码复测、全部格式逐项导入和 WorkManager 失败恢复自动化验收完成。

### 设备矩阵（2026-08-05）

| 环境 | 当前结果 | 已验证内容 |
| --- | --- | --- |
| API 26 ARM64 AVD | 待复测 | 较早实现已通过；本轮最终代码复测时模拟器未注册到 ADB，不沿用旧结果冒充最终结果 |
| Huawei ELE-AL00，API 29 | 通过 | 当前代码 Room 5/5、App/Intent/UI 6/6；外部 VIEW 复制后播放，测试后已重装并冷启动 Debug App |
| API 32 ARM64 AVD | 通过 | 当前代码 Room 5/5、App/Intent/UI 6/6；外部 VIEW 复制后播放与正式导航 |
| API 33 ARM64 AVD | 通过 | 当前代码 Room 5/5、App/Intent/UI 6/6；通知允许自动测试；手动拒绝后仍完成 1/1 并生成 App 副本 |
| API 36 ARM64 AVD | 通过 | 当前代码 Room 5/5、App/Intent/UI 6/6；外部 VIEW、错误 URI 与权限分支 |

### 合成格式 fixture

生成命令：

```bash
./scripts/generate-local-music-fixtures.sh
```

本机 FFmpeg 7.1.1 生成结果：

| 文件 | SHA-256 |
| --- | --- |
| `tone.mp3` | `8ad2a59d144088d1f9b5a40c5142b5ad9fe228ce9f365a34f3ab58ba7307e7ba` |
| `tone.m4a` | `f9e794c8b9e92ae5caed8d79c8f464e48b1d2ba07881fbb5a1c03a343659038b` |
| `tone.flac` | `b9f4fa28de416eaab24f502fba430515959426241f221276558281c307c86ac7` |
| `tone.ogg` | `e31f66bc7b7349a423c402aba3ce9c387d5a3e3f982bffd195d93f0983edda0f` |
| `tone.wav` | `83dc7efff66aacccd401d7e825a092a97bf76a2c7ed8c06a73ecaeb09b3d10c8` |

## 完成标准

- App 内单个、批量和设备扫描导入可用。
- 文件管理器选择 MoeKoe 后，只有复制和 Room 提交成功才立即播放。
- 删除原始文件不影响播放；重复打开不产生副本。
- 导入失败绝不播放外部临时 URI。
