package com.kuaigeglobal.epnova.feature.works

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ActivityUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.startRefreshWithAdapter
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.onUserStatusChanged
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorEpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlaySource
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorWorkInfo
import com.kuaigeglobal.epnova.databinding.FragmentMyCreationsBinding
import com.kuaigeglobal.epnova.feature.creator.continuewriting.ContinueWritingActivity
import com.kuaigeglobal.epnova.feature.creator.generate.StoryEditorActivity
import com.kuaigeglobal.epnova.feature.creatorplay.CreatorPlayerActivity
import com.kuaigeglobal.epnova.feature.creatorplay.data.WorkSeriesCreatorFeedRepository
import com.kuaigeglobal.epnova.feature.main.MainActivity
import com.kuaigeglobal.epnova.feature.series.SeriesActivity
import com.kuaigeglobal.epnova.router.AppRouter
import com.kuaigeglobal.epnova.widget.decoration.SpaceDecoration
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * My List - My Creations 我的作品
 */
class MyCreationsFragment :
    BaseViewModelFragment<FragmentMyCreationsBinding, MyWorksViewModel>() {

    companion object {
        private const val TAG = "MyCreationsFragment"
        private const val CLICK_INTERVAL = 500L // 限制 500ms 内只能触发一次

        fun newInstance(): MyCreationsFragment {
            val fragment = MyCreationsFragment()
            val bundle = Bundle()
            fragment.arguments = bundle
            return fragment
        }

        @JvmStatic
        fun start() {
            FlowEventBus.postSync(Events.MainTabEvent(Events.MainTabEvent.TAB_MY_CREATIONS))
            ActivityUtils.finishToActivity(MainActivity::class.java, false)
        }
    }

    private val myCreationsAdapter by lazy {
        MyCreationsAdapter(eventVm = eventVm)
    }

    private val eventVm by lazy {
        ItemMultipleChoiceEventsViewModel<MyWorksEvent, String>()
    }
    private var lastClickTime = 0L
    //当前点击的作品信息
    var workInfo: CreatorWorkInfo? = null
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
        myCreationsAdapter.refresh()
    }

    private fun initView() {
        binding.recyclerView.apply {
            val noMoreThreshold = 2
            itemAnimator = null
            val myMultiStatePagingFooterAdapter = MyMultiStatePagingFooterAdapter(
                myCreationsAdapter,
                noMoreThreshold = noMoreThreshold,
                loadMoreLoadEnd = "",
                noData = getString(R.string.empty_creation_history)
            ) {
                myCreationsAdapter.retry()
            }
            adapter = myCreationsAdapter.withLoadStateFooter(myMultiStatePagingFooterAdapter)
            layoutManager = LinearLayoutManager(mActivity, RecyclerView.VERTICAL, false)
            addItemDecoration(SpaceDecoration(12.dp, SpaceDecoration.Direction.MIDDLE))
        }
        binding.srl.startRefreshWithAdapter(
            pagingDataAdapter = myCreationsAdapter,
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
                    myCreationsAdapter.submitData(PagingData.empty())
                }
                // 不管是退登还是换号，直接重置流
                mRealVM.resetPagingForNewUser()
            }
            on<Events.WorkPublishChangedEvent> {
                refresh()
            }
        }

        lifecycleScope.launch {
            mRealVM.getWorkInformation().collectLatest {
                myCreationsAdapter.submitData(it)
            }
        }

        lifecycleScope.launch {
            eventVm.events.collect { event ->
                when (event) {
                    is MyWorksEvent.ViewDetails -> {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastClickTime < CLICK_INTERVAL) return@collect
                        lastClickTime = currentTime

                        when (event.target) {
                            MyWorksEvent.ViewDetails.Target.ITEM, MyWorksEvent.ViewDetails.Target.ALL_LIST -> {
                                val entry = if (event.target == MyWorksEvent.ViewDetails.Target.ITEM) {
                                    FirebaseEvent.Value.MY_LIST_ENTRY_CARD
                                } else {
                                    FirebaseEvent.Value.MY_LIST_ENTRY_ALL_LIST_BTN
                                }
                                EventManager.onFirebaseTrack(
                                    FirebaseEvent.Category.MY_LIST,
                                    FirebaseEvent.Action.GOTO_EPISODE_LIST,
                                    entry
                                )
                                openWork(event.workInfo)
//                                SeriesActivity.start(workId = event.workInfo?.workId)
                            }
                            MyWorksEvent.ViewDetails.Target.PUBLISH_TIP -> {
                                // 发布
                            }
                            MyWorksEvent.ViewDetails.Target.CONTINUE_CREATING -> {
                                EventManager.onFirebaseTrack(
                                    FirebaseEvent.Category.MY_LIST,
                                    FirebaseEvent.Action.CONTINUE_CREATING_CLICK
                                )
                                continueCreating(event.workInfo)
                            }
                        }
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

        mRealVM.continueEpisodeList.observe(this) { result ->
            result ?: return@observe
            mRealVM.continueEpisodeList.value = null
            openContinueEditor(result.first, result.second)
        }

        mRealVM.episodeList.observe(this) {
            if (it.isNullOrEmpty()) return@observe
            mRealVM.episodeList.value = null
            onPlayEpisodeClicked(workInfo = workInfo, episodes = it, clickPosition = 0)
        }
    }

    // 老逻辑
    private fun openWork(workInfo: CreatorWorkInfo?) {
        if (workInfo?.isNotCreatedEpisode() == true) {
            //作品类型：3-小说上传且为草稿时，先获取详情再恢复创作。
            mRealVM.getWorkDtl(workId = workInfo.workId)
            return
        }
        if (workInfo?.totalEpisodeCnt == 1) {
            if (workInfo.workStatus == CreatorWorkInfo.COMPLETED_STATE) {
                //单集已完成时直接播放。
                this.workInfo = workInfo
                mRealVM.getEpisodes(workInfo.workId)
                return
            }
            AppRouter.singleEpisodeSeriesDetails(workInfo)
            return
        }
        SeriesActivity.start(workId = workInfo?.workId)
    }

    private fun continueCreating(workInfo: CreatorWorkInfo?) {
        val info = workInfo ?: return
        val total = info.totalEpisodeCnt ?: 0
        val finished = info.finishedEpisodeCnt ?: 0
        if (total in 1..finished) {
            showToast(R.string.my_creations_all_over)
//            SeriesActivity.start(workId = info.workId)
            return
        }
        if (info.isNotCreatedEpisode()) {
            mRealVM.getWorkDtl(workId = info.workId)
            return
        }
        if (info.totalEpisodeCnt == 1) {
            AppRouter.singleEpisodeSeriesDetails(info)
            return
        }
        mRealVM.getEpisodesForContinue(info)
    }

    private fun openContinueEditor(
        workInfo: CreatorWorkInfo,
        episodes: List<CreatorEpisodeInfo>
    ) {
        val sortedEpisodes = episodes.sortedBy { it.sortNo ?: Int.MAX_VALUE }
        val targetStoryId = workInfo.continueCreatingStoryId
        val targetIndex = sortedEpisodes.indexOfFirst {
            !targetStoryId.isNullOrEmpty() && it.storyId == targetStoryId
        }
        if (targetIndex < 0) {
            SeriesActivity.start(workId = workInfo.workId)
            return
        }
        val targetEpisode = sortedEpisodes[targetIndex]
        if (!targetEpisode.isNotCreated()) {
            if (targetEpisode.storyId.isNullOrEmpty()) {
                SeriesActivity.start(workId = workInfo.workId)
                return
            }
            AppRouter.startStory(
                workId = workInfo.workId,
                storyId = targetEpisode.storyId,
                workTitle = workInfo.workTitle,
                workType = workInfo.workType,
                totalEpisodeCnt = workInfo.totalEpisodeCnt,
                workRatio = workInfo.workRatio,
                from = StoryEditorActivity.FROM_MY_WORKS
            )
            return
        }

        val episodeNo = targetEpisode.sortNo ?: targetIndex + 1
        if (workInfo.workType == CreatorWorkInfo.NOVEL_CREATION && episodeNo > 1) {
            ContinueWritingActivity.start(
                context = topActivity,
                workId = workInfo.workId,
                storyId = targetEpisode.storyId,
                workTitle = workInfo.workTitle
            )
            return
        }
        StoryEditorActivity.startContinue(
            context = topActivity,
            workId = workInfo.workId,
            workTitle = workInfo.workTitle,
            storyId = targetEpisode.storyId,
            previousStoryId = sortedEpisodes.getOrNull(targetIndex - 1)?.storyId,
            chapterIndex = episodeNo,
            workType = workInfo.workType,
            model = AppRouter.getModel(
                totalEpisodeCnt = workInfo.totalEpisodeCnt,
                workType = workInfo.workType,
                sortNo = episodeNo,
                firstCreated = targetEpisode.isFirstCreated()
            ),
            videoRatio = targetEpisode.workRatio ?: workInfo.workRatio,
            from = StoryEditorActivity.FROM_MY_WORKS
        )
    }

    fun onPlayEpisodeClicked(workInfo: CreatorWorkInfo?, episodes: List<CreatorEpisodeInfo>, clickPosition: Int) {
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
        val creatorWork = workInfo?.copy(
            userId = workInfo.userId ?: User.get().userId,
            workStoryItemList = validPlayList
        )
        val sourceKey = WorkSeriesCreatorFeedRepository.setPlayableFeed(
            source = CreatorPlaySource.MY_CREATIONS,
            work = creatorWork,
            episodes = validPlayList
        ) ?: return
        CreatorPlayerActivity.start(
            source = CreatorPlaySource.MY_CREATIONS,
            position = newPosition,
            sourceKey = sourceKey
        )
    }
}
