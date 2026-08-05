---
title: App 分层架构
type: architecture
status: active
last_verified: 2026-08-05
---

# App 分层架构

## 背景与目的

EpNova 的主应用按 `base`、`common`、`core`、`kit`、`router`、`service`、`widget` 和 `feature` 等包组织。分层用于指导职责、依赖和新增代码归属，不以目录绝对纯净为目标。

## 范围与非目标

本文覆盖 `app/src/main/java/com/kuaigeglobal/epnova/` 下共享代码和功能代码的归属，以及 `baselibrary` 与本地库模块的边界。

本文不要求搬迁或清空 `app/base`，不启动新的 Gradle 模块化，也不要求仅为符合架构图而批量重写 import。代码位置以是否更容易理解、复用、测试和维护为判断依据。

## 当前模型

- `app` 通过 `modules.gradle` 和 `app/build.gradle` 使用 Maven 制品 `com.kuaige:baselibrary:1.1.0`。
- `settings.gradle` 当前包含主应用 `:app`，以及 `:matisse`、`:magicindicator`、`:datepicker` 三个本地库模块；这些库不是业务分层模块。
- `base`、`common`、`core`、`kit`、`router`、`service`、`widget` 和 `feature` 是 `app` 内的 Kotlin/Java 包，不是独立 Gradle 模块。
- `app/base` 是基于 `baselibrary` 的 App 级适配集合，现有列表 VM、Dialog 基类、分享 ViewModel 等仍有业务调用，不是待清空目录。
- 当前 `core`、`kit`、`router`、`widget` 中存在对具体 `feature` 的引用。路由层负责最终页面装配时允许依赖目标页面；其他共享能力绑定具体页面属于触及时优先评估的收敛点。

## 职责与边界

| 位置 | 负责 | 不负责 |
| --- | --- | --- |
| `com.kuaige:baselibrary` | Android/MVVM 基类、通用基础组件和集中封装的第三方能力 | EpNova 业务模型、应用资源和具体页面 |
| `base/` | 基于 `baselibrary` 的既有 App 级基类和适配代码 | 作为新架构层持续无边界扩张 |
| `common/`、`widget/` | 与具体业务无关的工具、扩展、Adapter 和 UI 组件 | 全局业务状态和完整功能流程 |
| `core/` | 全局数据源、网络、数据库、用户与配置状态及共享业务管理 | 具体 `feature` 页面的交互编排 |
| `kit/` | 可复用的领域能力；与 `core` 的边界按职责和复用方式判断 | 绑定某个具体业务页面 |
| `router/` | App 内外路由分发、登录拦截和最终页面装配 | 具体页面的请求、状态机和交互逻辑 |
| `service/` | 需要 Android Service 生命周期承载的后台能力 | 页面展示和页面级业务状态 |
| `feature/` | 面向用户的页面、交互和完整业务流程 | 向共享层泄漏页面实现 |
| 本地库模块 | 可独立复用的选择器、指示器和日期组件 | EpNova 业务流程 |

新增代码优先按现有职责放置。旧代码只在真实收益足以覆盖迁移和回归成本时调整。

## 治理优先级

| 优先级 | 判断条件 | 处理原则 |
| --- | --- | --- |
| 必须处理 | 循环依赖；非装配型共享能力绑定具体页面；ViewModel 接受或持有 View/生命周期对象；耦合导致无法复用或测试 | 优先消除方向性和职责风险，不以搬包代替解耦 |
| 顺手处理 | 类名或包名明显误导，或导致新增代码难以判断归属 | 在相关业务变更中渐进改名或迁移 |
| 不必处理 | 与理想架构图不完全一致，但职责清楚、依赖稳定且没有维护成本 | 保持现状，避免低价值的 import 重写 |

第三方 SDK 隔离等项目红线统一以 `AGENTS.md` 为准，本文不重复维护。

## `app/base` 的渐进治理

- `AppRepositoryViewModel` 等职责清楚且依赖稳定的基类可留在原处，不为命名清晰度单独批量迁移。
- `RecyclerViewVM`、Blur Dialog 基类和 `BaseShareViewModel` 当前均有真实调用；触达对应业务时按职责和依赖风险评估，不把它们当作无引用删除候选。
- 新增共享能力前先判断是否应进入 `common`、`core`、`kit` 或 `widget`，避免继续把 `base` 当作默认收纳目录。

## 关键决策

| 决策 | 原因 | 未采用方案及原因 |
| --- | --- | --- |
| 最底层基础能力使用 `baselibrary` 命名 | 与实际依赖制品一致 | `BaseModule` 或 `app/base` 会混淆制品与应用适配代码 |
| 保留 `app/base` | 它是仍有调用方的 App 级适配集合 | 整体迁入 `core` 或清空目录会制造高风险、低收益迁移 |
| 本地库与业务分层分开描述 | 三个本地库是可复用组件，不是业务层级 | 把所有 Gradle module 画进业务依赖链会误导代码归属 |
| 旧代码渐进治理 | 可结合业务验证降低回归成本 | 独立批量整理 import 难以产生对应业务价值 |

## 实现地图与验证

| 关注点 | 稳定代码锚点 | 验证方式 |
| --- | --- | --- |
| 构建模块 | `settings.gradle` | 核对实际 include 的模块 |
| `baselibrary` 依赖 | `modules.gradle`、`app/build.gradle` | 核对依赖坐标和使用方式 |
| App 分层包 | `app/src/main/java/com/kuaigeglobal/epnova/` | 检查跨包 import 和职责 |
| App 级适配 | `app/src/main/java/com/kuaigeglobal/epnova/base/` | 修改前检查调用方、反射、资源和构建变体引用 |

仅有包名差异不构成架构问题。处理必须级问题或迁移既有代码时，再按用户要求编译受影响变体并回归对应页面。
