package com.kuaigeglobal.epnova.feature.works

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.PagingData
import com.blankj.utilcode.util.ActivityUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.showConfirmPrompt
import com.kuaigeglobal.epnova.common.ext.startRefreshWithAdapter
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.db.entity.WorkInformationInfo
import com.kuaigeglobal.epnova.core.ext.onUserStatusChanged
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorEpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlaySource
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorWorkInfo
import com.kuaigeglobal.epnova.databinding.FragmentMyWorksBinding
import com.kuaigeglobal.epnova.feature.creator.generate.StoryEditorActivity
import com.kuaigeglobal.epnova.feature.creatorplay.CreatorPlayerActivity
import com.kuaigeglobal.epnova.feature.creatorplay.data.WorkSeriesCreatorFeedRepository
import com.kuaigeglobal.epnova.feature.main.MainActivity
import com.kuaigeglobal.epnova.feature.main.MainViewModel
import com.kuaigeglobal.epnova.feature.series.SeriesActivity
import com.kuaigeglobal.epnova.router.AppRouter
import com.kuaigeglobal.epnova.widget.decoration.BottomSpaceDecoration
import com.kuaigeglobal.epnova.widget.decoration.SpaceDecoration
import com.kuaigeglobal.epnova.widget.stateful.applyGridSpanSizeLookup
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getViewModelOfActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class MyWorksFragment : BaseViewModelFragment<FragmentMyWorksBinding, MyWorksViewModel>() {


    companion object{
        const val TAG = "MyWorksFragment"
        const val CLICK_INTERVAL = 500L // 限制 500ms 内只能触发一次
        @JvmStatic
        fun start() {
            FlowEventBus.postSync(Events.MainTabEvent(Events.MainTabEvent.TAB_MY_CREATIONS))
            ActivityUtils.finishToActivity(MainActivity::class.java, false)
        }
    }

    //上一个的登录id
    private var userId: String? = null

    private val myWorksAdapter by lazy {
        MyWorksAdapter(eventVm = eventVm)
    }
    private val eventVm by lazy {
        ItemMultipleChoiceEventsViewModel<MyWorksEvent, String>()
    }
    private val parentVm by lazy {
        getViewModelOfActivity<MainViewModel>()
    }
    private var lastClickTime = 0L

    //当前点击的作品信息
    var workInfo: CreatorWorkInfo? = null

    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        //数据更新
        dataUpdate()
    }

    override fun onStart() {
        super.onStart()
        mRealVM.isShow.set(true)
        mRealVM.startChecking()
        refresh()
    }

    override fun onStop() {
        super.onStop()
        mRealVM.isShow.set(false)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) return
        if (isShowReception()) {
            mRealVM.isShow.set(true)
            mRealVM.startChecking()
            //更新必要信息
            refresh()
        } else {
            mRealVM.isShow.set(false)
        }
    }

    private fun refresh() {
        myWorksAdapter.refresh()
    }

    private fun initView() {
        binding.recyclerView.apply {
            addItemDecoration(SpaceDecoration(12.dp))
            addItemDecoration(BottomSpaceDecoration(80.dp))
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
        initPagingScrollToTop()
        binding.srl.startRefreshWithAdapter(
            pagingDataAdapter = myWorksAdapter,
            loadStateHolder = binding.lsv,
            onRefreshListener = {
                mRealVM.startChecking()
            }
        )
    }

    /**
     * 核心修复逻辑：严格监听 Paging 的 refresh 状态，忽略 append (加载更多) 状态。
     * 确保只有在整体数据刷新完成时，才重置列表位置到顶部。
     */
    private fun initPagingScrollToTop() {
        lifecycleScope.launch {
            myWorksAdapter.loadStateFlow
                // distinctUntilChangedBy 非常关键：它保证只有在 refresh 状态发生变化时才触发
                // 这样当你滑动到底部触发 append 时，这里的逻辑会被完全忽略，不会导致置顶
                .distinctUntilChangedBy { it.refresh }
                .filter { it.refresh is LoadState.NotLoading }
                .collect {
                    // 当 refresh 状态变为 NotLoading，说明全局刷新或初次加载完成了，强制回到顶部
                    binding.recyclerView.smoothScrollToPosition(0)
                }
        }
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
                            if (event.workInfo.workStatus == CreatorWorkInfo.COMPLETED_STATE) {
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

        getViewModelOfActivity<MainViewModel>()?.setIsEditing = { status ->
            mRealVM.isView.value = true
        }

        mRealVM.isView.observe(this) {
            eventVm.switchStatus(it)
            getViewModelOfActivity<MainViewModel>()?.isEditing?.set(!it)
            if (it) {
                //是否启用下拉刷新
                binding.srl.setEnableRefresh(true)
                binding.groupView.visibility = View.VISIBLE
                binding.groudEdit.visibility = View.GONE
                eventVm.clearAllSelection()
                getViewModelOfActivity<MainViewModel>()?.showBottomBar?.set(true)
                return@observe
            }
            binding.srl.setEnableRefresh(false)
            getViewModelOfActivity<MainViewModel>()?.showBottomBar?.set(false)
            binding.groupView.visibility = View.GONE
            binding.groudEdit.visibility = View.VISIBLE
        }

        mRealVM.selectedCount.observe(this) {count ->
            if (count <= 0) {
                binding.tvSelectCount.text = getString(R.string.selected_items_format, "0")
                return@observe
            }
            val countText = count.toString()
            val selectedCountText = getString(R.string.selected_items_format, countText)
            val spannableString = SpannableString(selectedCountText)
            val countStart = selectedCountText.indexOf(countText)
            if (countStart >= 0) {
                val countEnd = countStart + countText.length
                spannableString.setSpan(
                    ForegroundColorSpan(Color.WHITE),
                    countStart,
                    countEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            binding.tvSelectCount.text = spannableString
        }

        mRealVM.episodeList.observe(this) {
            if (it.isNullOrEmpty()) return@observe
            mRealVM.episodeList.value = null
            onPlayEpisodeClicked(workInfo = workInfo, episodes = it, clickPosition = 0)
        }
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
        val creatorWork = workInfo?.copy(workStoryItemList = validPlayList)
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


    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.btnEdit -> {
                //编辑
                mRealVM.isView.value = false
            }

            R.id.btnSelectAll -> {
                //全选
                val ids = hashSetOf<String>()
                val items: List<WorkInformationInfo> = myWorksAdapter.snapshot().items
                items.forEach { w -> ids.add(w.work.workId ?: "") }
                eventVm.setSelectedItems(ids)
                mRealVM.selectedCount.value = ids.size
            }

            R.id.btnDelete -> {
                //删除
                if (eventVm.getSelectedItems().isEmpty()) {
                    mRealVM.isView.value = true
                    return
                }
                topActivity?.showConfirmPrompt(
                    title = getString(R.string.confirm_delete_work),
                    cancelText = getString(R.string.cancel),
                    confirmText = getString(R.string.confirm_ok)
                ) {
                    mRealVM.delete(eventVm.getSelectedItems().toList())
                }
            }

            R.id.btnComplete -> {
                //完成
                mRealVM.isView.value = true
            }
        }
    }
}
