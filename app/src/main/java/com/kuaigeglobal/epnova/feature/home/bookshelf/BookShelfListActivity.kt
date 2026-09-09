package com.kuaigeglobal.epnova.feature.home.bookshelf

import android.app.Activity
import android.content.Intent
import androidx.paging.PagingData
import com.kuaigeglobal.epnova.common.ext.startRefreshWithAdapter
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo
import com.kuaigeglobal.epnova.databinding.ActivityAllListBinding
import com.kuaigeglobal.epnova.feature.play.DramasPlayPagerActivity
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelActivity

/**
 * 书架作品列表页。
 *
 * 入口可传入书架 ID；未传入时由页面从首页书架配置中选择目标书架。
 */
class BookShelfListActivity : BaseViewModelActivity<ActivityAllListBinding, BookshelfListViewModel>() {

    /** 接口联调前用于预览书架列表的本地数据。 */
    private val demoWorks = listOf(
        WorkVOInfo(
            workId = "demo-work-001",
            workTitle = "Bound by Vendetta: Sleeping with the Enemy",
            nickName = "Sarah",
            workDesc = "A hidden power awakens, pulling a young hero into a dangerous promise.",
            playCount = 7600,
            totalEpisodeCnt = 12
        ),
        WorkVOInfo(
            workId = "demo-work-002",
            workTitle = "Midnight Promise",
            nickName = "Emily",
            workDesc = "One promise, two hearts, and a secret that changes everything.",
            playCount = 12800,
            totalEpisodeCnt = 16
        ),
        WorkVOInfo(
            workId = "demo-work-003",
            workTitle = "The Last Wish",
            nickName = "Olivia",
            workDesc = "She gets one final wish and uses it to rewrite her unfinished story.",
            playCount = 4300,
            totalEpisodeCnt = 10
        ),
        WorkVOInfo(
            workId = "demo-work-004",
            workTitle = "A Contract with the CEO",
            nickName = "Lily",
            workDesc = "A temporary contract brings an unexpected romance into focus.",
            playCount = 21500,
            totalEpisodeCnt = 20
        )
    )

    private val adapter by lazy {
        BookshelfListAdapter { work ->
            DramasPlayPagerActivity.start(
                workId = work.workId,
                position = 0,
                isSeriesMode = false,
                source = FirebaseEvent.Value.SOURCE_HOME
            )
        }
    }

    override fun onViewModelCreated() {
        super.onViewModelCreated()
        binding.rvWorks.adapter = adapter.withLoadStateFooter(
            MyMultiStatePagingFooterAdapter(
                hostAdapter = adapter,
                displayEmptyState = false,
                refresh = { adapter.retry() }
            )
        )
        binding.srl.startRefreshWithAdapter(
            pagingDataAdapter = adapter,
            loadStateHolder = binding.lsv
        )
        adapter.submitData(lifecycle, PagingData.from(demoWorks))
        binding.lsv.showContent()
    }

    companion object {
        private const val EXTRA_BOOKSHELF_ID = "extra_bookshelf_id"
        private const val EXTRA_BOOKSHELF_NAME = "extra_bookshelf_name"

        fun start(
            activity: Activity?,
            bookshelfId: String? = null,
            bookshelfName: String? = null
        ) {
            activity ?: return
            activity.startActivity(
                Intent(activity, BookShelfListActivity::class.java).apply {
                    bookshelfId?.let { putExtra(EXTRA_BOOKSHELF_ID, it) }
                    bookshelfName?.let { putExtra(EXTRA_BOOKSHELF_NAME, it) }
                }
            )
        }
    }
}
