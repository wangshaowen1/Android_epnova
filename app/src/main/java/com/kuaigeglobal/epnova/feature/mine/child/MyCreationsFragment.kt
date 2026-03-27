package com.kuaigeglobal.epnova.feature.mine.child

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingData
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.startRefreshWithAdapter
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.onUserStatusChanged
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo
import com.kuaigeglobal.epnova.databinding.FragmentMineChildBinding
import com.kuaigeglobal.epnova.databinding.FragmentMyCreationsBinding
import com.kuaigeglobal.epnova.feature.creator.generate.StoryEditorActivity
import com.kuaigeglobal.epnova.feature.play.DramasPlayPagerActivity
import com.kuaigeglobal.epnova.feature.play.data.FeedRepository
import com.kuaigeglobal.epnova.feature.series.SeriesActivity
import com.kuaigeglobal.epnova.feature.works.ItemMultipleChoiceEventsViewModel
import com.kuaigeglobal.epnova.feature.works.MyWorksAdapter
import com.kuaigeglobal.epnova.feature.works.MyWorksEvent
import com.kuaigeglobal.epnova.feature.works.MyWorksFragment.Companion.CLICK_INTERVAL
import com.kuaigeglobal.epnova.feature.works.MyWorksViewModel
import com.kuaigeglobal.epnova.router.AppRouter
import com.kuaigeglobal.epnova.widget.decoration.BottomSpaceDecoration
import com.kuaigeglobal.epnova.widget.decoration.SpaceDecoration
import com.kuaigeglobal.epnova.widget.stateful.applyGridSpanSizeLookup
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MyCreationsFragment :
    BaseViewModelFragment<FragmentMyCreationsBinding, MyWorksViewModel>() {

    companion object {
        fun newInstance(): MyCreationsFragment {
            val fragment = MyCreationsFragment()
            val bundle = Bundle()
            fragment.arguments = bundle
            return fragment
        }
    }

    private val myWorksAdapter by lazy {
        MyWorksAdapter(eventVm = eventVm)
    }
    private val eventVm by lazy {
        ItemMultipleChoiceEventsViewModel<MyWorksEvent, String>()
    }
    private var lastClickTime = 0L
    //当前点击的作品信息
    var workInfo: WorkInfo? = null
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

    override fun onResume() {
        super.onResume()
        // 1. 标记为显示状态
        mRealVM.isShow.set(true)
        // 2. 只有在当前 Tab 真正可见时才启动（如果是 ViewPager 里的 Fragment）
        if (userVisibleHint || !isHidden) {
            mRealVM.startChecking()
        }
    }

    override fun onPause() {
        super.onPause()
        // 1. 离开页面立即标记不可见
        mRealVM.isShow.set(false)
        // 2. 停止轮询协程
        mRealVM.stopCheckGenerating()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        mRealVM.isShow.set(!hidden)
        if (!hidden) {
            mRealVM.stopCheckGenerating()
        } else {
            mRealVM.startChecking()
            refresh()
        }
    }

    private fun refresh() {
        myWorksAdapter.refresh()
    }

    private fun initView() {
        binding.recyclerView.apply {
            val noMoreThreshold = 2
            itemAnimator = null
            val myMultiStatePagingFooterAdapter = MyMultiStatePagingFooterAdapter(
                myWorksAdapter,
                noMoreThreshold = noMoreThreshold,
                noData = getString(R.string.empty_creation_history)
            ) {
                myWorksAdapter.retry()
            }
            adapter = myWorksAdapter.withLoadStateFooter(myMultiStatePagingFooterAdapter)
            binding.recyclerView.applyGridSpanSizeLookup()
        }
        binding.srl.startRefreshWithAdapter(
            pagingDataAdapter = myWorksAdapter,
            loadStateHolder = binding.lsv,
            onRefreshListener = {
                mRealVM.startChecking()
            }
        )
    }


    private fun dataUpdate() {
        observeEvents {
            //登录状态变化
            onUserStatusChanged {
                if (userId == User.get().id) return@onUserStatusChanged
                userId = User.get().id

                lifecycleScope.launch {
                    myWorksAdapter.submitData(PagingData.empty())
                }
                // 不管是退登还是换号，直接重置流
                mRealVM.resetPagingForNewUser()
            }
        }

        lifecycleScope.launch {
            mRealVM.getWorkInformation().collectLatest {
                myWorksAdapter.submitData(it)
            }
        }

        lifecycleScope.launch {
            eventVm.events.collect { event ->
                when (event) {
                    is MyWorksEvent.ViewDetails -> {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastClickTime < CLICK_INTERVAL) return@collect
                        lastClickTime = currentTime

                        if (event.workInfo?.isNotCreatedEpisode() == true) {
                            //作品类型：3-小说上传 且是草稿状态
                            mRealVM.getWorkDtl(workId = event.workInfo.workId)
                            return@collect
                        }
                        if (event.workInfo?.totalEpisodeCnt == 1) {
                            //这个是单集的
                            if (event.workInfo.workStatus == WorkInfo.COMPLETED_STATE) {
                                //已经完成了，直接播放
                                workInfo = event.workInfo
                                mRealVM.getEpisodes(event.workInfo.workId)
                                return@collect
                            }
                            AppRouter.singleEpisodeSeriesDetails(event.workInfo)
                            return@collect
                        }
                        //查看详情
                        SeriesActivity.start(workId = event.workInfo?.workId)
                    }

                    is MyWorksEvent.ComposeState -> {
                        //编辑下选择
                        mRealVM.selectedCount.value = eventVm.getSelectedItems().size
                    }
                }
            }
        }

        mRealVM.workDtl.observe(this) {
            if (it == null) return@observe
            mRealVM.workDtl.value = null
            if (it.isInspirationUploadMode()) {
                //查看详情
                SeriesActivity.start(workId = it.workId)
                return@observe
            }
            StoryEditorActivity.restore(
                context = topActivity,
                step = 1,
                workId = it.workId,
                storyId = null,
                workTitle = it.workTitle,
                chapterIndex = 1,
                workType = it.workType,
                model = AppRouter.getModel(workType = it.workType, sortNo = 1),
                videoRatio = it.workRatio,
                from = StoryEditorActivity.FROM_MY_WORKS
            )
        }

        mRealVM.episodeList.observe(this) {
            if (it.isNullOrEmpty()) return@observe
            mRealVM.episodeList.value = null
            onPlayEpisodeClicked(workInfo = workInfo, episodes = it, clickPosition = 0)
        }
    }

    fun onPlayEpisodeClicked(workInfo: WorkInfo?, episodes: List<EpisodeInfo>, clickPosition: Int) {
        // 1. [核心步骤] 过滤未完成的数据 + 转换为 Adapter 需要的 WorkInfo
        val validPlayList = episodes
            .filter { !it.videoUrl.isNullOrEmpty() } // 过滤条件
            .map { episode ->
                episode.workId = workInfo?.workId
                // 复用父作品信息，包装成 WorkInfo
               episode
            }

        // 2. 计算修正后的点击位置
        // 因为过滤了数据，原来的 position 可能变了，需要找到点击的那个 episode 在新列表里的位置
        val clickedEpisodeId = episodes[clickPosition].storyId
        val newPosition =
            validPlayList.indexOfFirst { it.storyId == clickedEpisodeId }.coerceAtLeast(0)
        // 3. 存入 Repository
        FeedRepository.setPreloadedSeries(workInfo?.workId ?: "", validPlayList)

        // 4. 跳转播放页
        DramasPlayPagerActivity.start(
            workId = workInfo?.workId,
            // 传入修正后的位置
            position = newPosition
        )
    }
}