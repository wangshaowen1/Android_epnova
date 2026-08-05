# 项目页面索引

仅在页面骨架 Skill 已命中且同 feature 页面不足以确认真实基线时读取。

| 要确认的能力 | 搜索入口 |
| --- | --- |
| Activity/Fragment Base | `BaseViewModelActivity`、`BaseViewModelFragment` |
| ViewModel Base | `AppViewModel`、`AppRepositoryViewModel` |
| Toolbar | `ToolbarViewModel`、`view_model_toolbar` |
| 页面参数 | `fun start(`、`fun newInstance(`、`mBundle`、`Constants.Extra` |
| Fragment 生命周期 | `onPrepareView`、`onDestroyView`、`onDestroy` |
| 模块与包名 | `namespace`、`applicationId`、`AndroidManifest.xml` |

```powershell
rg -n "class .*Activity : BaseViewModelActivity|class .*Fragment : BaseViewModelFragment" app/src/main/java
rg -n "class .*ViewModel : (AppViewModel|AppRepositoryViewModel)|ToolbarViewModel" app/src/main/java
rg -n "fun (start|newInstance)\(|onPrepareView\(|repeatOnLifecycle" app/src/main/java
```

优先读取目标 feature 内一个最接近页面；只有 Base 行为不清楚时再读基类定义。
