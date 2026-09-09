package com.kuaigeglobal.epnova.feature.home

import androidx.lifecycle.lifecycleScope
import com.kuaigeglobal.epnova.common.ext.startRefreshWithAdapter
import com.kuaigeglobal.epnova.databinding.FragmentHomeAllDramasBinding
import com.kuaigeglobal.epnova.feature.home.bookshelf.BookshelfListAdapter
import com.kuaigeglobal.epnova.feature.home.bookshelf.BookshelfListViewModel
import com.kuaigeglobal.epnova.feature.play.DramasPlayPagerActivity
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** All Dramas 页，复用现有书架列表数据和卡片实现。 */
class AllDramasFragment :
    BaseViewModelFragment<FragmentHomeAllDramasBinding, BookshelfListViewModel>() {

    companion object {
        fun newInstance(): AllDramasFragment = AllDramasFragment()
    }

    private val adapter by lazy {
        BookshelfListAdapter { work ->
            DramasPlayPagerActivity.start(
                workId = work.workId,
                position = 0,
                isSeriesMode = false
            )
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
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
        viewLifecycleOwner.lifecycleScope.launch {
            mRealVM.works.collectLatest { adapter.submitData(it) }
        }
        mRealVM.load(bookshelfId = null, bookshelfName = null)
    }
}
