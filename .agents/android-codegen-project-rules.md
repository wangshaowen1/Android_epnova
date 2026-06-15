# Android 项目基础规则

红线禁令统一见根目录 `AGENTS.md`《关键禁令》;标准任务快速入口见 `android-codegen-quick-rules.md`。**本文件单一职责:项目识别、技术基线、目录结构、Base 类。**

## 项目识别

- 当前已核实：namespace / applicationId / Manifest package 为 `com.kuaigeglobal.epnova`，源码主包为 `app/src/main/java/com/kuaigeglobal/epnova`，应用名为 `EpNova`。
- 当前 Manifest scheme 为 `epnovascheme`，OneLink host 为 `epnova.onelink.me`；涉及外部跳转时同步查 `AppRouter`。
- flavor、依赖、Manifest 声明和资源目录仍以当前分支文件为准，不沿用复制来源项目的名称、包名、scheme 或业务域。
- 识别入口:`config.gradle`(applicationId)、`app/build.gradle`(namespace)、`app/src/main/AndroidManifest.xml`(Application 类 / scheme / host)、`app/src/main/java/com/kuaigeglobal/epnova`(实际包目录)、`settings.gradle`(模块)、`app/src/main/res`(资源)。
- feature 业务域以 `app/src/main/java/com/kuaigeglobal/epnova` 下的实际子目录为准;新增代码优先落在已有业务域或其子包下。
- 新增包路径前先用 `rg --files app/src/main/java/com/kuaigeglobal/epnova` 找到当前主业务包路径,优先放进已有 `feature/`、`core/`、`common/`、`router/`、`widget/` 等目录体系。
- 文档或注释中如出现复制来源项目的旧项目名、旧包名、旧业务域痕迹,不直接照搬;先用 `rg` 确认当前分支是否真实存在对应文件或同类实现。
- 仓库可能存在海外版与中国大陆版差异。支付、登录、推送、广告、统计、归因、设备标识、合规弹窗等能力,必须按当前分支已有依赖和封装实现。
- 多语言目录按当前仓库已有 `values*/strings.xml` 和产品要求同步,不套用其他项目的语言目录。

## 技术基线

- 架构：MVVM + Repository，基于 `nhsd.basic`。
- UI：XML + DataBinding/ViewBinding。
- 语言：Kotlin 为主，保留少量历史 Java。
- 网络：Retrofit + `ApiResponse<T>`。
- 数据库：Room，入口通常为 `core/db/AppDatabase.kt`。
- 协程：优先使用项目已有 `launch`、`request`、`requestWithLoading`、`launchPolling`。

## 禁止项

红线禁令(Hilt/di/UseCase/MVI、接口实体 UI 字段、网络层/SDK 隔离、安全日志、破坏性迁移、`OssManager`、不新增多 Module/不改 `ApiResponse.java` 等)统一见根目录 `AGENTS.md`《关键禁令》,本文件不重复。Hilt 注解层面的红线包括禁止生成 `@Inject`、`@HiltViewModel`、`@AndroidEntryPoint`、`@Module`、`@InstallIn`、`@Provides`、`@Binds`。

## 目录规则

- 新增功能优先放在 `feature/<业务域>/` 下，和现有页面保持同一风格。
- `base/`：项目基础 ViewModel、ItemVM、RecyclerViewVM 等，不随意扩展。
- `common/`：通用 adapter、扩展函数、工具类、权限、LiveData 等。
- `core/network/apis/ApiService.kt`：Retrofit API 定义。
- `core/network/apis/request/`：请求体。
- `core/network/apis/response/`：接口响应模型，偏聚合数据。
- `core/network/apis/entity/`：接口实体模型，当前项目中也会被页面直接使用。
- `core/db/dao/`：Room Dao。
- `core/db/entity/`：Room Entity。
- `core/db/mapper/`：数据库模型转换。
- `core/data/CommonRepository.kt`：通用仓库基类。
- `core/manager/`：第三方 SDK、广告、支付、统计、登录、推送、归因、设备标识等平台能力隔离。
- `router/`：页面路由和跳转。
- `widget/`：自定义 View、Dialog、状态组件。
- 目录不存在时先查找当前分支等价目录，不要机械新建另一套平行结构。

## Base 类

- Activity 直接继承 `com.nhsd.basic.BaseViewModelActivity<VB, VM>`。
- Fragment 直接继承 `com.nhsd.basic.BaseViewModelFragment<VB, VM>`。
- 普通 ViewModel 继承 `AppViewModel`。
- 需要仓库的 ViewModel 继承 `AppRepositoryViewModel<T>`。
- `AppViewModel.id()` 和 `AppRepositoryViewModel.id()` 已固定返回 `BR.layout`，不要重复实现。
- 列表 item VM 继承 `AppItemViewModel<T>`，其 DataBinding 变量名固定为 `item`。
- 页面中直接使用基类注入对象：`binding`、`mRealVM`、`mRepo`、`mBundle`。
