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

class FeedbackHistoryActivity :
    BaseViewModelActivity<ActivityFeedbackHistoryBinding, FeedbackViewModel>() {

    private val feedbackHistoryAdapter by lazy {
        FeedbackHistoryAdapter { item ->
            MyFeedbackActivity.start(this, item.id)
        }
    }
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

    private fun initView() {
        binding.lsv.setEmptyView(
            layoutInflater.inflate(R.layout.layout_feedback_history_empty, binding.lsv, false)
        )
        binding.rvFeedbackHistory.apply {
            itemAnimator = null
            adapter = feedbackHistoryAdapter.withLoadStateFooter(
                MyMultiStatePagingFooterAdapter(
                    hostAdapter = feedbackHistoryAdapter,
                    loadMoreLoadEnd = "",
                    displayEmptyState = false
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

    private fun dataUpdate() {
        lifecycleScope.launch {
            mRealVM.feedbackHistory.collectLatest {
                feedbackHistoryAdapter.submitData(it)
            }
        }
    }
}
