package com.kuaigeglobal.epnova.widget.stateful

import android.content.Context
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.widget.stateful.paging.MultiStatePagingFooterAdapter
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:
 * @Data: 2026/2/9/周一  10:28
 * @Version: 1.0
 * *****************************************
 */

fun RecyclerView.applyGridSpanSizeLookup() {
    val lm = layoutManager
    if (lm !is GridLayoutManager) return
    val oldLookup = lm.spanSizeLookup

    lm.spanSizeLookup =  object : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            val adapter = this@applyGridSpanSizeLookup.adapter

            if (adapter is ConcatAdapter) {
                val pair = adapter.getWrappedAdapterAndPosition(position)
                val wrappedAdapter = pair.first

                // 只要是 footer adapter，就占满
                if (wrappedAdapter is MultiStatePagingFooterAdapter) {
                    return lm.spanCount
                }
            }
            return oldLookup?.getSpanSize(position) ?: 1
        }
    }
}

