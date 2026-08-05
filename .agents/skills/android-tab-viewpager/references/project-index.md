# Epnova 项目索引

只在需要确认复用点或生命周期边界时读取。路径和符号以当前分支为准。

## Fragment 容器 Adapter

- 搜索：`class FragmentStateCacheAdapter`
- 当前职责：保存预创建的 Fragment 列表，提供 `getFragment(position)`。
- 当前限制：`setFragments()` 不调用通知；itemId 来自 Fragment 实例 `hashCode()`。安全默认是初始化时设置一次稳定列表，不支持未经设计的动态增删 Tab。
- 项目还存在未发现业务调用的 `NeedUpdateFragmentStateAdapter`。它以位置作为 itemId，只处理同位置刷新和尾部区间增删，不是任意重排/动态 Tab 的现成答案；不要只因类名看起来匹配就替换默认 Adapter。

## Tab / 指示器接入点

- `magicindicator/.../ViewPagerHelper.java`：已有 ViewPager2 绑定入口，但内部注册匿名 callback，没有 unbind 入口。
- 搜索 `getCommonNavigatorVp2`：已有自定义标题点击后设置 `currentItem` 的封装。
- 搜索 `attachToViewPager2`：`PillIndicatorView` 同样注册匿名 callback，未暴露 detach。上述快捷绑定只适合生命周期与 ViewPager2 一致且不会重复绑定的容器；可复用 Fragment View 或需重复 attach 时，不要直接套用。

## 当前业务示例

- `feature/works/MyListFragment.kt`：MagicIndicator + 三个稳定 Fragment；额外 page callback 保存实例并在 `onDestroy()` 注销。
- `feature/creation/CreationFragment.kt`：两个稳定 Fragment + 自定义 Tab 状态；callback 同样成对注册/注销。
- `feature/details/ComputingPowerDetailsActivity.kt`：Activity 内两页容器；包含匿名 callback，审查或重构时需注意它没有显式注销句柄。

新实现优先参考与目标生命周期和交互最接近的页面，不把 Banner 示例当作 Fragment 容器示例。

## 定位命令

```bash
rg -n "FragmentStateCacheAdapter|NeedUpdateFragmentStateAdapter|ViewPager2|FragmentStateAdapter|TabLayoutMediator|registerOnPageChangeCallback|getCommonNavigatorVp2|attachToViewPager2" app magicindicator
```
