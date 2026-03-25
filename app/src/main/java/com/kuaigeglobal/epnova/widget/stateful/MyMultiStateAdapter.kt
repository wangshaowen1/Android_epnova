package com.kuaigeglobal.epnova.widget.stateful

import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.loadState.LoadState
import com.kuaigeglobal.epnova.R
import com.nhsd.basic.ext.getResString

/**
 * *****************************************
 * 项目名称: KgQuan
 * @Author: SZYMN
 * @Introduce:多状态视图
 * @Data: 2025/7/7/周一  11:23
 * @Version: 1.0
 * *****************************************
 */
open class MyMultiStateAdapter(
    adapter: BaseQuickAdapter<*, *>? = null,
    //网络异常提示内容
    networkAnomaly: String = R.string.page_network_error.getResString(),
    //请求异常提示内容
    requestException: String = R.string.page_load_error.getResString(),
    //无数据提示内容
    noData: CharSequence = R.string.page_empty.getResString(),
    //没有更多了提示
    loadMoreLoadEnd: String = R.string.no_more_data.getResString(),
    //网络异常提示图片
    @DrawableRes networkAnomalySrc: Int? = null,
    //请求异常提示图片
    @DrawableRes requestExceptionSrc: Int? = null,
    //无数据提示图片
    @DrawableRes noDataSrc: Int? = null,
    //无数据时否显示刷新按钮
    noDataButtonDisplay: Boolean = false,
    //无数据时刷新按钮显示文字(true)还是图片(false)
    noDataButtonTextAndImg: Boolean = true,
    //无数据时刷新按钮显示图片资源
    @DrawableRes noDataButtonImg: Int? = null,
    //异常时否显示刷新按钮（所有异常：网络+请求）
    requestButtonDisplay: Boolean = false,
    //异常时刷新按钮显示文字(true)还是图片(false)
    requestButtonTextAndImg: Boolean = true,
    //异常时否显示刷新按钮时按钮文字
    requestButtonText: String = R.string.refresh_to_retry.getResString(),
    //无数据时否显示刷新按钮时按钮文字
    noDataButtonText: String = R.string.refresh_to_retry.getResString(),
    //空数据高度
    val displayedHeight: Int = -1,
    //刷新回调
    refresh: (() -> Unit)? = null,
    //是否显示加载结束
    isLoadEndDisplay: Boolean = true,
    //加载更多预值
    preloadSize: Int = 4,
) : MultiStateAdapter(
    adapter = adapter, isLoadEndDisplay
) {
    init {
        super.networkAnomaly = networkAnomaly
        super.requestException = requestException
        super.noData = noData
        super.loadMoreLoadEnd = loadMoreLoadEnd
        super.networkAnomalySrc = networkAnomalySrc
        super.requestExceptionSrc = requestExceptionSrc
        super.noDataSrc = noDataSrc
        super.noDataButtonDisplay = noDataButtonDisplay
        super.noDataButtonTextAndImg = noDataButtonTextAndImg
        super.noDataButtonImg = noDataButtonImg
        super.requestButtonDisplay = requestButtonDisplay
        super.requestButtonTextAndImg = requestButtonTextAndImg
        super.requestButtonText = requestButtonText
        super.noDataButtonText = noDataButtonText
        super.refresh = refresh
        super.preloadSize = preloadSize
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): MultiStateVH {
        val holder = super.onCreateViewHolder(parent, loadState)
        if (displayedHeight != -1) {
            holder.multiStateAdapterViewBinding.llNullView.layoutParams.height = displayedHeight
        }
        return holder
    }

    override fun onViewAttachedToWindow(holder: MultiStateVH) {
        super.onViewAttachedToWindow(holder)
        val layoutParams = holder.itemView.layoutParams
        if (layoutParams is StaggeredGridLayoutManager.LayoutParams) {
            layoutParams.isFullSpan = true
        }
    }
}