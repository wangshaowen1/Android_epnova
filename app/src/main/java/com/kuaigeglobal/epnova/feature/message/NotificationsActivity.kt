package com.kuaigeglobal.epnova.feature.message

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.startRefreshWithAdapter
import com.kuaigeglobal.epnova.databinding.ActivityNotificationsBinding
import com.kuaigeglobal.epnova.router.AppRouter
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** 消息中心列表页，负责分页列表、空态、刷新以及消息跳转入口。 */
class NotificationsActivity :
    BaseViewModelActivity<ActivityNotificationsBinding, NotificationsViewModel>() {

    private val notificationsAdapter by lazy {
        NotificationsAdapter { item ->
            Log.d("NotificationsAdapter",item.jumpUri+"")
            // 没有地址或接口明确禁止跳转时，保留在消息页并提示用户当前消息不可打开。
            if(item.jumpUri.isNullOrEmpty() ||  item.canJump == 0){
                showToast(R.string.message_cannot_open)
                return@NotificationsAdapter
            }
            AppRouter.jump(item.jumpUri)
        }
    }

    companion object {
        /** 从已有 Activity 打开消息中心；调用方为空时不执行跳转。 */
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

    /** 接入页面空态、Paging 页脚和下拉刷新，共用同一套加载状态。 */
    private fun initView() {
        binding.lsv.setEmptyView(
            layoutInflater.inflate(R.layout.layout_notifications_empty, binding.lsv, false)
        )
        binding.rvNotifications.apply {
            itemAnimator = null
            adapter = notificationsAdapter.withLoadStateFooter(
                MyMultiStatePagingFooterAdapter(
                    hostAdapter = notificationsAdapter,
                    // 空列表由 LoadStateViewHolder 展示，页脚只负责追加加载状态和重试。
                    displayEmptyState = false,
                    noMoreThreshold = 1,
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

    /** 持续收集 ViewModel 的分页流，提交给适配器并由生命周期自动取消。 */
    private fun dataUpdate() {
        lifecycleScope.launch {
            mRealVM.notifications.collectLatest { pagingData ->
                notificationsAdapter.submitData(pagingData)
            }
        }
    }
}
