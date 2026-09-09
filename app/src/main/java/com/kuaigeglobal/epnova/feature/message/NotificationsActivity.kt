package com.kuaigeglobal.epnova.feature.message

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.paging.PagingData
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.startRefreshWithAdapter
import com.kuaigeglobal.epnova.databinding.ActivityNotificationsBinding
import com.kuaigeglobal.epnova.feature.creator.generate.StoryEditorActivity
import com.kuaigeglobal.epnova.feature.play.DramasPlayPagerActivity
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelActivity

class NotificationsActivity :
    BaseViewModelActivity<ActivityNotificationsBinding, NotificationsViewModel>() {

    private val notificationsAdapter by lazy { NotificationsAdapter(::onNotificationClick) }

    /** 接口接入前用于预览通知列表的本地数据。 */
    private val demoNotifications = listOf(
        NotificationItem(
            title = "🎉 Your First Viewer!",
            content = "Your drama is live! EpNova is now finding your first viewers.",
            workTitle = "《I Am Not a God》",
            timeText = "July 23, 2026",
            type = NotificationItem.TYPE_WORK,
            workId = "demo-work-001"
        ),
        NotificationItem(
            title = "Your story is gaining attention",
            content = "More viewers watched your latest episode today.",
            workTitle = "《I Am Not a God》",
            timeText = "July 22, 2026",
            type = NotificationItem.TYPE_STORY,
            workId = "demo-work-001",
            storyId = "demo-story-002"
        ),
        NotificationItem(
            title = "New episode published",
            content = "Episode 6 is now available to your audience.",
            workTitle = "《Midnight Promise》",
            timeText = "July 21, 2026"
        ),
        NotificationItem(
            title = "Creator milestone reached",
            content = "Congratulations! Your series has reached 1,000 views.",
            workTitle = "《The Last Wish》",
            timeText = "July 20, 2026"
        )
    )

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

    private fun onNotificationClick(item: NotificationItem) {
        when (item.type) {
            NotificationItem.TYPE_WORK -> {
                val workId = item.workId ?: return
                DramasPlayPagerActivity.start(
                    workId = workId,
                    position = 0,
                    isSeriesMode = true
                )
            }

            NotificationItem.TYPE_STORY -> {
                val workId = item.workId ?: return
                val storyId = item.storyId ?: return
                StoryEditorActivity.startContinue(
                    context = this,
                    workId = workId,
                    workTitle = item.workTitle,
                    storyId = storyId,
                    chapterIndex = 1,
                    from = StoryEditorActivity.FROM_SERIES
                )
            }
        }
    }

    private fun dataUpdate() {
        notificationsAdapter.submitData(
            lifecycle,
            PagingData.from(demoNotifications)
        )
        binding.lsv.showContent()
    }
}
