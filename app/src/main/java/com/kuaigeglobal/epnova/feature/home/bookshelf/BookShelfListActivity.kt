package com.kuaigeglobal.epnova.feature.home.bookshelf

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.kuaigeglobal.epnova.common.ext.startRefreshWithAdapter
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.databinding.ActivityAllListBinding
import com.kuaigeglobal.epnova.feature.play.DramasPlayPagerActivity
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 书架作品列表页。
 *
 * 入口可传入书架分区编码；未传入时由页面从首页书架配置中选择目标书架。
 */
class BookShelfListActivity : BaseViewModelActivity<ActivityAllListBinding, BookshelfListViewModel>() {

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
        val bookshelfSectionCode = intent.getStringExtra(EXTRA_BOOKSHELF_SECTION_CODE)
        val bookshelfName = intent.getStringExtra(EXTRA_BOOKSHELF_NAME)
        val entryType = intent.getStringExtra(EXTRA_ENTRY_TYPE)
        bookshelfName?.takeIf { it.isNotBlank() }?.let(mRealVM.toolbar.mTitle::set)
        if (!entryType.isNullOrBlank()) {
            EventManager.onFirebaseTrack(
                FirebaseEvent.Category.HOME,
                FirebaseEvent.Action.HOME_SHELF_ALL_ENTER,
                bookshelfName,
                entryType
            )
        }
        mRealVM.load(bookshelfSectionCode, bookshelfName)
        lifecycleScope.launch {
            mRealVM.works.collectLatest { pagingData ->
                adapter.submitData(pagingData)
            }
        }
    }

    companion object {
        private const val EXTRA_BOOKSHELF_SECTION_CODE = "extra_bookshelf_section_code"
        private const val EXTRA_BOOKSHELF_NAME = "extra_bookshelf_name"
        private const val EXTRA_ENTRY_TYPE = "extra_shelf_entry_type"

        const val ENTRY_CLICK_ALL = "click all"
        const val ENTRY_SWIPE = "swipe"

        fun start(
            activity: Activity?,
            bookshelfSectionCode: String? = null,
            bookshelfName: String? = null,
            entryType: String? = null
        ) {
            activity ?: return
            activity.startActivity(
                Intent(activity, BookShelfListActivity::class.java).apply {
                    bookshelfSectionCode?.let { putExtra(EXTRA_BOOKSHELF_SECTION_CODE, it) }
                    bookshelfName?.let { putExtra(EXTRA_BOOKSHELF_NAME, it) }
                    entryType?.let { putExtra(EXTRA_ENTRY_TYPE, it) }
                }
            )
        }
    }
}
