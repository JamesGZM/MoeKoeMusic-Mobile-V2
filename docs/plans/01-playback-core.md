# 阶段 2：播放内核

状态：已完成。开始日期：2026-08-05，完成日期：2026-08-05。

## 目标

先建立符合 Android 系统规范的播放能力，使播放器生命周期独立于 Activity 和 Compose 页面。

## 实现范围

- 在 `:playback` 创建并持有 ExoPlayer、MediaSession 和 MediaLibraryService。
- 通过 MediaController 暴露 `PlaybackController` 与不可变播放状态。
- 实现队列添加、删除、移动、清空、下一首播放和恢复快照。
- 实现顺序、随机、单曲循环和列表循环。
- 接入音频焦点、耳机拔出、媒体按钮、通知栏和锁屏控制。
- 抽象 App 管理的本地来源与酷狗来源；Service 不解析酷狗业务或导入文件，未接入来源返回类型化错误。
- 将高频进度与常规播放状态分流，避免整页高频重组。
- 使用 Room 原子保存队列、当前项、位置和模式；冷启动只恢复为暂停状态。
- 实现 Media3 playback resumption，只有系统媒体入口或用户操作可以恢复播放。
- 在现有 Design System Showcase 中增加工程实验台，不提前实现正式播放器页面。

## 已确定行为

- 播放模式固定为顺序、列表循环、单曲循环和随机；默认列表循环。
- 建立及追加队列按稳定媒体 ID 去重；“下一首播放”会移动已有项目而非复制。
- 删除当前项目时优先播放下一项，删除末项时切换到上一项，空队列停止播放。
- 连续错误最多自动跳过六项，成功播放或手动选歌后重置错误计数。
- 领域模型只保存稳定来源标识，不保存 Android URI、绝对路径或临时在线播放地址。
- App 冷启动、Activity 重建、Service 重建和设备重启均不得自动出声。
- 阶段内测试媒体由仓库中的确定性 WAV 生成器在 App 专属目录创建，三个逻辑媒体项复用同一段 12 秒原创旋律，不访问网络或用户文件。

## 模块边界

- `:playback` 暴露 Controller、状态流、来源解析和快照存储端口。
- `:data` 实现播放快照存储端口，使用 `:core:database` 的 Room DAO。
- `:feature:*` 通过各自 ViewModel 使用 Controller，不直接访问 MediaController 或 Service。
- Service 只持有运行时播放状态，不解析酷狗业务或执行本地音乐导入。

## 测试

- 队列去重、插播移动、四种模式映射和六首连续错误上限使用纯 JVM 单测。
- Snapshot 映射、顺序、位置、模式、坏字段与未知来源使用 Fake DAO 单测。
- Room v1 的原子替换和清空使用真机内存数据库测试。
- Compose 测试覆盖连接、载入队列、播放/暂停、通知和窄屏滚动可达性。
- MediaSession、后台通知、冷启动暂停恢复和重启后的媒体按钮主动恢复使用设备测试。

## 实现结果

- `MoeKoePlaybackService` 是唯一创建 ExoPlayer 与 `MediaLibrarySession` 的组件，并由 Hilt 管理依赖。
- `Media3PlaybackController` 将普通状态与 500ms 播放进度分流，通过 `StateFlow` 提供给 ViewModel。
- 默认列表循环；顺序、单曲循环和随机模式映射到 Media3 repeat/shuffle 行为。
- 队列和快照由 Room v1 原子保存；暂停、切歌、模式和队列变化立即保存，播放位置按 5 秒检查点保存。
- 冷启动恢复队列、索引、位置和模式但保持暂停；`onPlaybackResumption` 仅响应系统或用户主动播放入口。
- Design System Showcase 保留原有内容，并增加可操作的播放内核工程实验台；未创建正式播放器页面。
- Manifest 仅新增媒体播放前台服务、MediaButtonReceiver 和必要的前台服务/WakeLock 权限，没有提前声明本地导入 Intent 或媒体读取权限。

## 设备验收记录

| 环境 | 结果 | 覆盖内容 |
| --- | --- | --- |
| API 26 ARM64 Google APIs AVD | 通过 | 全新数据安装、连接、队列、播放/暂停、MediaStyle 通知与窄屏滚动 |
| Huawei ELE-AL00，API 29 | 通过 | 真机播放、退到后台、活动媒体通知、Activity 冷启动恢复为暂停、Room 事务测试 |
| API 36 ARM64 Google APIs AVD | 通过 | 全新数据 UI 测试、前台媒体服务、系统媒体会话、模拟器重启和硬件媒体键主动恢复 |

API 36 重启后，Activity 未启动时系统仍保留 MoeKoe 的 MediaButtonReceiver；`KEYCODE_MEDIA_PLAY` 成功创建 `mediaPlayback` 前台服务，从保存队列和位置继续播放。未重启用户的 Huawei 手机。

## 完成标准

- App 管理的测试音频可在退到后台、锁屏后继续播放。
- 通知栏、耳机和系统媒体按钮状态一致。
- Activity 重建不创建第二个 ExoPlayer。
- 冷启动恢复队列、位置和模式但保持暂停；主动媒体恢复可以从快照继续。
- 队列与模式强制单测全部通过。

以上条件已在本阶段测试矩阵中通过。阶段 3 可直接复用 `PlaybackSource.ImportedLocal` 与 Snapshot 边界，不得回退为临时外部 URI 播放。
