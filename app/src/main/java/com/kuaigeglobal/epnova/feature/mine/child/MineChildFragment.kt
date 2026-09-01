package com.kuaigeglobal.epnova.feature.mine.child

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.startRefreshWithAdapter
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.onUserStatusChanged
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo
import com.kuaigeglobal.epnova.databinding.FragmentMineChildBinding
import com.kuaigeglobal.epnova.feature.play.DramasPlayPagerActivity
import com.kuaigeglobal.epnova.feature.works.ItemMultipleChoiceEventsViewModel
import com.kuaigeglobal.epnova.widget.decoration.GridSpacingItemDecoration
import com.kuaigeglobal.epnova.widget.stateful.applyGridSpanSizeLookup
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import kotlinx.coroutines.launch

/**
 * My List - History or Collection 观看历史或收藏列表
 */
class MineChildFragment :
    BaseViewModelFragment<FragmentMineChildBinding, MineChildViewModel>() {

    companion object {
        //充值明细
        const val TYPE_HISTORY = 1

        //消耗明细
        const val TYPE_COLLECTION = 2

        private const val CLICK_INTERVAL = 500L // 限制 500ms 内只能触发一次

        fun newInstance(type: Int = TYPE_HISTORY): MineChildFragment {
            val fragment = MineChildFragment()
            val bundle = Bundle()
            bundle.putInt(Constants.Extra.TYPE, type)
            fragment.arguments = bundle
            return fragment
        }
    }

    private val mineAdapter by lazy {
        MineChildAdapter(eventVm = eventVm, mRealVM.isCollect())
    }
    private val eventVm by lazy {
        ItemMultipleChoiceEventsViewModel<MyWorksVOEvent, String>()
    }
    private var lastClickTime = 0L

    //当前点击的作品信息
    var workInfo: WorkVOInfo? = null
    private var userId: String? = null

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        //数据更新
        dataUpdate()
    }

    override fun onStart() {
        super.onStart()
        refresh()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isShowReception()) {
            refresh()
        }
    }

    private fun refresh() {
        if(!User.get().isVisitor){
            mineAdapter.refresh()
        }
    }

    private fun initView() {
        binding.recyclerView.apply {
            addItemDecoration(GridSpacingItemDecoration(3, 8.dp, 0))
            val noMoreThreshold = 2
            itemAnimator = null
            val myMultiStatePagingFooterAdapter = MyMultiStatePagingFooterAdapter(
                mineAdapter,
                noMoreThreshold = noMoreThreshold,
                loadMoreLoadEnd = "",
                noData = getString(R.string.empty_creation_history)
            ) {
                mineAdapter.retry()
            }
            adapter = mineAdapter.withLoadStateFooter(myMultiStatePagingFooterAdapter)
            binding.recyclerView.applyGridSpanSizeLookup()
        }
        binding.srl.startRefreshWithAdapter(
            pagingDataAdapter = mineAdapter,
            loadStateHolder = binding.lsv,
            onRefreshListener = {
                mineAdapter.refresh()
            }
        )
    }


    private fun dataUpdate() {
        observeEvents {
            //登录状态变化
            onUserStatusChanged {
                if (userId == User.get().id) return@onUserStatusChanged
                userId = User.get().id
                refresh()
            }
        }

        lifecycleScope.launch {
            mRealVM.getWorkInfo().collect {
                mineAdapter.submitData(it)
            }
        }

        lifecycleScope.launch {
            eventVm.events.collect { event ->
                when (event) {
                    is MyWorksVOEvent.ViewDetails -> {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastClickTime < CLICK_INTERVAL) return@collect
                        lastClickTime = currentTime
                        onPlayEpisodeClicked(event.workInfo)
                    }

                    else -> {}
                }
            }
        }
    }

    fun onPlayEpisodeClicked(workInfo: WorkVOInfo?) {
        DramasPlayPagerActivity.start(
            workId = workInfo?.workId,
            position = maxOf(0, (workInfo?.lastSortNo ?: 0) - 1),
            isSeriesMode = true,
            source = if (mRealVM.isCollect()) {
                FirebaseEvent.Value.SOURCE_COLLECTION
            } else {
                FirebaseEvent.Value.SOURCE_HISTORY
            }
        )
    }
}
