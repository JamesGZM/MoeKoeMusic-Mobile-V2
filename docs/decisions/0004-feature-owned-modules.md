# ADR-0004：Feature 所有权与模块边界

- 状态：Accepted
- 日期：2026-08-05
- 取代：ADR-0002 中“所有 Feature 暂存单一 `:features` 模块”的部分

## 背景

阶段 3 后，综合 `:features` 模块已经同时包含首页、发现、我的、搜索、本地音乐、设备扫描、Design System 实验台、播放壳组件和跨业务 `MoeKoeAppViewModel`。导航、页面状态与播放状态出现多个独立变化原因，原 ADR 的拆分条件已经满足。

固定参考和取舍见 [`../reference-audits/08-feature-modularization.md`](../reference-audits/08-feature-modularization.md)。

## 决定

- 按业务能力建立 `:feature:home`、`:feature:discover`、`:feature:my`、`:feature:search`、`:feature:localmusic` 和 Debug 专用 `:feature:foundation`。
- `:app` 是唯一组合根，持有 NavHost、应用壳播放器状态、MiniPlayer 和 Queue Sheet。
- Feature 自己拥有导航键、导航注册、Route、Screen、ViewModel 和测试。
- 删除跨业务 `MoeKoeAppViewModel`；本地音乐状态归 `LocalMusicViewModel`，应用壳播放状态归 `AppPlaybackViewModel`。
- 当前不为每个 Feature 增加空的 `api/impl` 双模块；达到明确复用或编译隔离需求后再拆分。

## 后果

- Feature 可以独立编译、测试并限制实现可见性，新增登录和首页真实业务时不会继续扩大一个共享文件。
- Gradle 模块数量增加，构建配置和依赖声明略有重复；后续重复稳定后再提取 Feature Convention Plugin。
- `app` 仍直接依赖各 Feature 的少量导航公共 API，这是当前规模下有意保留的简单组合方式。
