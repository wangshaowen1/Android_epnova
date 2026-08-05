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
2. 完整生成或复杂审查时，当前任务是否命中项目 skill：页面骨架、页面状态 DataBinding、API 请求、分页、非分页列表、Tab/ViewPager2、弹窗、导航事件、Figma UI、异步任务；局部小改只读目标代码和 quick check。
3. 当前分支的主业务工程、namespace、源码根包、语言目录和 SDK 依赖是否已经确认？
4. 新页面是否应该继承 `BaseViewModelActivity` / `BaseViewModelFragment`？
5. XML 是否需要 `<layout>`，变量名是否为 `layout` 或 `item`？
6. API 是否应加到 `ApiService.kt`，路径常量是否应加到 `ApiConstants.kt`，请求体是否放在 `request/`？
7. 是否真的需要新 Repository，还是 `AppRepository` / `CommonRepository` 已足够？
8. 是否真的需要 UiModel、UseCase、UiState？
9. 长任务所有者是否明确：ViewModel 页面任务保存 `Job`/handler 并在 `onCleared()` 取消；Service、系统下载或应用级 Manager 任务由页面解绑观察者而不是误取消？
10. 是否新增了字符串资源，并同步当前分支需要覆盖的 `values*/strings.xml`？
11. 是否涉及 Room version、Migration 或 AutoMigration？
12. 是否涉及支付、登录、授权、广告奖励、统计、归因、推送、设备标识等需要 SDK 隔离和安全校验的能力？
13. 是否误引入了 Hilt、AppContainer、ViewModelFactory、AppResult/AppError 等当前项目没有采用的结构？
14. 是否存在特殊渠道/审核模式/SDK 差异、登录/支付/授权/广告奖励、长任务状态映射、历史兼容或 UI 避让等需要保留业务原因的复杂分支，需要补必要中文注释？

## 生成后检查

完成代码后检查：

1. Activity/Fragment 只做 View 初始化、事件绑定、观察 ViewModel、跳转、Toast/Dialog/Loading 展示；Fragment 中手动注册回调的清理点按当前 baselibrary、基类 View 缓存策略和同类页面确定，没有跨版本套用固定生命周期方法。
2. ViewModel 不直接依赖 Activity、Fragment、View，不直接创建 `ApiService` 或 `AppDatabase`。
3. Repository 不随意吞异常，不把 Retrofit `Response` 暴露给 UI。
4. XML 只写简单 DataBinding 表达式，复杂判断已放到 ViewModel 或 BindingAdapter；稳定页面状态没有继续由多个 Observer/方法重复写 View，瞬时手势和动画状态也没有被机械搬进 XML。
5. Adapter 只负责展示和转发点击，没有直接发网络请求。
6. 新增 API、request、response、entity、dao、mapper 的目录符合总规范。
7. 新增完整接口、request/response/entity 模型时已有来自接口文档或业务说明的中文注释；局部小改沿用当前文件风格，没有编造注释。
8. 列表 item 的 UI 临时状态没有写进接口实体；DataBinding item XML 优先绑定 `ItemXxxVM`，自定义 Adapter/PagingDataAdapter 优先使用 `BindingViewHolder` 和带稳定业务 key 的 `binding.getVm(...)`，没有使用 position 或可变字段缓存 ItemVM。复杂列表、多选、批量操作、跨页面刷新或分页业务动作优先查当前 feature 是否已有事件 VM、callback 或 ViewModel 方法；简单选择、弹窗列表、静态配置项、局部 item 操作可沿用 item VM callback / `onItemClick` / 同类 `setOnItemClickListener`。多个 Tab 各自独立分页时，容器不持有各 Tab 列表数据，各子页持有独立分页链路，Fragment 列表不在 Tab 点击或刷新时重建。
9. 长任务有明确所有者和重复启动保护；页面级任务在成功、失败、异常、取消后复位 UI 状态，持久任务保留任务 id/真实状态供页面重新绑定。
10. `CancellationException` 没有被当成普通异常吞掉。
11. 特殊渠道/审核模式/SDK 差异、登录/支付/授权/广告奖励、轮询/SSE/下载/生成状态映射、历史兼容和 UI 避让等非显而易见逻辑已有 1-2 行中文原因注释；没有为简单赋值、普通点击、显而易见代码堆模板注释。
12. 新增 drawable 符合 XML 资源规则中的数值命名，尤其是不透明纯色 `bg_颜色值_r/tr/br_radius值`、透明色 `bg_透明百分比_颜色值_r/tr/br_radius值`、描边和渐变命名。
13. 新增 drawable 前已查询本地是否有同色值、同圆角、同描边资源；纯色/圆角/描边背景已复用已有资源或按颜色值命名，没有使用 `bg_<页面>_<模块>.xml` 这类页面语义命名。
14. 新增文案已同步当前分支实际维护的 `values*/strings.xml`，没有照搬不存在或不需要的语言目录。
15. Figma/截图 UI 已优先映射项目 `style`、`theme`、`attr`、`color`、`dimen`、`drawable`、`selector`。
16. 新增或修改的 XML 中，可映射的 `4dp/8dp/12dp/16dp/24dp` 间距已使用 `@dimen/spacing_xs/s/m/l/xl`，可映射的 `4dp/8dp/12dp/16dp` 圆角已使用 `@dimen/radius_s/m/l/xl`。
17. 新增或修改的 XML 中，Figma `Text1` 到 `Text5`、主题色、背景色、填充色、功能色语义已使用 `?attr/textColorPrimary/Title/Secondary/Hint/Disabled`、`?attr/colorBrand*`、`?attr/bgPrimary` 等背景语义、`?attr/fillPrimary` 等填充语义、`?attr/funcColor*` 或项目已有 `@color/@drawable`，没有无理由裸写 `#` 色值，也没有使用已废弃的 `themeColor1`、`textColor2`、`bgColor3` 等数字别名。
18. 新增或修改的 XML 中，已扫描裸 `dp`、裸 `#` 色值、`android:fontFamily`、`android:textStyle`、`android:textSize`、`android:letterSpacing`、`android:lineHeight`；能映射项目资源的已替换，非特殊需要的字距/行高已移除，`sans-serif-medium`、`sans-serif`、`PingFang SC` 等本地未定义字体兜底已移除，不能映射或必须保留的在输出中说明原因。
19. 文案控件未因写死宽高导致多语言截断风险。
20. 支付、登录、授权、广告奖励发放等没有只依赖客户端状态作为最终结果。
21. SDK 差异按当前分支实现隔离，没有把海外版/大陆版专属 SDK 调用散落到业务页面。
22. 未查看或修改 `OssManager.kt`，除非用户明确要求。

## 红线检查

逐项核对根目录 `AGENTS.md`《关键禁令》；本文件不复制红线内容，避免两处定义漂移。

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
