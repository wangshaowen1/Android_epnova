package com.kuaigeglobal.epnova.widget

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.chad.library.adapter.base.loadState.LoadState
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.updateLayoutParams
import com.kuaigeglobal.epnova.core.databinding.singleClick
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor

/**
 * 加载更多
 * @author xiaoowuu
 */
open class MyTrackTrailingLoadStateAdapter(
    @DrawableRes private val emptyDrawableRes: Int? = null,
    private val emptyText: String? = null,
    private val emptyButton: String? = null,
    private val emptyClick: (() -> Unit)? = null,
    private val emptyIconTopMargin: Int = 80.dp,
    @ColorRes
    private val textColor: Int = R.color.color_6a6a6a
) : DefaultTrailingLoadStateAdapter() {

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): TrailingLoadStateVH {
        val holder = super.onCreateViewHolder(parent, loadState)
        emptyDrawableRes?.let { holder.viewBinding.loadMoreLoadEmptyViewIcon.setImageResource(it) }
        emptyText?.let { holder.viewBinding.loadMoreLoadEmptyViewText.text = it }
        val emptyButton = emptyButton
        if (!emptyButton.isNullOrBlank()) {
            holder.viewBinding.loadMoreLoadEmptyViewRetry.apply {
                visibility = View.VISIBLE
                text = emptyButton
                singleClick(this) { emptyClick?.invoke() }
            }
        }
        holder.viewBinding.loadMoreLoadEmptyViewIcon.updateLayoutParams<MarginLayoutParams> { it.topMargin = emptyIconTopMargin }
        holder.viewBinding.loadMoreLoadFailViewIcon.updateLayoutParams<MarginLayoutParams> { it.topMargin = emptyIconTopMargin }


        val textColor = textColor.getResColor()
        holder.viewBinding.loadMoreLoadEmptyViewText.setTextColor(textColor)
        holder.viewBinding.loadMoreLoadFailViewText.setTextColor(textColor)
        holder.viewBinding.loadMoreLoadingViewText.setTextColor(textColor)
        holder.viewBinding.loadMoreLoadEndViewText.setTextColor(textColor)
        holder.viewBinding.loadMoreLoadCompleteViewText.setTextColor(textColor)
        holder.viewBinding.loadMoreLoadFailSmallViewText.setTextColor(textColor)

        return holder
    }

    override fun onViewAttachedToWindow(holder: TrailingLoadStateVH) {
        super.onViewAttachedToWindow(holder)
        val layoutParams = holder.itemView.layoutParams
        if (layoutParams is StaggeredGridLayoutManager.LayoutParams) {
            layoutParams.isFullSpan = true
        }
    }
}