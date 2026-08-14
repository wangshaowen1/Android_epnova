---
title: Matisse 媒体选择器接入契约
type: contract
status: active
last_verified: 2026-08-14
---

# Matisse 媒体选择器接入契约

## 目标与权威范围

本文约束 EpNova 对本地化 Matisse AAR 的依赖、调用入口、主题和文案配置，避免业务页面重复维护整套选择器 UI 参数。

公共 API 和默认资源以已发布 AAR 为事实源，版本坐标以 `modules.gradle` 为事实源，宿主配置以 `app/src/main/res/values/styles.xml` 和各语言 `strings.xml` 为事实源。仓库中的 `matisse/` 目录未被 `settings.gradle` 纳入构建，不作为当前应用行为依据。

## 范围与非目标

- 覆盖 Maven 依赖、系统 Photo Picker 与 Matisse 的路由边界、结果消费、主题优先级和本地化方式。
- 不约束业务上传、压缩或长期保存媒体文件的实现。
- 不保证 `content://` Uri 可以转换为真实文件路径；需要文件内容时通过 `ContentResolver` 读取。
- 不要求业务页面迁移为 Matisse 自带的系统选择器模式；EpNova 的系统选择流程由 `AppImagePicker` 统一路由。

## 依赖与入口

应用通过 `app/build.gradle` 引用 `modules.gradle` 中的 `com.kuaige.thirdparty:matisse:1.0.5`，不依赖仓库内的旧源码目录。

业务选图优先使用 `common/picker/AppImagePicker.kt`：

- Android 13 及以上使用 AndroidX Photo Picker，不申请媒体库读取权限。
- Android 12 及以下先走既有媒体权限流程，再打开 Matisse 自定义页面。
- 单选、多选都只回传本次结果；取消统一调用 `onCancel`。
- `selectedUris` 只用于旧系统上的 Matisse 预选，不与 Photo Picker 本次结果合并。

`common/ext/Context.kt` 中的选择方法属于旧调用兼容入口。新增业务不直接复制其中的 Matisse Builder 链，也不在页面内重新实现系统版本和权限分支。

## 主题与文案契约

### 主题选择

`AppTheme.Base` 必须通过 `matisseTheme` 指向 `Theme.Epnova.Matisse`，所有 AppTheme 派生主题继承该全局入口。`Theme.Epnova.Matisse` 必须继承 `Matisse.Base`，只覆盖 EpNova 的颜色、背景、按钮、语义图标和动画。

主题选择优先级为：单次 `.theme(...)` > Activity 或 Application Theme 中的 `matisseTheme` > `Matisse.Base`。只有某次调用需要整套不同视觉时才使用 `.theme(...)`。

返回、添加媒体、拍照图标以及进出场动画统一由 `Theme.Epnova.Matisse` 提供。按单个属性合并时保持以下优先级：

1. 当前请求在 `MatisseUiConfig` 中显式配置的值。
2. 当前项目主题 `Theme.Epnova.Matisse` 的属性。
3. `Matisse.Base` 的默认值。

Builder 未调用对应方法时才读取 Theme；显式传入 `0` 仍属于单次配置，不等同于“未配置”。

### 字符串资源

全局文案差异通过宿主覆盖 Matisse 的同名字符串资源，不在每个请求中重复设置。EpNova 当前维护以下资源及已有语言翻译：

- `button_cancel`、`button_open_settings`
- `matisse_partial_media_access_description`
- `matisse_camera_permission_description`、`matisse_media_permission_description`
- `matisse_add_media`、`matisse_take_photo`

默认 `values` 使用英文，简体中文、印度尼西亚语和泰语放在现有对应语言目录。图标必须是不含文字的 Drawable，不能把具体语言烘焙进图片资源。

## 单次配置边界

没有单次差异时不创建 `MatisseUiConfig`，由项目主题、宿主同名字符串和 `Matisse.Base` 补齐默认值。禁止恢复类似 `applyEpnovaMatisseUi()` 的全量配置扩展。

只有某次调用的文案、语义图标、部分授权横幅开关或转场确实不同，才创建 `MatisseUiConfig`，并只设置有差异的字段。单次整套视觉差异使用 `.theme(...)`，不要把可复用的按钮背景、颜色、文字外观或尺寸塞进请求配置。

启用 `.capture(true)` 时，调用方仍需提供与宿主 Manifest 和 FileProvider paths 一致的 `CaptureStrategy`；该 authority 依赖应用环境，Matisse 无法全局兜底。

## 版本与兼容

- `Matisse.Base`、`matisseTheme` 以及图标和动画主题属性从 `1.0.5` 起可用。
- `Matisse.Kuaige` 仅是兼容旧宿主的别名，EpNova 新主题不再继承该品牌主题。
- 默认选择器模式仍为自定义 Matisse 页面，升级不会自动改变旧系统上的权限和页面路由。
- `com.nhsd.mattise` 包名、`Matisse.obtainResult` 和旧请求码结果入口保持兼容。

## 联调与验证

- 静态检查 `modules.gradle` 只保留 `1.0.5` 坐标，所有 Matisse 调用不再注入全量 `MatisseUiConfig`。
- 检查 `AppTheme.Base` 的 `matisseTheme`、`Theme.Epnova.Matisse` 的父主题及图标/动画属性可解析。
- 分别在英文、简体中文、印度尼西亚语和泰语下检查取消、设置、权限说明、添加媒体与拍照文案。
- Android 13 及以上验证系统 Photo Picker 单选、多选和取消；Android 12 及以下验证权限、Matisse 预选、拍照及返回结果。
- 若新增单次 `MatisseUiConfig`，需验证未配置属性仍从项目主题继承，并验证与另一普通请求之间没有配置串扰。
