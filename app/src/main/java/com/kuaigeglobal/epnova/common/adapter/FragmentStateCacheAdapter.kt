package com.kuaigeglobal.epnova.common.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * *****************************************
 * 项目名称: KuaigeAi
 * @Author: SZYMN
 * @Introduce:通用FragmentStateAdapter适配器
 * @Data: 2025/9/5/周五  15:25
 * @Version: 1.0
 * *****************************************
 */
class FragmentStateCacheAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager,lifecycle) {
    private val fragments = mutableListOf<Fragment>()

    fun setFragments(newFragments: List<Fragment>) {
        fragments.clear()
        fragments.addAll(newFragments)
    }

    fun getFragment(position: Int):Fragment?{
        if (position >= fragments.size||position<0)return null
        return fragments[position]
    }

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }

    override fun getItemId(position: Int): Long {
        // 返回 Fragment 的唯一标识
        return fragments[position].hashCode().toLong()
    }

    override fun containsItem(itemId: Long): Boolean {
        // 检查 Fragment 是否已经存在
        return fragments.any { it.hashCode().toLong() == itemId }
    }
}