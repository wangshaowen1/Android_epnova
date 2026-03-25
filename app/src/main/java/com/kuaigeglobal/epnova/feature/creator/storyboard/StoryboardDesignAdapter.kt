package com.kuaigeglobal.epnova.feature.creator.storyboard

import android.content.Context
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import com.chad.library.adapter.base.BaseMultiItemAdapter
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.feature.creator.assets.ItemAddVM
import com.nhsd.basic.item.QuickViewHolder

class StoryboardDesignAdapter : BaseMultiItemAdapter<AppItemViewModel<*>>() {

    init {
        addItemType(1, MultiItemAdapterListener(R.layout.item_storyboard_design_no_image))
        addItemType(2, MultiItemAdapterListener(R.layout.item_add_button))
        onItemViewType { position, list ->
            val item = list[position]
            when (item) {
                is ItemAddVM -> 2
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