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

