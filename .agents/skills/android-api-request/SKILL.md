---
name: android-api-request
description: "Android Retrofit 接口链路接入规范。仅在新增接口，或本次新增、修改、审查 ApiService、request/response model、Repository、ViewModel 中至少两层，或调整 response/request/requestWithLoading 错误语义时使用。当前分支已有完整同类链路且只补明确字段、参数或单一调用点时走 quick rules 局部接口快路径；只改 Toast、loading 文案或普通 UI 状态时不要使用。"
---

# Android 接口请求

## 配套读取

- 已有完整同类链路且只补字段、参数或单一调用点时，改走 `.agents/android-codegen-quick-rules.md` 的局部接口快路径。
- 实际同时新增页面、分页或长任务时，才追加对应 skill；SSE、轮询和下载的生命周期归 `android-async-task`。
- 只有找不到附近同类、需要确认 helper 或真实落点时，才读取 [项目接口索引](references/project-examples.md)。

## 契约证据

1. 接口路径、方法、字段、类型、可空性和枚举值必须来自用户资料、当前接口文档或当前代码事实，不能从 UI 文案和相似旧模型猜测。
2. 用户给出的新契约与当前代码冲突时，先指出差异和兼容范围，不静默覆盖。
3. 本地证据足够时直接实现；证据不足且项目已有接口查询能力时只查目标 endpoint。仍无法确认的字段保留为待确认，不借用相似业务补齐。

## 项目链路

- 路径常量：`core/network/apis/ApiConstants.kt`。
- Retrofit 方法：`core/network/apis/ApiService.kt`。
- 请求体：`core/network/apis/request/`；聚合响应：`response/`；接口实体：`entity/`。
- 返回类型保持 `ApiResponse<T>`。
- ViewModel 通过 `mRepo.api` 或确有复用价值的 feature Repository 调用。
- 错误处理沿用 `response(...)`、`request(...)`、`requestWithLoading(...)`。
- 新增完整接口或模型时按已确认契约补中文业务注释；局部字段修改沿用当前文件风格，不编造字段含义。

## 接入顺序

1. 用业务词、路径、常量名、方法名和模型名搜索当前链路，优先复用真实类型。
2. 在 `ApiConstants.kt` 相近区域增加路径常量。
3. 仅在现有类型不能表达契约时新增 request/response/entity。
4. 在 `ApiService.kt` 增加返回 `ApiResponse<T>` 的方法。
5. 在 ViewModel 调用；只有命中下方条件时增加 feature Repository。

## 请求 helper 选择

| 场景 | 使用 |
| --- | --- |
| 页面初始化、静默刷新、普通并行请求 | `request(...)` 或附近的 `launch { response(...) }` |
| 保存、提交、删除、重试等用户操作 | `requestWithLoading(...)` |
| 已处于组合协程且需要 loading | `launchWithLoading { response(...) }` |
| 需要回滚、异常提示或必然清理临时状态 | `launch(block, onError, onComplete)` |

- `response.error` 处理接口业务失败；`launch.onError` 处理网络/协程异常；`launch.onComplete` 清理 `savingIds`、loading、progress 等临时状态。
- 不在普通 `launch` 中再包一层 `try/catch/finally`；确需捕获时必须放行 `CancellationException`。
- Activity/Fragment 不直接发起网络请求。

## Repository 边界

默认直接使用 `mRepo.api.xxx(...)`。仅在以下任一情况新增 feature Repository 方法：

- 同一请求或接口组合被多个 ViewModel 复用。
- 需要组合 API、数据库、缓存、文件或本地存储。
- 需要统一 IO 切换、数据转换、缓存降级、重试或第三方 SDK 隔离。
- 请求构造已经成为需要独立测试或复用的业务逻辑。

Repository 不吞掉普通 API 异常，仍交由现有响应 helper 处理。

## 模型与错误

- `XxxBody` / `XxxPagedBody` 放 request；`XxxInfo` 放 entity；`XxxData` 放 response。
- `PagedList<T>` 用于分页；`ListData<T>` 用于普通列表；确实不消费返回体时才使用 `Any?`。
- 后端字段默认保持当前 JSON 命名；只有冲突或不符合 Kotlin 标识符时才按项目现有序列化方式映射。
- 成功码、`ApiException`、登录态事件和默认错误提示以当前 `ApiResponse`、`Response.kt`、`ViewModelExt.kt` 为准，不在 skill 固化易漂移数值。
- 不散落 `response.code == ...` 判断；统一走项目响应 helper。

SSE 接口声明可返回 `Flow<SSEvent>`，其处理器与取消交给异步 skill。分页返回进入 `android-paging-state-ui`，不要用普通列表请求手拼页码。

## 检查

- 契约字段都有证据，冲突和缺口已说明。
- 已搜索并复用常量、方法、Body 和模型，目录与返回类型符合当前链路。
- 用户操作与静默请求选择了正确 helper，失败没有被吞掉。
- ViewModel 通过 `mRepo.api` 或必要 Repository 调用，页面层没有直接请求。
- 没把分页、SSE 或轮询误写成一次性请求。
