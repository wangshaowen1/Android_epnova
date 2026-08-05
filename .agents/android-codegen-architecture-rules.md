# Android 架构与业务规则

只有跨层所有权不清楚、Repository/模型放置存在选择，或用户要求架构审查时读取。一次性接口、分页、长任务和列表细则分别路由到对应 skill，不在本文重复。

## ViewModel 与页面

- 页面层负责 View 初始化、观察、导航和展示；业务状态与请求调度进入 ViewModel。
- 只需要参数和本地页面状态时使用 `AppViewModel`；需要 API、数据库或仓库能力时使用 `AppRepositoryViewModel<AppRepository>` 或已有 feature Repository。
- ViewModel 通过 `mRepo.api` / `mRepo.appDatabase` 访问项目能力，不自行创建服务或数据库实例。
- 状态类型按消费方式选择：XML/观察者需要刷新才用 `MutableLiveData` / `Observable*`；已有 Paging/SSE/连续数据链路才使用 Flow。
- 不为了不可变暴露而大规模改造已有公开 `MutableLiveData`；新增复杂状态可在不破坏当前风格时私有可写、公开只读。

## Repository 决策

简单请求可由 ViewModel 沿用附近 `mRepo.api + response/request/requestWithLoading`。只有以下情况新增 feature Repository 方法：

- 同一业务被多个 ViewModel 复用。
- 需要组合 API、数据库、缓存、文件或本地存储。
- 需要统一 IO、复杂映射、错误降级、重试或第三方能力隔离。

Repository 返回当前项目已有响应/业务模型，不随意吞异常；只有缓存降级、重试和兜底确属数据层职责时在此封装。

## 模型边界

- API request/response/entity、Room entity/dao/mapper 的目录以当前项目结构和 [API Skill](skills/android-api-request/SKILL.md) / [场景路由](android-codegen-scenarios.md) 为准。
- 接口模型可直接用于简单展示；涉及本地选中、展开、编辑、进度等 UI 状态时，按 `AGENTS.md` 红线使用 ItemVM 或必要 UiModel。
- 不为单一页面制造只有字段透传的额外模型层。

## 异步所有权

- 页面 ViewModel 创建的轮询、SSE handler、临时下载和页面任务在 `onCleared()` 取消。
- View 拥有的播放器、动画和逐帧进度按真实 View 生命周期释放。
- Service、系统下载、应用级 Manager 或持久队列拥有的任务可跨页面继续，页面通过任务 ID 解绑/重绑观察。
- 具体 helper、重复启动、终态和错误清理见 [异步 Skill](skills/android-async-task/SKILL.md)。

## 业务注释

- 用户明确要求注释，或代码包含反直觉兼容、业务口径、状态机、重试/兜底、渠道/审核分支时，在关键分支旁用 1–2 行说明“为什么”和边界。
- 普通赋值、明显点击转发和常规 helper 调用不写模板注释。

安全、日志、全局架构禁令只以 `AGENTS.md` 为准。
