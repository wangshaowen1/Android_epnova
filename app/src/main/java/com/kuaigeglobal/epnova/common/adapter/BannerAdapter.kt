package com.kuaigeglobal.epnova.common.adapter

import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.nhsd.basic.viewmodel.VariableId
import com.nhsd.basic.item.QuickItemViewModel
import com.zhpan.bannerview.BaseBannerAdapter
import com.zhpan.bannerview.BaseViewHolder

/**
 * BannerViewPager适配器
 */
class BannerAdapter<T : QuickItemViewModel>(private val viewType: BannerViewType<T>) : BaseBannerAdapter<T>() {

    val indexedViewHolders by lazy { SparseArray<BannerViewHolder<T>>() }

    constructor(@LayoutRes layoutId: Int) : this(DefaultBannerViewType(layoutId))

    override fun createViewHolder(parent: ViewGroup, itemView: View?, viewType: Int): BaseViewHolder<T> {
        val binding = itemView?.let { DataBindingUtil.bind<ViewDataBinding>(it) }
        return BannerViewHolder(binding!!)
    }

    override fun bindData(holder: BaseViewHolder<T>?, data: T, position: Int, pageSize: Int) {
        (holder as? BannerViewHolder)?.let {
            indexedViewHolders.set(position, it)
            it.bind(data)
        }
    }

    override fun getLayoutId(viewType: Int): Int {
        return this.viewType.getLayoutId(viewType)
    }

    public override fun getViewType(position: Int): Int {
        return viewType.getViewType(position, mList.getOrNull(position))
    }
}

interface BannerViewType<T : QuickItemViewModel> {
    fun getViewType(position: Int, item: T?): Int

    @LayoutRes
    fun getLayoutId(viewType: Int): Int
}

class DefaultBannerViewType<T : QuickItemViewModel>(@LayoutRes private val layoutId: Int) :
    BannerViewType<T> {
    override fun getViewType(position: Int, item: T?): Int {
        return 1
    }

    override fun getLayoutId(viewType: Int): Int {
        return layoutId
    }
}

class BannerViewHolder<T : QuickItemViewModel>(val binding: ViewDataBinding) : BaseViewHolder<T>(binding.root) {
    fun <T : VariableId> bind(item: T?) {
        item?.id()?.let { binding.setVariable(it, item) }
        binding.executePendingBindings()
    }
}