package com.kuaigeglobal.epnova.widget.stateful.paging


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.utils.NetworkUtil
import com.kuaigeglobal.epnova.databinding.MultiStateAdapterViewBinding
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
open class MultiStatePagingFooterAdapter(
    private val hostAdapter: RecyclerView.Adapter<*>? = null,
) : LoadStateAdapter<MultiStatePagingFooterAdapter.MultiStateVH>() {

    //网络异常提示内容
    var networkAnomaly = R.string.page_network_error.getResString()

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

    //无数据时否显示刷新按钮时按钮文字
    var noDataButtonText = R.string.refresh_to_retry.getResString()

    //异常时否显示刷新按钮（所有异常：网络+请求）
    var requestButtonDisplay: Boolean = false

    //异常时刷新按钮显示文字(true)还是图片(false)
    var requestButtonTextAndImg: Boolean = true

    //异常时显示刷新按钮时按钮文字
    var requestButtonText = R.string.refresh_to_retry.getResString()

    //异常时刷新按钮显示图片资源
    var requestButtonImg: Int? = null

    //设置低于多少的时候不显示没有更多了
    var noMoreThreshold: Int = 5

    //刷新回调
    var refresh: (() -> Unit)? = null

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
                refresh?.invoke()
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
        val itemCount = hostAdapter?.itemCount ?: -10
        when (loadState) {
            is LoadState.NotLoading -> {
                if (itemCount == 0) {
                    //异常处理
                    exceptionHandling(holder = holder, loadState = loadState)
                    return
                }
                holder.multiStateAdapterViewBinding.apply {
                    llLoadMoreLoading.visibility = View.GONE
                    llLoadMoreLoadFailSmall.visibility = View.GONE
                    if (hostAdapter != null && itemCount < noMoreThreshold) {
                        loadMoreLoadEnd.visibility = View.GONE
                    } else if (loadState.endOfPaginationReached) {
                        loadMoreLoadEnd.visibility = View.VISIBLE
                    } else {
                        loadMoreLoadEnd.visibility = View.GONE
                    }
                    llNullView.visibility = View.GONE
                }
            }

            is LoadState.Error -> {
                //异常处理
                if (itemCount == 0) {
                    exceptionHandling(holder = holder, loadState = loadState, error = loadState.error)
                }else{
                    holder.multiStateAdapterViewBinding.apply {
                        llLoadMoreLoading.visibility = View.GONE
                        llLoadMoreLoadFailSmall.visibility = View.VISIBLE
                        loadMoreLoadEnd.visibility = View.GONE
                        llNullView.visibility = View.GONE
                    }
                }
            }

            is LoadState.Loading -> {
                holder.multiStateAdapterViewBinding.apply {
                    llLoadMoreLoading.visibility = View.VISIBLE
                    llLoadMoreLoadFailSmall.visibility = View.GONE
                    loadMoreLoadEnd.visibility = View.GONE
                    llNullView.visibility = View.GONE
                }
            }
        }
    }

    /**
     * 异常处理
     * @param holder MultiStateVH
     */
    private fun exceptionHandling(
        holder: MultiStateVH,
        loadState: LoadState,
        error: Throwable? = null
    ) {
        holder.multiStateAdapterViewBinding.apply {
            llLoadMoreLoading.visibility = View.GONE
            llLoadMoreLoadFailSmall.visibility = View.GONE
            //是否是异常
            val isException = error != null || !NetworkUtil.isNetworkAvailable()

            if (isException) {
                if (requestButtonDisplay) {
                    if (requestButtonTextAndImg) {
                        tvRefresh.visibility = View.VISIBLE
                        ivRefresh.visibility = View.GONE
                        tvRefresh.text = requestButtonText
                    } else {
                        requestButtonImg?.let {
                            ivRefresh.setImageResource(it)
                        }
                        tvRefresh.visibility = View.GONE
                        ivRefresh.visibility = View.VISIBLE
                    }
                } else {
                    tvRefresh.visibility = View.GONE
                    ivRefresh.visibility = View.GONE
                }

                if (NetworkUtil.isNetworkAvailable()) {
                    requestExceptionSrc?.let { ivAsv.setImageResource(it) }
                    tvAsv.text = requestException
                    tvNetworkAnomaly.visibility = View.VISIBLE
                } else {
                    networkAnomalySrc?.let { ivAsv.setImageResource(it) }
                    tvAsv.text = (loadState as? LoadState.Error)?.error?.message ?: networkAnomaly
                    tvNetworkAnomaly.visibility = View.GONE
                }
            } else {
                if (noDataButtonDisplay) {
                    if (noDataButtonTextAndImg) {
                        tvRefresh.visibility = View.VISIBLE
                        ivRefresh.visibility = View.GONE
                        tvRefresh.text = noDataButtonText
                    } else {
                        noDataButtonImg?.let {
                            ivRefresh.setImageResource(it)
                        }
                        tvRefresh.visibility = View.GONE
                        ivRefresh.visibility = View.VISIBLE
                    }
                } else {
                    tvRefresh.visibility = View.GONE
                    ivRefresh.visibility = View.GONE
                }
                noDataSrc?.let { ivAsv.setImageResource(it) }
                tvAsv.text = noData
                tvNetworkAnomaly.visibility = View.GONE
            }

            if (itemCount > 0) {//有缓存数据的时候只显示缩略版尾巴
                llNullView.visibility = View.GONE
                llLoadMoreLoading.visibility = View.GONE
                llLoadMoreLoadFailSmall.visibility = View.VISIBLE
                loadMoreLoadEnd.visibility = View.GONE
                llNullView.visibility = View.GONE
            } else {
                loadMoreLoadEnd.visibility = View.GONE
                llNullView.visibility = View.VISIBLE
            }
        }
    }

    override fun getStateViewType(loadState: LoadState): Int = R.layout.multi_state_adapter_view

    //重写该方法，否则底部LoadStatus不展示，闪过
    override fun displayLoadStateAsItem(loadState: LoadState): Boolean {
        return loadState is LoadState.Loading ||
                loadState is LoadState.Error ||
                loadState is LoadState.NotLoading
    }
}