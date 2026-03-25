package com.kuaigeglobal.epnova.widget

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import java.lang.Integer.min
import java.lang.ref.WeakReference

abstract class NeedUpdateFragmentStateAdapter<T> : FragmentStateAdapter {

    private var loop: Boolean = false

    // 使用 WeakReference 避免持有强引用导致内存泄漏
    private var dataList: ArrayList<T> = ArrayList()

    private var dataSet = false

    // Fragment 弱引用缓存
    private val fragmentCache: MutableMap<Long, WeakReference<Fragment>> = mutableMapOf()

    constructor(fragmentActivity: FragmentActivity) : super(fragmentActivity)
    constructor(fragment: Fragment) : super(fragment)
    constructor(fragmentManager: FragmentManager, lifecycle: Lifecycle) : super(fragmentManager, lifecycle)

    constructor(fragmentActivity: FragmentActivity, loop: Boolean = false) : super(fragmentActivity) {
        this.loop = loop
    }

    constructor(fragment: Fragment, loop: Boolean = false) : super(fragment) {
        this.loop = loop
    }

    constructor(fragmentManager: FragmentManager, lifecycle: Lifecycle, loop: Boolean = false) : super(fragmentManager, lifecycle) {
        this.loop = loop
    }

    // 提交新数据
    fun submitData(data: List<T>) {
        if (!dataSet) {
            initData(data)
        } else {
            updateData(data)
        }
    }

    /**
     * 初始化数据
     */
    private fun initData(dataList: List<T>) {
        dataSet = true
        this.dataList.clear()
        this.dataList.addAll(dataList)
        notifyDataSetChanged()
    }

    abstract fun isDiff(old: T, new: T): Boolean

    abstract fun createFragment(position: Int, item: T): Fragment

    // 完全按照新列表顺序来显示
    private fun updateData(newDataList: List<T>) {
        val minSize = min(dataList.size, newDataList.size)
        val deltaSize = kotlin.math.abs(dataList.size - newDataList.size)

        // 更新前面的 Fragment
        for (i in 0 until minSize) {
            val newData = newDataList[i]
            if (isDiff(dataList[i], newData)) {
                // 更新已经存在的 Fragment
                val fragment = fragmentCache[getItemId(i)]?.get()
                fragment?.let {
                    (it as? DataRefreshable<T>)?.onRefresh(newData)
                }
            }
            dataList[i] = newData // 无论如何都要更新新数据
        }

        // 处理后面的数据
        if (newDataList.size < dataList.size) {
            // 删除多余的 Fragment
            repeat(deltaSize) { dataList.removeLastOrNull() }
            notifyItemRangeRemoved(minSize, deltaSize)
        } else if (newDataList.size > dataList.size) {
            // 插入新的 Fragment
            repeat(deltaSize) { dataList.add(newDataList[minSize + it]) }
            notifyItemRangeInserted(minSize, deltaSize)
        }
    }

    // 创建 Fragment 并缓存其弱引用
    override fun createFragment(position: Int): Fragment {
        val realPosition = realPosition(position)
        val data = dataList[realPosition]
        val fragment = createFragment(realPosition, data)

        // 缓存 Fragment 的弱引用
        fragmentCache[getItemId(realPosition)] = WeakReference(fragment)

        return fragment
    }

    // 清理无效的 Fragment 缓存
    private fun cleanUpFragmentCache() {
        fragmentCache.entries.removeIf { it.value.get() == null }
    }

    override fun getItemCount(): Int {
        return if (loop && dataList.size > 1) Integer.MAX_VALUE else dataList.size
    }

    override fun getItemId(position: Int): Long {
        // 根据实际位置返回唯一标识
        return realPosition(position).toLong()
    }

    fun realPosition(position: Int): Int {
        return if (loop) {
            if (dataList.isEmpty()) 0 else position % dataList.size
        } else {
            position
        }
    }

    fun onPageSelected(position: Int) {
        cleanUpFragmentCache()
        if (dataList.isNotEmpty()) {
            val realPos = realPosition(position)
            fragmentCache.forEach { (key, fragmentRef) ->
                fragmentRef.get()?.userVisibleHint=(key == getItemId(realPos))
            }
        }
    }

    // 清理资源以避免内存泄漏
    fun clear() {
        dataList.clear()
        fragmentCache.clear()
        dataSet = false
    }

    // 在 Fragment 销毁时调用，以释放资源
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        clear() // 清理所有缓存和数据
    }
    interface DataRefreshable<T> {

        fun onRefresh(data:T)
    }
}
