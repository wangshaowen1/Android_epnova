# 项目列表索引

仅在完整列表 Skill 已命中且目标 feature 没有足够同类实现时读取。

| 要确认的能力 | 搜索入口 |
| --- | --- |
| 单类型 Adapter | `QuickAdapter<`、`submitList(` |
| ItemVM 绑定变量 | `AppItemViewModel`、`BR.item` |
| ViewHolder 复用 | `BindingViewHolder`、`binding.getVm` |
| 静态列表 | `RecyclerViewVM`、`setQuickAdapter` |
| 多类型能力 | `MultiTypeAdapter`、`ViewType<` |
| 点击转发 | `onItemClick.value`、`setOnItemClickListener` |

```powershell
rg -n "class .*Adapter|QuickAdapter<|BindingViewHolder|binding\.getVm" app/src/main/java
rg -n "class Item.*VM|AppItemViewModel|BR\.item|submitList\(" app/src/main/java
```

只读取目标列表和一个最近的同类 Adapter/ItemVM/item XML；不要维护业务页面快照。
