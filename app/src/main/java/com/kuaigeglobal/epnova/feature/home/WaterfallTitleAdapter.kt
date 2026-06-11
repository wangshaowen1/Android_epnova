package com.kuaigeglobal.epnova.feature.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.databinding.ItemWaterfallTitleBinding

class WaterfallTitleAdapter : RecyclerView.Adapter<WaterfallTitleAdapter.TitleViewHolder>() {

    private var title: String? = null

    var isVisible: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value) notifyItemInserted(0) else notifyItemRemoved(0)
            }
        }

    fun setTitle(title: String?) {
        val newTitle = title?.takeIf { it.isNotBlank() }
        if (this.title != newTitle) {
            this.title = newTitle
            if (isVisible) {
                notifyItemChanged(0)
            }
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
        holder.bind(title)
    }

    inner class TitleViewHolder(val binding: ItemWaterfallTitleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String?) {
            binding.tvTitle.text = title ?: binding.root.context.getString(R.string.home_search_tab_recommended_for_u)
        }
    }
}
