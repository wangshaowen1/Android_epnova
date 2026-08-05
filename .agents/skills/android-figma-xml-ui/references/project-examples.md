# 项目 UI 资源索引

仅在完整 Figma Skill 已命中且精确搜索仍无法确认资源入口时读取。

| 要确认的能力 | 搜索入口 |
| --- | --- |
| 文本 style | `styles.xml` 中 `Text.` / 当前语义 style |
| 主题颜色 | `attrs.xml`、`colors.xml`、theme 中对应 attr |
| 间距与圆角 | `dimens.xml` 中 `spacing_`、`radius_` |
| 图片 BindingAdapter | `loadUri`、`loadThumbnailUri`、`loadVideoUrl` |
| drawable 复用 | 目标颜色值、圆角值、stroke 值和直接引用 |

```powershell
rg -n "目标颜色值|目标间距值|目标圆角值|候选资源名" app/src/main/res
rg -n "load(Uri|ThumbnailUri|HQThumbnailUri|VideoUrl)" app/src/main/java
rg -n "@style/Text\.|\?attr/|@dimen/(spacing|radius)_" app/src/main/res/layout
```

读取目标 XML、一个最近同类 XML 和命中的资源定义即可；本索引不维护固定 token 值副本。
