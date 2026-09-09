package com.kuaigeglobal.epnova.feature.home

import android.app.Activity
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.kuaigeglobal.epnova.core.network.apis.response.Section
import com.kuaigeglobal.epnova.databinding.ItemShelfContainerBinding
import com.kuaigeglobal.epnova.feature.home.bookshelf.BookShelfListActivity
import com.nhsd.basic.ext.setOnSingleClickListener

class ShelfContainerAdapter : ListAdapter<Section, ShelfContainerAdapter.ShelfViewHolder>(DIFF_CALLBACK) {

    // 性能优化：共享内部 RecyclerView 的 ViewPool，减少对象创建
    private val viewPool = RecyclerView.RecycledViewPool()
    private val scrollStates = mutableMapOf<String, Parcelable?>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShelfViewHolder {
        val binding = ItemShelfContainerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShelfViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShelfViewHolder, position: Int) {
        val item = getItem(position)
        // 【关键】设置满跨度，让这个书架占满瀑布流的整行
        val layoutParams = holder.itemView.layoutParams
        if (layoutParams is StaggeredGridLayoutManager.LayoutParams) {
            layoutParams.isFullSpan = true
        }

        val savedState = scrollStates[item.bookshelfId]
        holder.bind(item, savedState)
    }

    override fun onViewRecycled(holder: ShelfViewHolder) {
        super.onViewRecycled(holder)
        val position = holder.bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
            val item = getItem(position)
            // 保存当前 LayoutManager 的滑动状态
            scrollStates[item.bookshelfId ?: ""] = holder.innerLayoutManager.onSaveInstanceState()
        }
    }

    inner class ShelfViewHolder(private val binding: ItemShelfContainerBinding) : RecyclerView.ViewHolder(binding.root) {

        // 优化：每个 ViewHolder 只初始化一次 InnerAdapter
        private val innerAdapter = InnerHorizontalAdapter()
        val innerLayoutManager = LinearLayoutManager(binding.root.context, LinearLayoutManager.HORIZONTAL, false).apply {
            // 优化：预加载几个屏幕外的 Item，提升横滑流畅度
            initialPrefetchItemCount = 4
        }

        init {
            binding.rvInnerHorizontal.apply {
                layoutManager = innerLayoutManager
                setRecycledViewPool(viewPool)
                adapter = innerAdapter
            }
        }

        fun bind(data: Section, savedState: Parcelable?) {
            binding.tvShelfTitle.text = data.bookshelfName
            binding.tvAll.setOnSingleClickListener {
                BookShelfListActivity.start(
                    activity = binding.root.context as? Activity,
                    bookshelfId = data.bookshelfId,
                    bookshelfName = data.bookshelfName
                )
            }
            // 使用带 callback 的 submitList，确保 Diff 计算并绑定完数据后再恢复位置
            innerAdapter.submitList(data.horizontalWorks) {
                if (savedState != null) {
                    innerLayoutManager.onRestoreInstanceState(savedState)
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Section>() {
            override fun areItemsTheSame(oldItem: Section, newItem: Section): Boolean {
                // 假设 Section 有个唯一标识符，如果没有，可以用 bookshelfName 替代
                return oldItem.bookshelfId == newItem.bookshelfId
            }

            override fun areContentsTheSame(oldItem: Section, newItem: Section): Boolean {
                return oldItem == newItem
            }
        }
    }
}
