# Design System 公共组件审计

状态：已接受。审计日期：2026-08-07。

## 目标

在继续实现登录页之前，先审查当前全部已确认页面与组件设计稿，明确哪些 UI 能进入 `:core:designsystem`，哪些必须留在 Feature。公共组件的准入依据是可复用的产品语义、结构和交互约束，不是代码片段相似或视觉上像一个组件。

本审计不改变 Gradle 模块边界、不引入依赖，也不把页面设计坐标或登录业务状态搬入 Design System。

## 审计范围

- 核心页面确认稿 `01` 至 `10`。
- 通用组件板 `11`、`12`、`14` 至 `18`。
- 登录主稿 `13` 与 `login-v2` 单状态确认稿 `19a` 至 `22d`。
- 播放器歌词确认稿 `23a` 至 `23h`。
- 当前 `:core:designsystem`、`:app` 与各 Feature 中直接使用 Material 组件的实现。

旧版总览、撤销稿和历史原型不作为公共 API 的复用证据。

## 组件准入规则

候选能力按以下顺序判断：

1. 至少两个已确认页面或业务场景具有相同语义、结构和交互时，建立公共组件。
2. 当前只有一个消费者，但无障碍、反馈队列、焦点、系统栏或全局视觉约束必须一致时，可以建立窄公共 API，并记录约束来源。
3. 只负责页面排版、设计坐标映射或业务状态组合的内容留在所属 Feature。
4. 视觉相似但语义、状态机或操作优先级不同的内容不抽象。
5. 公共组件不得接收 ViewModel、Repository、业务枚举或页面布局规格；Feature 通过薄适配层映射文案、状态和事件。
6. 第二个消费者出现前，不为推测中的复用增加变体、参数或万能 Slot。

删除组件或让两个页面暂时重复，优于维护一个语义不清的通用容器。

## 模块与包边界

继续使用既有 `:core:designsystem`，不新建设计系统模块。公共组件按职责分包：

| 包 | 责任 | 禁止承载 |
| --- | --- | --- |
| `component.action` | Button、IconButton 及其稳定状态 | 登录提交、播放控制等业务动作 |
| `component.input` | 文本输入的外观、焦点、错误和可访问性 | 手机号、验证码、密码校验规则 |
| `component.navigation` | 标准、沉浸、搜索、多选等有真实消费者的 Toolbar | 页面路由和业务标题来源 |
| `component.overlay` | Dialog、Bottom Sheet 的模态外壳与操作布局 | 风控、退出等业务状态机 |
| `component.feedback` | Snackbar/Toast 的呈现、排队和遮挡规则 | 将持久错误降级成临时提示 |
| `component.state` | 加载、空数据、持续错误和离线页面状态 | 数据加载与重试策略 |
| `component.media` | 歌曲行、封面、徽标、MiniPlayer、队列行 | 播放器或数据层状态 |

迁移期间可保留原包的弃用转发入口，避免为包整理制造一次性全仓改动；新调用只使用职责包。

## 已确认的公共组件

| 候选 | 复用证据 | 决策 | 首批边界 |
| --- | --- | --- | --- |
| Button | 登录、搜索、本地音乐、我的、播放器及 Showcase 均有主要/次要操作 | 公共 | Filled、Tonal、Outlined、Text、Destructive；48/56dp；loading 不改变几何 |
| TextField | 登录、搜索、本地导入与 Dialog 输入 | 公共 | 标准与 Compact 尺寸；leading/trailing、password、error、disabled；校验留给 Feature |
| Toolbar | 搜索、本地音乐、设置、资料、歌单详情、播放器与登录 | 公共组合组件 | 仅为确认存在的标准、沉浸、搜索、多选语义提供独立 API；不做万能 Toolbar |
| Alert/Input Dialog | 退出、删除、登录风控、文本输入 | 公共模态外壳 | 标准标题/正文/操作和输入型结构；复杂内容使用受控内容 Slot |
| Bottom Sheet | 队列、排序/音质、多项选择 | 公共模态外壳 | 遮罩、顶部圆角、Insets、拖拽/返回与操作区 |
| Snackbar | 全局可恢复反馈和可选操作 | 公共 | Host、队列、去重、MiniPlayer 避让和最多两行 |
| Toast | 保存完成等低优先级且无操作的瞬时反馈 | 公共但后置 | Compose Host 可用时不调用系统 Toast；持久/可恢复错误不得使用 |
| PageState | 加载、空、离线、持续错误 | 公共 | 语义图标、文案和可选重试；页面决定数据状态 |
| Music content | 首页、搜索、本地音乐、歌单、播放器队列 | 保留公共 | 继续使用现有已验证组件，后续只整理包路径 |

Toolbar 采用多个窄入口，而不是把返回、搜索、折叠、多选和沉浸状态塞进一个布尔参数集合。是否新增某个 Toolbar 变体，仍需第二个消费者或明确的全局交互约束。

## 明确保留在 Feature 的内容

以下登录内容不进入公共组件：

- `LoginLayoutSpec` 及 `852 × 1846` 设计坐标映射；
- 登录 Hero、登录表单 Card 的位置和重叠关系；
- 验证码/密码/扫码模式的组合、协议说明和法律文案；
- 二维码生成、等待、确认、过期与失败状态；
- 多账号选择状态与风险验证状态机；
- 当前只在登录出现的模式分段控件。

登录可以保留 `LoginPrimaryButton`、`LoginTextField` 等薄适配函数，但它们只能选择公共组件的受支持变体并映射业务状态，不得重新绘制边框、圆角、阴影或加载行为。

## 暂缓与拒绝

- 不创建 `MoeUniversalCard`、`MoeUniversalToolbar` 或通过大量布尔参数覆盖所有设计的组件。
- 登录 Card 阴影是页面视觉契约；在跨页面表面层级审计完成前不建立任意 `MoeCardElevation`。
- Segmented control 虽出现在组件板，但当前已确认业务消费者只有登录，先留在 Feature。
- 风险 Dialog 不作为独立公共组件；它由公共 Dialog 外壳与登录业务内容组合。
- 不为简单的 `Row`、`Column`、间距组合或一次性插画建立公共组件。

## API 与实现约束

- 优先以 Material 3 提供语义、焦点、按压和无障碍，公共组件集中固定颜色、形状、尺寸和状态；Material 无法满足确认稿几何时，定制实现仍必须保留等价语义和最小 `48dp` 触控区。
- API 只暴露产品需要的参数和内容 Slot，不向 Feature 暴露任意颜色、圆角、阴影或内部 Padding。
- 默认尺寸来自 Token；确认稿确有两套稳定规格时使用具名 Size，不允许调用方传裸 `Dp`。
- 文案由调用方以资源字符串提供。组件不持有业务文案，也不读取导航或业务状态。
- Preview 和测试必须覆盖正常、按下/聚焦、加载、错误、禁用、深色、AMOLED 和大字体中适用的状态。

## 实施顺序

1. 建立 Button 与 TextField，迁移登录薄适配层，验证尺寸和状态不漂移。
2. 整理 Toolbar 到 `component.navigation`，先覆盖已有标准与沉浸式消费者，再按真实页面补搜索/多选。
3. 建立 Dialog 外壳并迁移登录风险、退出和删除确认。
4. 完成 Snackbar Host；Toast 只在明确的低优先级消费者出现时实现。
5. Bottom Sheet 与 PageState 随对应页面切片实现，不提前设计全部变体。

每一步独立测试、截图验证并形成原子提交；不得以批量重录截图掩盖与确认稿的偏差。

## 验证矩阵

- 组件级：Compose UI 语义测试、浅色/深色/AMOLED 截图、`1.0×`/`1.5×`/`2.0×` 字体。
- 消费页面：页面确认稿归一化叠加、IME/Insets、返回、焦点恢复和标准视口零意外滚动。
- 工程级：`spotlessCheck`、`:core:designsystem:testDebugUnitTest`、Design System 截图验证、受影响 Feature 测试与 Lint。
- 真机：只使用用户指定且已连接的设备；不创建或启动模拟器。

## 参考与采用结论

- [Android Custom design systems](https://developer.android.com/develop/ui/compose/designsystems/custom)：采用在 Material 基础上以窄包装组件固定产品约束；不暴露 Material 的全部可定制参数。
- [Android Compose app bars](https://developer.android.com/develop/ui/compose/components/app-bars)：采用 `title`、`navigationIcon`、`actions` 与滚动行为的语义分工。
- [Android Compose dialogs](https://developer.android.com/develop/ui/compose/components/dialog)：标准结构使用 `AlertDialog`，复杂结构使用 `Dialog` 与受控 Surface。
- [Now in Android `7d45eae4`](https://github.com/android/nowinandroid/tree/7d45eae4f8720a0c77f507712ba2437ff974b6ed/core/designsystem/src/main/kotlin/com/google/samples/apps/nowinandroid/core/designsystem/component)：采用按产品语义建立独立 Button/TopAppBar 包装函数和窄 API 的方式；不复制实现。
- [Compose Samples `84788c81`](https://github.com/android/compose-samples/tree/84788c81186acd5bf0d280100992c8a9c04120ad/Jetsnack/app/src/main/java/com/example/jetsnack/ui/components)：采用组件职责拆分和反馈 Host 的组织方式；不复制实现。

以上外部项目均为 Apache-2.0；本审计只采用架构与 API 设计原则，不引入源码或新依赖。
