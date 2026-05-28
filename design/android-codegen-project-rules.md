# Android 项目基础规则

完整总规范见 `android-codegen-guidelines.md`，快速入口见 `android-codegen-quick-rules.md`。

## 项目识别

- 主业务工程、namespace、applicationId、源码根包、flavor、依赖、Manifest 声明和资源目录，以当前分支文件为准，不使用复制来源项目的名称或包名。
- 常用识别入口：`settings.gradle`、`app/build.gradle`、`app/src/main/AndroidManifest.xml`、`app/src/main/java`、`app/src/main/res`。
- 新增代码默认放在已有主业务工程；除非用户明确要求工程重构，不新增 Gradle Module。
- 新增包路径前先通过 `rg --files app/src/main/java` 找到当前主业务包路径，优先放到已有 `feature/`、`core/`、`common/`、`router/`、`widget/` 等目录体系中。
- 当前仓库可能有海外版和中国大陆版差异。支付、登录、推送、广告、统计、归因、设备标识、合规弹窗等能力必须按当前分支已有依赖和封装实现。
- 多语言目录按当前仓库已有 `values*/strings.xml` 和产品要求同步，不固定套用其他项目的语言目录。

## 技术基线

- 架构：MVVM + Repository，基于 `nhsd.basic`。
- UI：XML + DataBinding/ViewBinding。
- 语言：Kotlin 为主，保留少量历史 Java。
- 网络：Retrofit + `ApiResponse<T>`。
- 数据库：Room，入口通常为 `core/db/AppDatabase.kt`。
- 协程：优先使用项目已有 `launch`、`request`、`requestWithLoading`、`launchPolling`。

## 禁止项

- 不要把本项目当成纯 ViewBinding 小项目处理。页面 XML 已普遍使用 `<layout>`、`layout` 变量、`item` 变量和自定义 `BindingAdapter`。
- 不要新增 `AppContainer`、`ViewModelFactory` 或 `di` 包。当前 `nhsd.basic` 已负责 `binding`、`mRealVM`、`mRepo`、`mBundle` 等注入。
- 不要新增 Hilt 相关代码。禁止生成 `@Inject`、`@HiltViewModel`、`@AndroidEntryPoint`、`@Module`、`@InstallIn`、`@Provides`、`@Binds`。
- 不要为了“标准 Clean Architecture”新增 `domain`、`UseCase`、`AppResult`、`AppError`。
- 不要默认把接口实体再包装成 Domain Model / UiModel。
- 不要修改 `ApiResponse.java`，除非用户明确要求做全局网络层改造。
- 不要新增多 Gradle Module，除非用户明确要求重构工程结构。
- 不要在 Activity/Fragment 中直接请求网络或访问数据库。
- 不要在 Adapter 中写业务请求。
- 不要在业务代码里直接散落第三方 SDK 调用。
- 不要硬编码 token、secret、api key、私钥。
- 不要在日志中输出 token、手机号、邮箱、用户 ID、订单号、支付参数、授权 code。
- 不要检查或修改 `OssManager.kt`，除非用户明确要求。

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
