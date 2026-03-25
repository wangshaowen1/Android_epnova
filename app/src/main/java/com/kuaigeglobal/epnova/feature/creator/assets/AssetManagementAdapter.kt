package com.kuaigeglobal.epnova.feature.creator.assets

import android.content.Context
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import com.chad.library.adapter.base.BaseMultiItemAdapter
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.nhsd.basic.item.QuickViewHolder

class AssetManagementAdapter : BaseMultiItemAdapter<AppItemViewModel<*>>() {

    init {
        addItemType(1, MultiItemAdapterListener(R.layout.item_asset_management))
        addItemType(2, MultiItemAdapterListener(R.layout.item_add_button))
        addItemType(3, MultiItemAdapterListener(R.layout.item_asset_management_title))
        onItemViewType { position, list ->
            when (list[position]) {
                is ItemAddVM -> 2
                is ItemTitleVM -> 3
                else -> 1
            }
        }
    }
}

class MultiItemAdapterListener(@LayoutRes private val layout: Int) : BaseMultiItemAdapter.OnMultiItemAdapterListener<AppItemViewModel<*>, QuickViewHolder> {
    override fun onBind(holder: QuickViewHolder, position: Int, item: AppItemViewModel<*>?) {
        holder.bind(item)
    }

    override fun onCreate(context: Context, parent: ViewGroup, viewType: Int): QuickViewHolder = QuickViewHolder.create(layout, parent)
}