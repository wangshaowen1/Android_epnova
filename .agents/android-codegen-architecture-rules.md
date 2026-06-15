# Android 架构与业务规则

快速入口见 `android-codegen-quick-rules.md`。普通 ViewModel 字段、点击状态、Toast、简单接口调用或局部状态同步不读取本文件；只有架构边界不确定、Repository 是否新增不确定、接口/模型放置不确定、异步/安全/UseCase/MVI 判断不确定，或用户要求架构审查时才读取本文件。

## ViewModel

- ViewModel 负责页面状态、用户事件处理、调用 Repository、调度协程和处理页面业务。
- ViewModel 不直接依赖 Activity、Fragment、View。
- 简单网络请求可以沿用现有项目风格，通过 `mRepo.api.xxx(...)` 配合 `response(...)`、`request(...)` 或 `requestWithLoading(...)` 调用。
- 当同一业务存在多处复用、包含 IO 切换、数据库组合、缓存策略或多接口组合时，在对应 feature 下新增 Repository 方法，由 ViewModel 调用 `mRepo.xxx(...)`。
- 不直接创建 `ApiService` 或 `AppDatabase`，统一通过 `mRepo.api` 和 `mRepo.appDatabase`。
- 页面状态优先使用 `MutableLiveData`、`ObservableField`、`ObservableBoolean`、`ObservableInt`，和现有 XML DataBinding 风格保持一致。
- 已有 Paging、SSE、播放器状态等 Flow 场景可以使用 `StateFlow` / `Flow`。
- Paging 使用 `Pager(...).flow.cachedIn(viewModelScope)`。
- 轮询、SSE、下载、支付监听等长任务必须保存 `Job` 或 handler，并在 `onCleared()` 中取消。
- 避免为了“不可变暴露”大规模改造已有 `MutableLiveData` 公有字段。新增复杂状态可以采用私有 `MutableLiveData` + 公开 `LiveData`，但以不破坏项目一致性为准。

## Repository

- 通用仓库继承 `AppRepository` 或 `CommonRepository`。
- 功能仓库放在对应 feature 下，例如 `feature/<domain>/<Domain>Repository.kt`。
- Repository 可以调用 `api`、`appDatabase`、MMKV、SharedPreferences、DataStore 或文件缓存。
- Repository 返回类型优先保持 `ApiResponse<T>` 或项目已有业务模型，不为单个新功能单独引入 `AppResult<T>`。
- 需要 IO 切换时，Repository 方法可以使用 `withContext(Dispatchers.IO)`。
- API 错误处理优先交给 `response(...)`、`request(...)`、`requestWithLoading(...)`，不要在 Repository 中随意吞异常。
- 只有当错误文案、重试策略、缓存降级等属于数据层职责时，才在 Repository 中封装。

## API 与模型

- Retrofit 方法统一写在 `core/network/apis/ApiService.kt`。
- API 路径常量写在 `ApiConstants.kt`。
- 请求体写在 `core/network/apis/request/`。
- 响应聚合模型写在 `core/network/apis/response/`。
- 接口实体写在 `core/network/apis/entity/`。
- Room Entity 写在 `core/db/entity/`。
- Room Dao 写在 `core/db/dao/`，并在 `AppDatabase` 中注册。
- 数据库模型和接口/UI 模型有差异时，在 `core/db/mapper/` 或 feature 内新增 mapper。
- 接口实体可以直接用于简单 UI 展示，但禁止加入 UI 专用状态字段，例如 `isSelected`、`isExpanded`、`isEditing`、`localProgress`、`displayText`、`errorMessage`、`isChecked`。
- 列表 item 需要本地 UI 状态时，优先使用 `ItemXxxVM` 包装接口实体，或新增 `XxxUiModel`。

ViewModel 默认可以通过 `mRepo.api.xxx(...)` 配合 `response(...)`、`request(...)` 或 `requestWithLoading(...)` 处理简单请求，并优先沿用当前 feature 附近同类写法。

只有出现以下情况时，才在对应 feature Repository 中新增方法，由 ViewModel 调用 `mRepo.xxx(...)`：

- 同一业务请求或接口组合被多个 ViewModel 复用。
- 需要组合多个接口、数据库、缓存、文件、MMKV 或 DataStore。
- 需要统一 IO 切换、复杂数据转换、错误降级、重试或兜底数据。
- 请求构造明显变复杂，或需要隔离第三方 SDK / 数据层细节。

## 异步与错误处理

- 无 loading 的网络请求：`request({ mRepo.api.xxx(body) }) { ... }`
- 用户触发且需要 loading 的请求：`requestWithLoading({ mRepo.api.xxx(body) }) { ... }`
- 非网络本地协程：`launch { ... }`
- 带 loading 的非网络本地协程：`launchWithLoading { ... }`
- 简单接口请求可沿用附近写法：`launch { response(mRepo.api.xxx(body)) { ... } }`、`request(...)` 或 `requestWithLoading(...)`。
- 需要状态回滚、自定义异常提示或 `savingIds`、loading/progress 等必然清理时，使用 `launch(block = { response(...) }, onError = { ... }, onComplete = { ... })`。
- `response.error` 处理接口业务失败，例如后端返回非成功码、回滚页面状态、展示后端错误文案。
- `launch.onError` 处理协程或网络异常，例如异常回滚、`parseMessage(e)?.showToast()`。
- `launch.onComplete` 处理必然清理逻辑，例如移除 `savingIds`、复位 loading/progress 标记。
- 不要在 `launch` 内手写 `try/catch/finally` 包网络请求，除非确有特殊原因；如必须手写，`CancellationException` 必须放行。
- 轮询：`launchPolling(...)`
- 后端任务状态检查优先使用 `launchPolling(...)`，保存返回的 `Job` 并在 `onCleared()` 取消。
- SSE 流式生成优先使用 `SSEStreamHandler`，启动新流前取消旧 handler。
- 下载/导出进度优先复用已有下载任务、下载弹窗和状态持有类；类名以当前分支实际实现为准。
- 所有长任务必须有重复启动保护，并在成功、失败、异常、取消后复位 loading/progress/generating 等 UI 状态。
- `CancellationException` 必须放行，不要吞掉协程取消。
- 接口失败默认通过 `response(...)` 的 error 分支或 `requestWithLoading(showToastError = true)` 处理。
- UI 只展示用户能理解的 Toast/Dialog/状态页，不展示原始堆栈。

## 业务注释

- 用户明确要求“加注释”“说明原因”或“写清楚边界”时，注释是验收项，必须落实；涉及业务口径、特殊兼容、局部开关、反直觉实现、非通用算法时，在代码旁说明为什么这么做和作用边界，不只靠方法名表达。
- ViewModel/Repository 中的复杂状态映射、后台任务阶段判断、重试/兜底/兼容逻辑、SDK/渠道/审核模式分支必须在关键分支附近补 1-2 行中文注释，说明业务原因。
- 轮询、SSE、下载、支付、登录、授权、广告奖励等高风险流程，注释重点写触发条件、状态含义、失败/取消为什么这样处理。
- 不为普通 getter/setter、简单赋值、明显点击转发、常规 `request/requestWithLoading` 写模板注释。

## UseCase 与 MVI

- 默认不生成 UseCase，也不生成完整 MVI。
- 只有同一业务逻辑被多个 ViewModel 复用、需要组合多个 Repository、规则计算明显复杂或需要独立测试时，才新增 UseCase。
- 只有页面状态很多、事件多且异步结果交叉影响 UI、或 Flow/SSE/Paging/轮询混合后 LiveData 难以维护时，才新增轻量 UiState。
- 不要生成 Store、Reducer、Intent、Effect 全套结构，除非用户明确要求。

## 安全与日志

硬编码 token/secret/key、敏感字段日志输出、打印完整 request/response 的红线统一见 `AGENTS.md`《关键禁令》。本文件只补充数据层相关:

- UI 不展示原始异常堆栈,只展示用户能理解的 Toast/Dialog/状态页。
- 涉及支付、登录、授权、广告奖励发放时,不能只依赖客户端状态作为最终结果,以服务端校验为准。
