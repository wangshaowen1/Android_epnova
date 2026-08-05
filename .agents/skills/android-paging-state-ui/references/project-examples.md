# 项目分页索引

仅在分页 Skill 已命中且目标 feature 没有足够同类实现时读取。

| 要确认的能力 | 搜索入口 |
| --- | --- |
| Pager/PagingSource | `Pager(`、`PagingSource<`、`cachedIn` |
| 页面状态绑定 | `startRefreshWithAdapter`、`LoadStateViewHolder` |
| Footer/网格 | `MyMultiStatePagingFooterAdapter`、`applyGridSpanSizeLookup` |
| ItemVM 复用 | `BindingViewHolder`、`binding.getVm` |
| 旧容器兼容 | `CoolRefreshView` 及当前 feature 扩展 |
| Room 缓存 | `RemoteMediator`、`withTransaction`、remote key |

```powershell
rg -n "Pager\(|PagingSource<|PagingDataAdapter|cachedIn" app/src/main/java
rg -n "startRefreshWithAdapter|LoadStateViewHolder|MyMultiStatePagingFooterAdapter|applyGridSpanSizeLookup" app/src/main/java
rg -n "RemoteMediator|withTransaction|CoolRefreshView|ConcatAdapter" app/src/main/java
```

只读取一个同容器、同数据源的最近分页链路；多 Tab 容器能力改查 `android-tab-viewpager` 的索引。
