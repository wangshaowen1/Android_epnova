package com.kuaigeglobal.epnova.feature.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.kuaigeglobal.epnova.databinding.ItemWaterfallTitleBinding

class WaterfallTitleAdapter : RecyclerView.Adapter<WaterfallTitleAdapter.TitleViewHolder>() {

    var isVisible: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value) notifyItemInserted(0) else notifyItemRemoved(0)
            }
        }

    // 只需要显示一个标题
    override fun getItemCount(): Int = if (isVisible) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TitleViewHolder {
        val binding = ItemWaterfallTitleBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TitleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TitleViewHolder, position: Int) {
        // 【关键】强制该 Item 占据整行跨度
        val layoutParams = holder.itemView.layoutParams
        if (layoutParams is StaggeredGridLayoutManager.LayoutParams) {
            layoutParams.isFullSpan = true
        }
    }

    inner class TitleViewHolder(val binding: ItemWaterfallTitleBinding) : RecyclerView.ViewHolder(binding.root)
}