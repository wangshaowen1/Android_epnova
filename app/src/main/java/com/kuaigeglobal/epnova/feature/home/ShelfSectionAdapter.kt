package com.kuaigeglobal.epnova.feature.home

import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.kuaigeglobal.epnova.core.network.apis.entity.BookShelfSectionInfo
import com.kuaigeglobal.epnova.databinding.ItemShelfContainerBinding
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.item.BindingViewHolder

/** 配置驱动的横滑区块，复用现有书架布局和作品 Adapter。 */
class ShelfSectionAdapter(
    private val onOpenShelf: (BookShelfSectionInfo) -> Unit
) : ListAdapter<ShelfSectionUiModel, ShelfSectionAdapter.ShelfViewHolder>(DIFF_CALLBACK) {

    private val scrollStates = mutableMapOf<Int, Parcelable?>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShelfViewHolder {
        return ShelfViewHolder(
            ItemShelfContainerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ShelfViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun onViewAttachedToWindow(holder: ShelfViewHolder) {
        super.onViewAttachedToWindow(holder)
        (holder.itemView.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.isFullSpan = true
    }

    override fun onViewRecycled(holder: ShelfViewHolder) {
        holder.recycle()
        super.onViewRecycled(holder)
    }

    inner class ShelfViewHolder(
        private val viewBinding: ItemShelfContainerBinding
    ) : BindingViewHolder<ItemShelfContainerBinding>(viewBinding) {

        private val innerAdapter = InnerHorizontalAdapter()
        private val innerLayoutManager = LinearLayoutManager(
            viewBinding.root.context, LinearLayoutManager.HORIZONTAL, false
        ).apply { initialPrefetchItemCount = 4 }
        private var boundSectionCode: Int? = null

        init {
            viewBinding.rvInnerHorizontal.apply {
                layoutManager = innerLayoutManager
                // 使用各自的回收池，避免 inner ViewHolder 跨 Adapter 复用后读取其他区块的数据。
                adapter = innerAdapter
            }
        }

        override fun bind(position: Int) {
            val item = getItem(position)
            val sectionCode = item.config.bookshelfSectionCode ?: return
            val changedSection = boundSectionCode != sectionCode
            if (changedSection) saveScrollState()
            boundSectionCode = sectionCode
            val savedState = scrollStates[sectionCode]
            viewBinding.tvShelfTitle.text = item.config.bookshelfName
            viewBinding.tvAll.setOnSingleClickListener { onOpenShelf(item.config) }
            viewBinding.dragContainer.setDragListener { onOpenShelf(item.config) }
            innerAdapter.submitList(item.works) {
                if (boundSectionCode == sectionCode && changedSection) {
                    if (savedState != null) {
                        innerLayoutManager.onRestoreInstanceState(savedState)
                    } else {
                        innerLayoutManager.scrollToPositionWithOffset(0, 0)
                    }
                }
            }
        }

        fun recycle() {
            saveScrollState()
            boundSectionCode = null
        }

        private fun saveScrollState() {
            boundSectionCode?.let { scrollStates[it] = innerLayoutManager.onSaveInstanceState() }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ShelfSectionUiModel>() {
            override fun areItemsTheSame(oldItem: ShelfSectionUiModel, newItem: ShelfSectionUiModel): Boolean {
                return oldItem.config.bookshelfSectionCode == newItem.config.bookshelfSectionCode
            }

            override fun areContentsTheSame(oldItem: ShelfSectionUiModel, newItem: ShelfSectionUiModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}
