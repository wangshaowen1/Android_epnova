---
name: android-databinding-list
description: "Android 非分页 DataBinding 列表实现规范。仅在本次实际新增、重构或审查非分页 RecyclerView/DataBinding 列表实现，包括 Adapter、ItemVM、item layout、列表数据更新和点击转发时使用。只排查列表 bug、只改 item 文案/样式或只改一小段刷新逻辑时，优先读目标代码和 android-check-quick.md，不触发完整列表 skill。"
---

# Android 非分页 DataBinding 列表

## 配套读取

- 涉及 `PagingDataAdapter`、分页刷新或上拉加载时，改读 `android-paging-state-ui`。
- 同时新增页面骨架或页面级稳定状态时，才追加对应 skill。
- 只有附近实现不足、需要确认 Adapter/ItemVM 绑定方式或混排能力时，才读取 [项目列表索引](references/project-examples.md)。

## 默认组合

- 单类型列表优先 `QuickAdapter<ItemXxxVM>(R.layout.item_xxx, emptyList())`。
- ItemVM 继承 `AppItemViewModel<T>`，只承载实体、本地 UI 状态和回调。
- 使用 `AppItemViewModel` 或项目 Base Adapter 时，item XML 使用 `<layout>` 且变量名必须为 `item`，因为 `AppItemViewModel.id()` 返回 `BR.item`；已有自定义绑定体系则跟随其真实 binding id。
- XML 能直接读取 `item.data.xxx` 的字段、空值兜底、颜色或显隐时，不在 ItemVM 再生成同义 getter、`displayXxx` 或 `isXxxVisible`。
- 列表数据优先映射成新 ItemVM 列表后 `submitList(...)`；不要反复修改并提交同一个可变 List。

## ViewHolder 与复用

- 自定义 Adapter 或 PagingDataAdapter 的 ViewHolder 优先继承 `BindingViewHolder<VB>`。
- 仅在模型确有非空、稳定、唯一的业务 key 时使用 `binding.getVm(key, ...)`；不能传 `null`、adapter position 或会变化的展示字段。
- `AppItemViewModel.id()` 是 DataBinding 变量 ID，不是业务唯一键。
- position 重载已废弃，新代码不再调用；没有可靠 key 时先确认当前 Adapter 的创建/回收策略，不伪造 key。

## 事件与更新

- Adapter 只渲染和转发事件，不请求网络、不访问数据库、不直接承担导航业务。
- 简单选择、弹窗列表、静态配置和局部 item 操作可用 ItemVM callback、`onItemClick.value`、ViewModel 方法或附近同类监听。
- 多选、批量操作、跨页面刷新和复杂 item 动作，先复用当前 feature 已有事件 VM 或 callback，由页面/ViewModel 收敛。
- 本地编辑型列表可以按现有 BaseQuickAdapter 能力增删；服务端刷新仍优先提交新列表。
- 只有强制全量重绘且当前实现确有需要时才使用 `refresh()` / `notifyDataSetChanged()`。

## 分支

- 多类型列表只在真实混排且仓库存在可验证模式时使用 `MultiTypeAdapter` 或附近 Adapter，不为潜在扩展提前引入。
- XML 已绑定 `RecyclerViewVM` 的静态/配置型列表可沿用其 Builder；普通页面不强行切换。
- 出现 `PagedList`、page/pageSize、LoadState、分页 footer 或 `PagingSource` 时转到分页 skill。

## 检查

- `item` 变量与当前 Base Adapter 的 binding id 一致。
- `binding.getVm` 使用真实稳定业务 key，没有 position/null/可变字段。
- ItemVM 没有重复实体展示字段，Adapter 没有业务请求。
- 更新使用新列表和 `submitList`，事件复杂度与转发方式匹配。
- 普通列表没有误引入分页、RemoteMediator、新 BaseAdapter 或无需求的混排结构。
