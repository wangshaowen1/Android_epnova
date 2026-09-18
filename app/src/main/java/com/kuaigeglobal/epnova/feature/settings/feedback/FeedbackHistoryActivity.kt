package com.kuaigeglobal.epnova.feature.settings.feedback

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.startRefreshWithAdapter
import com.kuaigeglobal.epnova.databinding.ActivityFeedbackHistoryBinding
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** 反馈历史列表页，负责分页展示、空态、刷新和详情入口。 */
class FeedbackHistoryActivity :
    BaseViewModelActivity<ActivityFeedbackHistoryBinding, FeedbackViewModel>() {

    private val feedbackHistoryAdapter by lazy {
        FeedbackHistoryAdapter { item ->
            item.feedbackId?.let { MyFeedbackActivity.start(this, it) }
        }
    }
    // 首次展示由分页流自动加载，后续恢复前台时再主动刷新，避免首次重复请求。
    private var hasResumed = false

    companion object {
        fun start(activity: Activity?) {
            activity ?: return
            activity.startActivity(Intent(activity, FeedbackHistoryActivity::class.java))
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        dataUpdate()
    }

    override fun onResume() {
        super.onResume()
        if (hasResumed) {
            mRealVM.refreshFeedbackHistory()
        }
        hasResumed = true
    }

    /** 将整页状态、分页页脚重试和下拉刷新接入同一个历史列表。 */
    private fun initView() {
        binding.lsv.setEmptyView(
            layoutInflater.inflate(R.layout.layout_feedback_history_empty, binding.lsv, false)
        )
        binding.rvFeedbackHistory.apply {
            itemAnimator = null
            adapter = feedbackHistoryAdapter.withLoadStateFooter(
                MyMultiStatePagingFooterAdapter(
                    hostAdapter = feedbackHistoryAdapter,
                    // 空列表由页面级空态展示，页脚不重复显示空态。
                    displayEmptyState = false,
                    noMoreThreshold = 1,
                ) {
                    feedbackHistoryAdapter.retry()
                }
            )
        }
        binding.srl.startRefreshWithAdapter(
            pagingDataAdapter = feedbackHistoryAdapter,
            loadStateHolder = binding.lsv
        )
    }

    /** 持续提交最新分页数据，收集任务随 Activity 销毁取消。 */
    private fun dataUpdate() {
        lifecycleScope.launch {
            mRealVM.feedbackHistory.collectLatest {
                feedbackHistoryAdapter.submitData(it)
            }
        }
    }
}
