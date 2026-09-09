package com.kuaigeglobal.epnova.feature.message

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.startRefreshWithAdapter
import com.kuaigeglobal.epnova.databinding.ActivityNotificationsBinding
import com.kuaigeglobal.epnova.router.AppRouter
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationsActivity :
    BaseViewModelActivity<ActivityNotificationsBinding, NotificationsViewModel>() {

    private val notificationsAdapter by lazy {
        NotificationsAdapter { item ->
            AppRouter.jump(item.jumpUri)
        }
    }

    companion object {
        fun start(activity: Activity?) {
            activity ?: return
            activity.startActivity(Intent(activity, NotificationsActivity::class.java))
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        dataUpdate()
    }

    override fun onResume() {
        super.onResume()
        mRealVM.readAllMessages()
    }

    private fun initView() {
        binding.lsv.setEmptyView(
            layoutInflater.inflate(R.layout.layout_notifications_empty, binding.lsv, false)
        )
        binding.rvNotifications.apply {
            itemAnimator = null
            adapter = notificationsAdapter.withLoadStateFooter(
                MyMultiStatePagingFooterAdapter(
                    hostAdapter = notificationsAdapter,
                    loadMoreLoadEnd = "",
                    displayEmptyState = false
                ) {
                    notificationsAdapter.retry()
                }
            )
        }
        binding.srl.startRefreshWithAdapter(
            pagingDataAdapter = notificationsAdapter,
            loadStateHolder = binding.lsv
        )
    }

    private fun dataUpdate() {
        lifecycleScope.launch {
            mRealVM.notifications.collectLatest { pagingData ->
                notificationsAdapter.submitData(pagingData)
            }
        }
    }
}
