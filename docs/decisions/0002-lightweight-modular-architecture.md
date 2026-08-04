# ADR-0002：轻量模块化架构

- 状态：Accepted
- 日期：2026-08-04

## 背景

第一版计划对齐 PC 端主要功能，业务范围包含发现、搜索、详情、播放、歌词、账号、音乐库、云盘、MV、本地音乐和识曲。

热门开源音乐项目常见单一大型 App 模块和超大型播放 Service；Now in Android 则采用适合大型团队的细粒度模块。两种极端都不适合项目初期。

## 决定

采用 MVVM、UDF、Repository 和少量稳定 Gradle Module：

- `:app`
- `:core:model`
- `:core:common`
- `:core:designsystem`
- `:core:database`
- `:kugou-api`
- `:data`
- `:playback`
- `:features`

Feature 初期在单模块内按 package 组织，达到明确拆分条件后再独立模块化。

## 理由

- API、播放器、数据库和设计系统是稳定且高价值的物理边界。
- Feature 变化频繁，过早拆分会增加 Gradle 配置和跨模块样板。
- 保留未来并行开发与增量构建的演进空间。
- 比复制大型样板更适合小型开源团队。

## 代价

- `:features` 需要通过代码审查维持 package 边界。
- 初期不能获得每个 Feature 独立编译的全部收益。
- 后续拆分需要调整依赖和可见性。

## Feature 拆分条件

满足以下两项以上时考虑拆分：

- 构建时间明显受该功能影响。
- 功能具有独立维护者或发布节奏。
- 被多个入口或平台复用。
- 与其他 Feature 的依赖可以形成清晰单向边界。
- 当前 package 已频繁产生跨功能误用。
