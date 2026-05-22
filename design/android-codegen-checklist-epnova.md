# Epnova Android 代码审查 Checklist

本文档用于生成前后自查和代码审查输出。总规则见 [android-codegen-guidelines-epnova.md](android-codegen-guidelines-epnova.md)，场景细则见 [android-codegen-scenarios-epnova.md](android-codegen-scenarios-epnova.md)。

## 生成前检查

开始写代码前先判断：

1. 这个功能已有相似页面、仓库、Adapter、Dialog、BindingAdapter 或 manager 吗？优先复用同类写法。
2. 当前任务是否命中项目 skill：页面骨架、API 请求、命名、分页、非分页列表、弹窗、导航事件、Figma UI、异步任务。
3. 新页面是否应该继承 `BaseViewModelActivity` / `BaseViewModelFragment`？
4. XML 是否需要 `<layout>`，变量名是否为 `layout` 或 `item`？
5. API 是否应加到 `ApiService.kt`，路径常量是否应加到 `ApiConstants.kt`，请求体是否放在 `request/`？
6. 是否真的需要新 Repository，还是 `AppRepository` / `CommonRepository` 已足够？
7. 是否真的需要 UiModel、UseCase、UiState？
8. 是否有长任务需要保存 `Job` 或 handler，并在 `onCleared()` 取消？
9. 是否新增了字符串资源并同步多语言文件？
10. 是否涉及 Room version、Migration 或 AutoMigration？
11. 是否涉及支付、登录、授权、广告奖励、统计等需要 SDK 隔离和安全校验的能力？
12. 是否误引入了 Hilt、AppContainer、ViewModelFactory、AppResult/AppError 等当前项目没有采用的结构？

## 生成后检查

完成代码后检查：

1. Activity/Fragment 只做 View 初始化、事件绑定、观察 ViewModel、跳转、Toast/Dialog/Loading 展示。
2. ViewModel 不直接依赖 Activity、Fragment、View，不直接创建 `ApiService` 或 `AppDatabase`。
3. Repository 不随意吞异常，不把 Retrofit `Response` 暴露给 UI。
4. XML 只写简单 DataBinding 表达式，复杂判断已放到 ViewModel 或 BindingAdapter。
5. Adapter 只负责展示和转发点击，没有直接发网络请求。
6. 新增 API、request、response、entity、dao、mapper 的目录符合总规范。
7. 列表 item 的 UI 临时状态没有写进接口实体。
8. 长任务有重复启动保护，并在成功、失败、异常、取消后复位 UI 状态。
9. `CancellationException` 没有被当成普通异常吞掉。
10. 新增资源命名符合命名 skill，尤其是 `bg_透明值_颜色值_r/tr/br_radius值` 和渐变命名。
11. 新增文案已同步 `values`、`values-zh`、`values-th`、`values-in`。
12. Figma/截图 UI 已优先映射项目 `style`、`theme`、`attr`、`color`、`dimen`、`drawable`、`selector`。
13. 文案控件未因写死宽高导致多语言截断风险。
14. 支付、登录、授权、广告奖励发放等没有只依赖客户端状态作为最终结果。
15. 未查看或修改 `OssManager.kt`，除非用户明确要求。

## 禁止误引入项

发现以下内容必须回退或明确说明原因：

- `@Inject`
- `@HiltViewModel`
- `@AndroidEntryPoint`
- `@Module`
- `@InstallIn`
- `@Provides`
- `@Binds`
- 新增 `AppContainer`
- 新增 `ViewModelProvider.Factory`
- 新增 `di` 包
- 新增 `AppResult`
- 新增 `AppError`
- 为单行调用新增 UseCase
- 为简单页面新增完整 MVI Store/Reducer/Intent/Effect
- 为普通分页默认新增 `RemoteMediator` / `VideoRemoteMediator`
- Activity/Fragment 直接请求网络或访问数据库
- Adapter 直接请求网络
- 接口实体中新增 UI 临时状态字段
- 业务代码直接 import 第三方 SDK 类
- `fallbackToDestructiveMigration`
- 硬编码 token、secret、api key、私钥
- 线上日志输出敏感信息

## 风险提示格式

如果生成或审查时发现风险，按以下格式输出，优先列风险再列改动摘要：

```text
风险：
1. [严重级别] 文件路径:行号 - 问题描述。
   影响：说明可能导致的行为或维护风险。
   建议：给出最小修改建议。

需要确认：
- 无法从当前代码判断的点。

改动摘要：
- 本次新增/修改了什么。

验证：
- 已运行的检查或未能运行的原因。
```

严重级别：

- `高`：会导致崩溃、数据丢失、安全问题、支付/登录/广告奖励错误、数据库破坏性迁移。
- `中`：会导致业务流程错误、状态不同步、重复请求、分页错乱、UI 明显异常。
- `低`：命名、组织、重复代码、轻微可维护性问题。

## 输出清单

生成代码后必须说明：

1. 新增/修改文件列表。
2. 复用了哪些项目已有类、方法、BindingAdapter、style、drawable。
3. 是否新增 API、request、response、entity、dao、mapper。
4. 是否新增 Repository，以及原因。
5. 是否新增 UiModel、UiState、UseCase，以及原因。
6. 是否涉及 Room version 或 Migration。
7. 是否涉及多语言 string。
8. 是否涉及长任务，以及在哪里取消。
9. 是否存在需要人工确认的点。

## 推荐生成模式

简单页面：

- `FeatureActivity/Fragment`
- `FeatureViewModel : AppRepositoryViewModel<AppRepository>()`
- XML `<layout>` + `layout` 变量
- 必要时新增 request body / response model / entity

中等复杂页面：

- `FeatureActivity/Fragment`
- `FeatureViewModel : AppRepositoryViewModel<FeatureRepository>()`
- `FeatureRepository : AppRepository()` 或 `CommonRepository()`
- ItemVM + QuickAdapter 或 Paging3
- 必要时增加轻量 UiState

复杂异步页面：

- 保持 MVVM
- 明确拆分 Repository、状态对象、轮询/SSE handler、进度模拟器
- Job 和 handler 在 `onCleared()` 统一释放
- 只在复用或业务规则复杂时增加 UseCase
