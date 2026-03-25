package com.kuaigeglobal.epnova.widget.stateful

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.loadState.LoadState
import com.chad.library.adapter.base.loadState.trailing.TrailingLoadStateAdapter
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.utils.NetworkUtil
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.databinding.MultiStateAdapterViewBinding
import com.nhsd.basic.ext.getResString

/**
 * *****************************************
 * 项目名称: KgQuan
 * @Author: SZYMN
 * @Introduce:多状态视图
 * @Data: 2025/7/7/周一  11:23
 * @Version: 1.0
 * @param isLoadEndDisplay 是否还有更多
 * *****************************************
 */
open class MultiStateAdapter(
    private val adapter: BaseQuickAdapter<*, *>? = null,
    isLoadEndDisplay: Boolean = true
) :
    TrailingLoadStateAdapter<MultiStateAdapter.MultiStateVH>(isLoadEndDisplay) {

    //网络异常提示内容
    var networkAnomaly =R.string.page_network_error.getResString()

    //请求异常提示内容
    var requestException = R.string.page_load_error.getResString()

    //无数据提示内容
    var noData: CharSequence = R.string.page_empty.getResString()

    //没有更多了提示
    var loadMoreLoadEnd = R.string.no_more_data.getResString()

    //网络异常提示图片
    var networkAnomalySrc: Int? = null

    //请求异常提示图片
    var requestExceptionSrc: Int? = null

    //无数据提示图片
    var noDataSrc: Int? = null

    //无数据时否显示刷新按钮
    var noDataButtonDisplay: Boolean = false

    //无数据时刷新按钮显示文字(true)还是图片(false)
    var noDataButtonTextAndImg: Boolean = true

    //无数据时刷新按钮显示图片资源
    var noDataButtonImg: Int? = null

    //异常时否显示刷新按钮（所有异常：网络+请求）
    var requestButtonDisplay: Boolean = false

    //异常时刷新按钮显示文字(true)还是图片(false)
    var requestButtonTextAndImg: Boolean = true

    //异常时否显示刷新按钮时按钮文字
    var requestButtonText = R.string.refresh_to_retry.getResString()

    //无数据时否显示刷新按钮时按钮文字
    var noDataButtonText = R.string.refresh_to_retry.getResString()

    //最少多少才显示没有更多
    var minLoadMoreLoadEnd = 4

    //刷新回调
    var refresh: (() -> Unit)? = null

    class NoDataException : Throwable()

    class NoDataFailException(val error: Throwable?) : Throwable()

    class MultiStateVH(
        parent: ViewGroup,
        val multiStateAdapterViewBinding: MultiStateAdapterViewBinding = MultiStateAdapterViewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    ) : RecyclerView.ViewHolder(multiStateAdapterViewBinding.root)

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): MultiStateVH {
        return MultiStateVH(parent).apply {
            multiStateAdapterViewBinding.tvRefresh.setOnClickListener {
                // 失败重试点击事件
                refresh?.invoke()
            }
            multiStateAdapterViewBinding.ivRefresh.setOnClickListener {
                // 失败重试点击事件
                refresh?.invoke()
            }
            multiStateAdapterViewBinding.llLoadMoreLoadFailSmall.setOnClickListener {
                // 失败重试点击事件
                invokeLoadMore()
            }
        }
    }

    /**
     * bind LoadState
     *
     * 绑定加载状态
     */
    override fun onBindViewHolder(holder: MultiStateVH, loadState: LoadState) {
        holder.multiStateAdapterViewBinding.loadMoreLoadComplete.text = loadMoreLoadEnd
        val items = adapter?.items?.size ?: -10
        when (loadState) {
            is LoadState.NotLoading -> {
                holder.multiStateAdapterViewBinding.llLoadMoreLoading.visibility = View.GONE
                holder.multiStateAdapterViewBinding.llLoadMoreLoadFailSmall.visibility = View.GONE
                holder.multiStateAdapterViewBinding.llNullView.visibility = View.GONE
                holder.multiStateAdapterViewBinding.llNullView.visibility = View.GONE
                if (loadState.endOfPaginationReached) {
                    //没有更多了
                    holder.multiStateAdapterViewBinding.loadMoreLoadEnd.visibility =
                        if (items > 0 && items < minLoadMoreLoadEnd) {
                            View.GONE
                        } else if (isLoadEndDisplay) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                } else {
                    holder.multiStateAdapterViewBinding.loadMoreLoadEnd.visibility =
                        if (items > 0 && items < minLoadMoreLoadEnd) {
                            View.GONE
                        } else if (isLoadEndDisplay) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                }
            }

            is LoadState.Loading -> {
                holder.multiStateAdapterViewBinding.llLoadMoreLoading.visibility = View.VISIBLE
                holder.multiStateAdapterViewBinding.llLoadMoreLoadFailSmall.visibility = View.GONE
                holder.multiStateAdapterViewBinding.loadMoreLoadEnd.visibility = View.GONE
                holder.multiStateAdapterViewBinding.llNullView.visibility = View.GONE
                holder.multiStateAdapterViewBinding.tvNetworkAnomaly.visibility=View.GONE
            }

            is LoadState.Error -> {
                holder.multiStateAdapterViewBinding.llLoadMoreLoading.visibility = View.GONE
                holder.multiStateAdapterViewBinding.llLoadMoreLoadFailSmall.visibility = View.GONE
                when (val error = loadState.error) {
                    is NoDataException -> {
                        if (noDataButtonDisplay) {
                            if (noDataButtonTextAndImg) {
                                holder.multiStateAdapterViewBinding.tvRefresh.visibility = View.VISIBLE
                                holder.multiStateAdapterViewBinding.ivRefresh.visibility = View.GONE
                                holder.multiStateAdapterViewBinding.tvRefresh.text =
                                    noDataButtonText
                            } else {
                                noDataButtonImg?.let {
                                    holder.multiStateAdapterViewBinding.ivRefresh.setImageResource(
                                        it
                                    )
                                }
                                holder.multiStateAdapterViewBinding.tvRefresh.visibility = View.GONE
                                holder.multiStateAdapterViewBinding.ivRefresh.visibility =
                                    View.VISIBLE
                            }
                        } else {
                            holder.multiStateAdapterViewBinding.tvRefresh.visibility = View.GONE
                            holder.multiStateAdapterViewBinding.ivRefresh.visibility = View.GONE

                        }
                        noDataSrc?.let {
                            holder.multiStateAdapterViewBinding.ivAsv.setImageResource(
                                it
                            )
                        }
                        holder.multiStateAdapterViewBinding.tvAsv.text = noData

                        holder.multiStateAdapterViewBinding.loadMoreLoadEnd.visibility = View.GONE
                        holder.multiStateAdapterViewBinding.llNullView.visibility = View.VISIBLE
                        holder.multiStateAdapterViewBinding.tvNetworkAnomaly.visibility=View.GONE
                    }

                    is NoDataFailException -> {
                        if (requestButtonDisplay) {
                            if (requestButtonTextAndImg) {
                                holder.multiStateAdapterViewBinding.tvRefresh.visibility =
                                    View.VISIBLE
                                holder.multiStateAdapterViewBinding.ivRefresh.visibility = View.GONE
                                holder.multiStateAdapterViewBinding.tvRefresh.text =
                                    requestButtonText
                            } else {
                                noDataButtonImg?.let {
                                    holder.multiStateAdapterViewBinding.ivRefresh.setImageResource(
                                        it
                                    )
                                }
                                holder.multiStateAdapterViewBinding.tvRefresh.visibility = View.GONE
                                holder.multiStateAdapterViewBinding.ivRefresh.visibility =
                                    View.VISIBLE
                            }
                        } else {
                            holder.multiStateAdapterViewBinding.tvRefresh.visibility = View.GONE
                            holder.multiStateAdapterViewBinding.ivRefresh.visibility = View.GONE
                        }

                        if (NetworkUtil.isNetworkAvailable()) {
                            networkAnomalySrc?.let {
                                holder.multiStateAdapterViewBinding.ivAsv.setImageResource(
                                    it
                                )
                            }
                            holder.multiStateAdapterViewBinding.tvAsv.text =
                                (error.error as? ApiException)?.message ?: requestException
                            holder.multiStateAdapterViewBinding.tvNetworkAnomaly.visibility=View.VISIBLE
                        } else {
                            requestExceptionSrc?.let {
                                holder.multiStateAdapterViewBinding.ivAsv.setImageResource(
                                    it
                                )
                            }
                            holder.multiStateAdapterViewBinding.tvAsv.text = networkAnomaly
                            holder.multiStateAdapterViewBinding.tvNetworkAnomaly.visibility=View.GONE
                        }

                        holder.multiStateAdapterViewBinding.loadMoreLoadEnd.visibility = View.GONE
                        holder.multiStateAdapterViewBinding.llNullView.visibility = View.VISIBLE
                    }

                    else -> {
                        holder.multiStateAdapterViewBinding.llLoadMoreLoading.visibility = View.GONE
                        holder.multiStateAdapterViewBinding.llLoadMoreLoadFailSmall.visibility = View.GONE
                        holder.multiStateAdapterViewBinding.loadMoreLoadEnd.visibility = View.GONE
                        holder.multiStateAdapterViewBinding.llNullView.visibility = View.GONE
                        holder.multiStateAdapterViewBinding.tvNetworkAnomaly.visibility=View.GONE
                    }
                }
            }

            is LoadState.None -> {
                holder.multiStateAdapterViewBinding.llLoadMoreLoading.visibility = View.GONE
                holder.multiStateAdapterViewBinding.llLoadMoreLoadFailSmall.visibility = View.GONE
                holder.multiStateAdapterViewBinding.loadMoreLoadEnd.visibility = View.GONE
                holder.multiStateAdapterViewBinding.llNullView.visibility = View.GONE
            }
        }
    }

    override fun getStateViewType(loadState: LoadState): Int = R.layout.multi_state_adapter_view
}