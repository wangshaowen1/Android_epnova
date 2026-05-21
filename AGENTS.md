# Epnova Codex Project Memory

## Response And Collaboration

- Always answer in Chinese.
- Prefer concise, direct engineering communication.
- Project owner is an Android developer working on the Epnova AI short-drama creation app.
- Main language is Kotlin, with some legacy Java.
- Package name: `com.kuaigeglobal.epnova`.
- The project shares the same `nhsd.basic` base framework style as FastFilm.

## Project Architecture

- Architecture: MVVM + Repository.
- Base library: `nhsd.basic`.
- View layer: `Activity` / `Fragment` handles UI, event listeners, and observing `LiveData` / `Flow`.
- ViewModel layer: business logic, UI state, calls Repository.
- Repository layer: wraps network, database, and cache sources.
- Model layer: pure data classes under `entity`, `request`, or `response`.

Common package layout:

- `base/`: project base classes wrapping `nhsd.basic`.
- `common/`: shared extensions, adapters, utilities, constants.
- `core/network/apis/`: Retrofit APIs, request bodies, response models.
- `core/data/CommonRepository.kt`: common repository.
- `core/manager/`: managers and third-party SDK isolation.
- `feature/`: feature modules by page or business domain.
- `router/`, `widget/`, `kit/`, `service/`: routing, widgets, media/toolkit, background services.

## Base Class Rules

- Activity directly extends `nhsd.basic.BaseViewModelActivity<VB, VM>`.
- Fragment directly extends `nhsd.basic.BaseViewModelFragment<VB, VM>`.
- Repository ViewModel extends `AppRepositoryViewModel<T>`.
- Non-repository ViewModel extends `AppViewModel`.
- `AppViewModel.id()` and `AppRepositoryViewModel.id()` return `BR.layout`.
- `binding`, `mRealVM`, `mRepo`, and `mBundle` are injected by base classes.
- Fragment initialization should override `onPrepareView()`, then call `initView()` and `initObservers()`.
- ViewModel initialization should override `onCreate()`.
- Clean `Job`, SSE handlers, and other resources in `onCleared()`.

## Kotlin And Naming

- Indentation: 4 spaces.
- Prefer safe calls and Elvis operators over `!!`.
- Keep line length around 120 characters.
- Class names:
  - `XxxActivity`
  - `XxxFragment`
  - `XxxViewModel`
  - `XxxRepository`
  - `XxxDialog` / `XxxBottomSheet`
  - list item ViewModel: `ItemXxxVM`
- Private members use camelCase.
- Constants use upper snake case in `companion object`.
- Boolean names start with `is`, `has`, `can`, or `should`.
- `LiveData` names use business meaning directly; do not add a `LiveData` suffix.
- Function names start with verbs. Event callbacks use `onXxx`.
- Hard-coded keys for MMKV, SharedPreferences, Intent extras, Bundle keys, and repeated string keys must be extracted to named constants.

## Comments

- Use Chinese comments when comments are needed.
- Class comments may use the project style:

```kotlin
/**
 * @Introduce: XXX界面
 */
```

- Comments should explain why, not restate obvious code.
- TODO format: `//TODO xxx`.

## DataBinding And UI

- DataBinding layout variable for page ViewModels must be named `layout`.
- Item layout variable must be named `item`.
- Click events should use `setOnSingleClickListener`, not raw `setOnClickListener`.
- Fragment-level click routing can override `onClick(view, data)` when the action belongs to navigation or View-layer behavior.
- Keep pure business logic in the ViewModel.

## RecyclerView

- Lists should use `QuickAdapter<ItemXxxVM>` plus `AppItemViewModel<T>` with DataBinding item layouts.
- Declare adapters in ViewModel when following existing project patterns.
- Update list data with `submitList(newItems)`.
- Use `refresh()` only when a forced full refresh is really needed.

## Network

- API definitions live in `core/network/apis/ApiService.kt`.
- Request body data classes live in `core/network/apis/request/`.
- Kotlin response wrappers live in `core/network/apis/response/`.
- Do not modify `ApiResponse.java`.
- Repository methods should directly call API methods and leave result handling to ViewModel.
- Use `request(...)` for silent/background loading.
- Use `requestWithLoading(...)` for user-triggered operations that need a loading dialog or automatic error toast.
- `showToastError = true` should be used when API `msg` should be shown automatically.
- Polling jobs must be cancelled before restart and in `onCleared()`.
- SSE handlers must be cancelled before restart and in `onCleared()`.

## Dialogs

- Center dialog with ViewModel: `BaseVMDialogFragment<VB, VM>`.
- Bottom dialog with ViewModel: `BaseBottomPushVMDialogFragment<VB, VM>`.
- Simple center dialog without ViewModel: `BaseDialogFragment<VB>`.
- Very small ViewBinding-only dialog may use `Dialog(context, style)`.
- Dialog arguments should be passed through `arguments` and read from `mBundle` in ViewModel where applicable.

## String Resources

When adding string resources, use only these files and check duplicated keys once:

- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-zh/strings.xml`
- `app/src/main/res/values-th/strings.xml`
- `app/src/main/res/values-in/strings.xml`

Insert new strings near related keys and keep semantic grouping.

## Third-Party SDK Isolation

Features that may swap third-party platforms, such as ads, payment, push, analytics, and statistics, must be isolated behind project interfaces.

- Business code should not directly import third-party SDK classes.
- Put abstractions and platform implementations under `core/manager/<feature>/`.
- Manager classes should depend on interfaces, not concrete SDK types.

## Figma And Design Access

- Access Figma designs through available MCP tools or curl with the Figma REST API.
- Do not use WebFetch for Figma URLs.

## Ignore Rules

- Claude ignore equivalent: `**/OssManager.kt`.
- Do not inspect or edit `OssManager.kt` unless explicitly requested.

## 六、Figma UI 精确实现规范

> 涉及 Figma 设计稿、截图还原、视觉走查、UI 页面/组件实现时，默认启用“精确模式”。

### 触发条件

以下任一情况必须按精确模式执行：

- 用户提供 Figma 链接、节点、截图、标注图或设计稿导出资源
- 用户要求“还原设计稿”“按 Figma 实现”“像素级还原”“精确实现 UI”
- 任务目标是生成页面、组件、弹窗、表单、卡片、列表、仪表盘等可见 UI

### 执行要求

1. **优先读取设计源**：有 Figma 插件/连接器时，优先读取 Figma 节点数据、设计 Token、布局属性和资源；没有可用连接器时，明确说明缺少设计源，并基于用户提供的截图/标注谨慎实现。
2. **精确还原视觉属性**：尺寸、间距、颜色、字体、字号、字重、行高、圆角、边框、阴影、透明度、层级、对齐方式必须按设计稿实现，不用“差不多”的估算值替代。
3. **精确还原布局关系**：保持设计稿中的栅格、约束、Auto Layout/Flex 方向、gap、padding、宽高、最小/最大尺寸和响应式行为。
4. **优先使用项目 Token**：如果项目存在设计 Token、CSS 变量、SCSS 变量或组件库主题，先映射到项目 Token；仅当设计稿值没有对应 Token 时，才使用局部精确值，并保留必要说明。
5. **资产真实落地**：图标、图片、插画、背景、头像等视觉资产应使用设计稿导出资源或项目已有资产；不要用随意占位图替代明确资产。
6. **交互状态完整**：按钮、输入框、选择器、Tab、弹窗等组件需要覆盖默认、hover、active、focus、disabled、loading、error、empty 等设计中出现或业务必需的状态。
7. **禁止自行改设计**：不得擅自调整品牌色、圆角风格、阴影层级、字体层级、布局密度或组件形态；如设计稿存在明显缺失或矛盾，先说明假设再实现。
8. **实现后自检**：完成后检查实际页面与设计稿在首屏结构、关键间距、颜色、字体、对齐和响应式表现上的差异；能运行项目时，优先用浏览器截图或构建结果验证。

### 输出标准

- 说明已按 Figma 精确模式处理的范围。
- 列出无法精确还原的内容及原因，例如缺少字体文件、缺少图片资产、设计稿未提供移动端状态。
- 不把设计说明文案写进页面 UI，除非设计稿本身包含这些文案。

---
