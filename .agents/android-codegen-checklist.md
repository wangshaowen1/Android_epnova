# Android 代码审查 Checklist

本文档用于整页生成、复杂改动、用户要求全量审查或发布前审查。快速局部检查优先使用 [android-check-quick.md](android-check-quick.md)，不要为小改动读取完整 Checklist。红线禁令以根目录 `AGENTS.md`《关键禁令》为准，场景路由和高风险补充见 [android-codegen-scenarios.md](android-codegen-scenarios.md)。

## 使用范围

Checklist 按任务范围裁剪执行，不要求每次全量跑完；只有完整生成、复杂审查、用户明确要求全量审查或发布前检查时才读取本文件。

- 快速局部修改：只检查本次触达文件、直接引用、XML 解析和明显资源缺失。
- 标准代码生成：检查命中场景的相关条目，不读取或执行无关 skill。
- Figma UI 生成或还原：检查目标节点/目标 XML 的资源映射、drawable 命名、字体/行高/字距规则和直接资源引用；只有整页重写才做完整页面级检查。
- 用户明确说“不走 Gradle”“直接检查”时，不运行 Gradle，仅做静态检查并说明范围。
- 快速局部修改不执行“生成前检查”和“输出清单”的完整条目，只保留和本次触达代码直接相关的项目。

## 生成前检查

开始写代码前先判断：

1. 这个功能已有相似页面、仓库、Adapter、Dialog、BindingAdapter 或 manager 吗？优先复用同类写法。
2. 完整生成或复杂审查时，当前任务是否命中项目 skill：页面骨架、API 请求、命名、分页、非分页列表、弹窗、导航事件、Figma UI、异步任务；局部小改只读目标代码和 quick check。
3. 当前分支的主业务工程、namespace、源码根包、语言目录和 SDK 依赖是否已经确认？
4. 新页面是否应该继承 `BaseViewModelActivity` / `BaseViewModelFragment`？
5. XML 是否需要 `<layout>`，变量名是否为 `layout` 或 `item`？
6. API 是否应加到 `ApiService.kt`，路径常量是否应加到 `ApiConstants.kt`，请求体是否放在 `request/`？
7. 是否真的需要新 Repository，还是 `AppRepository` / `CommonRepository` 已足够？
8. 是否真的需要 UiModel、UseCase、UiState？
9. 是否有长任务需要保存 `Job` 或 handler，并在 `onCleared()` 取消？
10. 是否新增了字符串资源，并同步当前分支需要覆盖的 `values*/strings.xml`？
11. 是否涉及 Room version、Migration 或 AutoMigration？
12. 是否涉及支付、登录、授权、广告奖励、统计、归因、推送、设备标识等需要 SDK 隔离和安全校验的能力？
13. 是否误引入了 Hilt、AppContainer、ViewModelFactory、AppResult/AppError 等当前项目没有采用的结构？
14. 是否存在特殊渠道/审核模式/SDK 差异、登录/支付/授权/广告奖励、长任务状态映射、历史兼容或 UI 避让等需要保留业务原因的复杂分支，需要补必要中文注释？

## 生成后检查

完成代码后检查：

1. Activity/Fragment 只做 View 初始化、事件绑定、观察 ViewModel、跳转、Toast/Dialog/Loading 展示；Fragment 中 `onPrepareView()` 注册的 ViewPager/adapter/键盘/事件等手动回调默认在 `onDestroy()` 解除，不套用 `onDestroyView()`，除非同类或基类已有明确模式。
2. ViewModel 不直接依赖 Activity、Fragment、View，不直接创建 `ApiService` 或 `AppDatabase`。
3. Repository 不随意吞异常，不把 Retrofit `Response` 暴露给 UI。
4. XML 只写简单 DataBinding 表达式；接口实体已有展示字段时可直接绑定。复杂业务判断放到 ViewModel 或 BindingAdapter，但不要为了空判断、简单兜底、颜色/显隐三元表达式生成 `getXxxText()`、`titleText()`、`subtitleText()`、`isXxxVisible()` 等透传函数。
5. ViewModel public 状态/方法都被 XML、Fragment/Dialog 或外部调用方实际消费；只在 VM 内部使用的 helper 保持 `private`，未使用的展示属性/函数已清理。
6. Adapter 只负责展示和转发点击，没有直接发网络请求。
7. 新增 API、request、response、entity、dao、mapper 的目录符合总规范。
8. 新增完整接口、request/response/entity 模型时已有来自接口文档或业务说明的中文注释；局部小改沿用当前文件风格，没有编造注释。
9. 列表 item 的 UI 临时状态没有写进接口实体；DataBinding item XML 优先绑定 `ItemXxxVM`，自定义 Adapter/PagingDataAdapter 优先使用 `BindingViewHolder` 和 `binding.getVm(...)`。复杂列表、多选、批量操作、跨页面刷新或分页业务动作优先使用 `BaseItemEventsViewModel` 或同类事件 VM；简单选择、弹窗列表、静态配置项、局部 item 操作可沿用 item VM callback / `onItemClick` / 同类 `setOnItemClickListener`。
10. 长任务有重复启动保护，并在成功、失败、异常、取消后复位 UI 状态。
11. `CancellationException` 没有被当成普通异常吞掉。
12. 特殊渠道/审核模式/SDK 差异、登录/支付/授权/广告奖励、轮询/SSE/下载/生成状态映射、历史兼容和 UI 避让等非显而易见逻辑已有 1-2 行中文原因注释；没有为简单赋值、普通点击、显而易见代码堆模板注释。
13. 新增资源命名符合命名 skill，尤其是不透明纯色 `bg_颜色值_r/tr/br_radius值`、透明色 `bg_透明百分比_颜色值_r/tr/br_radius值`、描边和渐变命名。
14. 新增 drawable 前已查询本地是否有同色值、同圆角、同描边资源；纯色/圆角/描边背景已复用已有资源或按颜色值命名，没有使用 `bg_<页面>_<模块>.xml` 这类页面语义命名。
15. 新增文案已同步当前分支实际维护的 `values*/strings.xml`，没有照搬不存在或不需要的语言目录。
16. Figma/截图 UI 已优先映射项目 `style`、`theme`、`attr`、`color`、`dimen`、`drawable`、`selector`。
17. 新增或修改的 XML 中，可映射的 `4dp/8dp/12dp/16dp/24dp` 间距已使用 `@dimen/spacing_xs/s/m/l/xl`，可映射的 `4dp/8dp/12dp/16dp` 圆角已使用 `@dimen/radius_s/m/l/xl`。
18. 新增或修改的 XML 中，Figma `Text1` 到 `Text5`、主题色、背景色、填充色、功能色语义已使用 `?attr/textColor*`、`?attr/themeColor*`、`?attr/bgColor*`、`?attr/fillColor*` 或项目已有 `@color/@drawable`，没有无理由裸写 `#` 色值。
19. 新增或修改的 XML 中，已扫描裸 `dp`、裸 `#` 色值、`android:fontFamily`、`android:textStyle`、`android:textSize`、`android:letterSpacing`、`android:lineHeight`；能映射项目资源的已替换，非特殊需要的字距/行高已移除，`sans-serif-medium`、`sans-serif`、`PingFang SC` 等本地未定义字体兜底已移除，不能映射或必须保留的在输出中说明原因。
20. 文案控件未因写死宽高导致多语言截断风险。
21. 支付、登录、授权、广告奖励发放等没有只依赖客户端状态作为最终结果。
22. SDK 差异按当前分支实现隔离，没有把海外版/大陆版专属 SDK 调用散落到业务页面。
23. 未查看或修改 `OssManager.kt`，除非用户明确要求。

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
- 为普通分页默认新增 `RemoteMediator`
- Activity/Fragment 直接请求网络或访问数据库
- Adapter 直接请求网络
- 接口实体中新增 UI 临时状态字段
- 业务代码直接 import 第三方 SDK 类
- `fallbackToDestructiveMigration`
- 硬编码 token、secret、api key、私钥
- 线上日志输出敏感信息
- 固定使用另一个版本的项目名、包名、scheme、语言目录或 SDK

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

生成代码后按任务规模说明。快速局部修改不使用完整清单，只说明改动、验证和风险；以下条目只在实际触及时说明：

1. 新增/修改文件列表。
2. 复用了哪些项目已有类、方法、BindingAdapter、style、drawable。
3. 是否新增 API、request、response、entity、dao、mapper。
4. 是否新增 Repository，以及原因。
5. 是否新增 UiModel、UiState、UseCase，以及原因。
6. 是否涉及 Room version 或 Migration。
7. 是否涉及多语言 string。
8. 是否涉及长任务，以及在哪里取消。
9. 是否涉及 SDK 差异或版本差异处理。
10. 是否存在需要人工确认的点。

## 模式选择边界

生成代码时先按目标文件和同类实现判断，不从固定模板倒推文件：

- 简单页面不默认使用仓库型 ViewModel；只有页面确实请求 API、数据库或仓库能力时，才使用 `AppRepositoryViewModel`。
- XML 是否使用 `<layout>`、变量是否为 `layout` / `item`，以当前页面和同类实现为准；不要为了模板改写历史 ViewBinding 或无绑定 layout。
- 只有接口字段、请求体或响应模型无法复用时才新增 request / response / entity。
- 只有复用、组合请求、缓存、复杂错误策略或 SDK/数据层隔离需要时才新增 Repository。
- 只有 UI 展示结构明显独立、状态交叉复杂或多处共享时才新增 UiModel / UiState。
- 只有轮询、SSE、下载、生成任务等长任务才拆 Job / handler，并在 `onCleared()` 统一释放。
- UseCase 只在复用或业务规则复杂到需要独立测试时增加。
