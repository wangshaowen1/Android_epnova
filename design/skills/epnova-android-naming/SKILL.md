---
name: epnova-android-naming
description: Epnova Android Kotlin naming rules for package, class, ViewModel, Repository, API model, resource, View id, state, event, constant, function, layout, drawable, string, dimen, and attr names. Use when the user mentions naming, 命名, 命名规则, resource naming, 资源命名, drawable naming, id naming, string key, class naming, package naming, or when generating/reviewing Epnova Android code, XML layouts, drawable resources, string resources, Retrofit APIs, request bodies, response/entity models, adapters, ViewModels, Repositories, events, states, constants, and keys.
---

# Epnova Android Naming

## Trigger Conditions

Use this skill when the task includes any naming decision or naming review in the Epnova Android project:

- The user mentions naming, 命名, 命名规则, 资源命名, drawable 命名, id 命名, string key, 包名, 类名, 方法名, 字段名, or 常量名.
- Generating or reviewing package, class, function, property, ViewModel, Repository, Adapter, item VM, UiState, event, effect, constant, Intent extra, Bundle key, MMKV key, or SharedPreferences key names.
- Adding or reviewing Android resource names: layout, drawable, selector, gradient, string, dimen, attr, style, color, View id, and BindingAdapter attribute names.
- Adding or reviewing Retrofit API names, request body names, response model names, entity names, Room entity names, DAO names, mapper names, or UiModel names.
- Implementing UI from Figma or XML where new resources or View ids need project-consistent names.

## Core Rule

Follow the current Epnova project style first. Do not introduce generic Android template names when an existing local pattern is available.

Use Kotlin by default. Keep legacy Java names only when editing existing Java files.

## Packages

- Use lowercase package names.
- Place new business code under the closest existing `feature/<domain>` package.
- Use `data`, `request`, `response`, `entity`, and `mapper` only when the subpackage has a clear role.
- Avoid creating deep one-page package trees.
- Avoid copying historical misspellings such as `manger` into new packages unless compatibility requires it.

## Classes

- Activity: `XxxActivity`
- Fragment: `XxxFragment`
- ViewModel: `XxxViewModel`
- Repository: `XxxRepository`
- Dialog: `XxxDialog`
- Adapter: `XxxAdapter`
- Item ViewModel: `ItemXxxVM`
- Ui state: `XxxUiState`
- Event: `XxxEvent`
- Effect: `XxxEffect`, only when explicitly needed
- Enum: `XxxEnum` or an established business name such as `MainBulletFrameEnum`

Prefer concrete business names:

- Good: `SeriesViewModel`, `AssetManagementRepository`, `ItemRechargeCreditsVM`
- Avoid: `CommonViewModel2`, `DataManager`, `ListItem`, `MyAdapter`

Use `Manager`, `Controller`, or `Helper` only for coordination, platform isolation, or utility responsibilities.

## Properties

- Use lowerCamelCase for variables, properties, and functions.
- Do not add a private `m` prefix in new code. Keep framework fields like `mRealVM`, `mRepo`, and `mBundle` unchanged.
- Name `LiveData` by business meaning, not by type: `workInfo`, `loginTaskData`, `episodeInfo`.
- Name flows by business meaning; add `Flow` only when helpful: `feedFlow`, `currentWaterfallId`.
- Adapter variables: `xxxAdapter`.
- Job variables: `xxxJob`.
- Boolean names start with `is`, `has`, `can`, or `should`.
- Collection names are plural or use `xxxList`: `episodes`, `selectedIds`, `roleImageList`.

## Functions

- Use verb-led names: `loadData()`, `fetchUserInfo()`, `submitFeedback()`, `updateLoadState()`.
- Use `onXxx` for callbacks: `onPaySuccess()`, `onItemSelected()`.
- Use standard init names: `initView()`, `initListener()`, `initObservers()`, `initData()`.
- Use `fetchXxx()` for active remote refresh.
- Use `loadXxx()` for page data loading, possibly with cache.
- Use `getXxx()` for lightweight reads or existing API naming.
- Use `queryXxx()` for query-style APIs.
- Use `submitXxx()`, `saveXxx()`, `deleteXxx()` for user-triggered mutations.

Avoid vague names such as `doSomething()`, `handleData()`, and `process()` unless the local scope is tiny and obvious.

## Constants And Keys

- Put constants in `companion object` unless a shared `Constants` location already exists.
- Use upper snake case.
- Intent/Bundle keys: `KEY_XXX` or `Constants.Extra.XXX`.
- Request codes: `REQUEST_CODE_XXX`.
- Defaults: `DEFAULT_XXX`.
- Page sizes: `PAGE_SIZE`, `INITIAL_LOAD_SIZE`.
- Time intervals: `CLICK_INTERVAL_TIME`, `POLLING_INTERVAL_MS`.
- Extract MMKV, SharedPreferences, cache keys, protocol fields, event types, and interface enum values to named constants.

## API And Models

- Retrofit method names use lowerCamelCase and match business action: `getUserHomeInfo()`, `seriesFollow()`, `createOrder()`.
- API constants in `ApiConstants.kt` use upper snake case.
- Request body: `XxxBody`, for example `WorkIdBody`.
- Paged request body: `XxxPagedBody`.
- Aggregated API response model: `XxxData`, for example `HomeBookShelfData`.
- API entity: `XxxInfo`, for example `WorkInfo`, `EpisodeInfo`, `PaymentInfo`.
- Room entity: `XxxEntity`.
- Mapper functions: `toXxx()`, for example `toDomain()`, `toEntity()`, `toUiModel()`.
- Add `XxxUiModel` only when UI needs a separate display model.

## Resources

Layout files:

- Activity: `activity_xxx.xml`
- Fragment: `fragment_xxx.xml`
- Dialog: `dialog_xxx.xml`
- RecyclerView item: `item_xxx.xml`
- Include fragment: `layout_xxx.xml` or `include_xxx.xml`

Drawable files:

- Icon: `icon_xxx.xml` or `ic_xxx.xml`
- Image: `img_xxx.webp` or `img_xxx.png`
- Solid background: `bg_<alpha>_<color>_<corner><radius>.xml`
- Selector: `selector_xxx.xml` or `cb_xxx.xml`
- Gradient: `gradient_<alpha1>_<color1>_<alpha2>_<color2>_<direction>_<corner><radius>.xml`
- Oval/shape: `oval_xxx.xml` or `shape_xxx.xml`
- Progress: `progress_xxx.xml`

Drawable token rules:

- `<alpha>` uses two lowercase hex digits without `#`: `00`, `10`, `20`, `50`, `80`, `ff`.
- `<color>` uses six lowercase hex digits without `#`: `ffffff`, `000000`, `121212`.
- `<radius>` uses the dp value only, without `dp`: `r8`, `r12`, `tr16`.
- Capsule backgrounds always use `r50`, for example `bg_ff_ffffff_r50.xml`.
- Use `r<radius>` for all corners.
- Use `tr<radius>` for top corners only.
- Use `br<radius>` for bottom corners only.
- For solid backgrounds, prefer exact value names over semantic names when the resource is generic:
  - `bg_20_ffffff_r8.xml`
  - `bg_80_121212_r16.xml`
  - `bg_ff_f5f5f5_tr24.xml`
- For new gradient resources, include both color stops first, then direction and corner:
  - `gradient_ff_fff1ea_ff_ffffff_top_r16.xml`
  - `gradient_ff_ff6159_ff_d91f1f_left_r16.xml`
  - `gradient_80_121212_80_1c1c2d_top_tr16.xml`
- Supported direction tokens: `top`, `bottom`, `left`, `right`, `ltop`, `rtop`, `lbottom`, `rbottom`. Keep legacy direction-first names when editing existing files, but use the color-first order for new files.
- If a drawable is highly screen-specific or function-specific, use `shape_bg_<feature>_<state_or_role>.xml` instead of encoding every value, for example `shape_bg_publish_task.xml`, `shape_bg_story_upload.xml`, `shape_bg_work_popularity.xml`.
- Do not use vague names such as `bg_gray.xml`, `shape_1.xml`, or `gradient_button.xml` when values or business role can be named.

Other resources:

- Reuse common dimens such as `spacing_xs`, `spacing_s`, `spacing_m`, `spacing_l`, `radius_s`, `radius_m`, and `radius_l`.
- Business-specific dimens use `business_property`, for example `main_bottom_nav_height`.
- String keys use semantic names, for example `feedback_submit_success`.
- Avoid positional names such as `text_button_1`.

## View IDs

Use lowerCamelCase View ids, matching existing XML style.

Common prefixes:

- `tv`: TextView
- `iv`: ImageView
- `btn`: button or clickable entrance
- `rv` or `recyclerView`: RecyclerView
- `srl`: SmartRefreshLayout
- `lsv`: LoadStateViewHolder
- `et`: EditText
- `ll`, `cl`, `fl`: LinearLayout, ConstraintLayout, FrameLayout
- `vp`: ViewPager or ViewPager2
- `tab`: custom tab click layer
- `rb`: RadioButton

Prefer business meaning: `btnSearch`, `ivCover`, `tvTitle`.

Avoid `view1`, `text2`, `button3`, and other positional names.

## Review Checklist

Before finishing generated code, check:

- Names match an existing nearby feature pattern.
- No new Hilt, `AppContainer`, `ViewModelFactory`, `AppResult`, or `AppError` names were introduced for naming-only consistency.
- API, request body, response, entity, and mapper names live in the expected package.
- XML layout variables remain `layout` for pages and `item` for list items.
- New string, drawable, layout, and id names are semantic and searchable.
