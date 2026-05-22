# 项目资源索引

本文件记录 Figma/截图到 Android XML skill 的项目依据。还原 UI 前先查这些文件，再决定是否新增资源。

## 主题与样式

- `app/src/main/res/values/attrs.xml`
  - 主题色：`themeColor1`、`themeColor2`、`themeColor3`。
  - 功能色：`funcColorSuccess`、`funcColorWarning`、`funcColorError`、`funcColorLink`。
  - 填充色：`fillColor1`、`fillColor2`、`fillColor3`、`fillColor4`。
  - 文本色：`textColor1`、`textColor2`、`textColor3`、`textColor4`、`textColor5`。
  - 背景色：`bgColor1`、`bgColor2`、`bgColor3`、`bgColor4`、`bgColor5`、`bgColor6`。
- `app/src/main/res/values/styles.xml`
  - `AppTheme.Base` 中绑定了 `themeColor*`、`textColor*`、`bgColor*`、`fillColor*`。
  - 字重 style：`Text.Bold`、`Text.SemiBold`、`Text.Medium`、`Text.Regular`。
  - 字号 style：`Text28`、`Text20`、`Text18`、`Text16`、`Text14`、`Text13`、`Text12`、`Text11`、`Text10`。
  - 常见按钮 style：`Button.Medium`、`Button.Bold`、`Button.Bold.Gradient`。
- `app/src/main/res/values/dimens.xml`
  - 通用间距：`spacing_xs`、`spacing_s`、`spacing_m`、`spacing_l`、`spacing_xl`。
  - 通用圆角：`radius_s`、`radius_m`、`radius_l`、`radius_xl`。
  - 主导航高度：`main_bottom_nav_height`。

## XML 示例

- `app/src/main/res/layout/view_model_toolbar.xml`
  - Toolbar 标题和按钮文本使用 `@style/Text16`、`@style/Text.Medium`、`?attr/textColor1`。
- `app/src/main/res/layout/activity_series.xml`
  - 页面级卡片、标题、封面、列表区域混合使用 `textAppearance="@style/Text18/Text14/Text13"` 和 `?attr/textColor1`。
  - 图片使用 `loadThumbnailUri="@{layout.workInfo.workCoverUrl}"`。
- `app/src/main/res/layout/item_video_feed.xml`
  - feed item 复用 `?attr/bgColor1`、`@style/Text18`、`@style/Text12`、`?attr/textColor1`。
- `app/src/main/res/layout/include_episode_unlock.xml`
  - 示例中大量使用 `@style/Text.Medium`、`@style/Text.Regular`、`?attr/textColor1`。
- `app/src/main/res/layout/multi_state_adapter_view.xml`
  - 状态 UI 文案使用项目 Text style。

## BindingAdapter

- `app/src/main/java/com/kuaigeglobal/epnova/core/databinding/BindingAdapters.kt`
  - 图片：`loadUri`、`loadThumbnailUri`、`loadHQThumbnailUri`、`loadVideoUrl`。
  - 尺寸/边距：`setLayoutWidth`、`setLayoutHeightDp`、`layoutHeight`、`layoutWidth`、`marginTopDp`、`marginBottomDp`。
  - 点击：`singleClick`、`navigationClick`。
  - 状态/可见性：`setVisible`、`animateVisibility`、`setLoadState`。

## Drawable 命名示例

优先复用已有通用资源：

- 胶囊：`bg_05_ffffff_r50.xml`、`bg_10_ff8902_r50.xml`、`bg_20_000000_r50.xml`。
- 圆角背景：`bg_10_ffffff_r12.xml`、`bg_30_000000_r16.xml`、`bg_80_121212_r12.xml`。
- 顶部/底部圆角：`bg_121212_tr16.xml`、`bg_ffffff_br16.xml`。
- 渐变：`gradient_top_ff6159_bottom_d91f1f_r16.xml`、`gradient_top_80_121212_bottom_80_1c1c2d_tr16.xml`、`gradient_left_ff4332_right_ffa029_r50.xml`。
- 功能语义背景：`shape_bg_publish_task.xml`、`shape_bg_story_upload.xml`、`shape_bg_ai_message_in.xml`。
- 主题色背景：`bg_theme_color1_r50.xml`、`btn_theme1_r50.xml`。

新增 drawable 时，如果是通用资源，按值命名；如果只服务某个界面且语义强，按 `shape_bg_<功能>_<状态或角色>.xml`。

## 搜索命令

```powershell
rg -n "textColor1|bgColor1|themeColor1|fillColor1" app/src/main/res/values
rg -n "Text\\.Bold|Text\\.SemiBold|Text\\.Medium|Text\\.Regular|Text16|Text14" app/src/main/res/values/styles.xml
rg --files app/src/main/res/drawable -g "bg_*.xml" -g "gradient_*.xml" -g "shape_bg_*.xml" -g "selector_*.xml"
rg -n "loadUri|loadThumbnailUri|loadHQThumbnailUri|loadVideoUrl|singleClick" app/src/main/java/com/kuaigeglobal/epnova/core/databinding/BindingAdapters.kt app/src/main/res/layout
```
