package com.kuaigeglobal.epnova.feature.series

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.showCommonDialog
import com.kuaigeglobal.epnova.common.ext.showConfirmPrompt
import com.kuaigeglobal.epnova.common.ext.startRefreshWithAdapter
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.common.permissions.requestRead2WritePermissions
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorEpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlaySource
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorWorkInfo
import com.kuaigeglobal.epnova.core.network.apis.request.RatingEnterEnum
import com.kuaigeglobal.epnova.core.network.apis.response.WorkPublishStatusData
import com.kuaigeglobal.epnova.databinding.ActivitySeriesBinding
import com.kuaigeglobal.epnova.databinding.PopupSeriesReleaseFailTipsBinding
import com.kuaigeglobal.epnova.feature.creator.continuewriting.ContinueWritingActivity
import com.kuaigeglobal.epnova.feature.creator.data.DramaGenerateConstants
import com.kuaigeglobal.epnova.feature.creator.generate.StoryEditorActivity
import com.kuaigeglobal.epnova.feature.creatorplay.CreatorPlayerActivity
import com.kuaigeglobal.epnova.feature.creatorplay.data.WorkSeriesCreatorFeedRepository
import com.kuaigeglobal.epnova.feature.creatorplay.mapper.LegacyCreatorBridgeMapper
import com.kuaigeglobal.epnova.feature.download.DownloadingDialog
import com.kuaigeglobal.epnova.feature.download.DownloadingViewModel
import com.kuaigeglobal.epnova.feature.editor.info.DramaInfoEditActivity
import com.kuaigeglobal.epnova.feature.play.more.DownloadMethodDialog
import com.kuaigeglobal.epnova.feature.rating.AppRatingDialog
import com.kuaigeglobal.epnova.feature.series.management.WorksManagementActivity
import com.kuaigeglobal.epnova.feature.web.SimpleBrowserActivity
import com.kuaigeglobal.epnova.feature.works.ItemMultipleChoiceEventsViewModel
import com.kuaigeglobal.epnova.router.AppRouter
import com.kuaigeglobal.epnova.widget.Popup
import com.kuaigeglobal.epnova.widget.SeriesMorePopupView
import com.kuaigeglobal.epnova.widget.decoration.BottomSpaceDecoration
import com.kuaigeglobal.epnova.widget.stateful.applyGridSpanSizeLookup
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.isActive
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 剧集详情页
 */
class SeriesActivity :
    BaseViewModelActivity<ActivitySeriesBinding, SeriesViewModel>(),
    DownloadMethodDialog.DownloadTypeLauncher,
    DownloadingViewModel.DownloadStatusHolderFactory {
    companion object {
        fun start(workId: String?) {
            val activity = topActivity ?: return
            activity.startActivity(
                Intent(activity, SeriesActivity::class.java).apply {
                    putExtra(Constants.Extra.ID, workId)
                }
            )
        }
    }

    private val eventVm by lazy { ItemMultipleChoiceEventsViewModel<SeriesEpisodeEvent, String>() }
    private val episodeAdapter by lazy { EpisodeAdapter(eventVm, Config.get().isGridMode) }
    private var downloadVideoUrl: String? = null
    private var downloadingDialog: DownloadingDialog? = null
    private var isShowPublishRejectReason = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // work_detail_show：作品详情页面展示时上报。
        EventManager.onFirebaseTrack(
            FirebaseEvent.Category.PUBLISH,
            FirebaseEvent.Action.WORK_DETAIL_SHOW
        )
        binding.toolbarLayout.minimumHeight = (32 + 62 + 12).dp
        initView()
        //数据更新
        observeData()
    }

    override fun onPostResume() {
        super.onPostResume()
        AppRatingDialog.show(supportFragmentManager, RatingEnterEnum.SERIES)
    }

    private fun initView() {
        updateSwitchTypeIcon()
        val noDataButtonTextAs = getString(R.string.refresh)
        val noDataAs = getString(R.string.empty_creation_history)
        binding.recyclerView.apply {
            itemAnimator = null
            addItemDecoration(BottomSpaceDecoration(80.dp))
            adapter = episodeAdapter.withLoadStateFooter(
                MyMultiStatePagingFooterAdapter(
                    hostAdapter = episodeAdapter,
                    noData = noDataAs,
                    noDataButtonDisplay = false,
                    requestButtonDisplay = false,
                    noDataButtonText = noDataButtonTextAs,
                    displayedHeight = 400.dp,
                    refresh = { refresh() },
                    requestButtonTextAndImg = false,
                )
            )
        }
        applyEpisodeLayout()
        binding.loadStateViewHolder.apply {
            setGravityHorizontalTop(100.dp)
            setRetryListener { refresh() }
            setNoDataButton(noDataButtonTextAs, { refresh() })
            setNoDataPrompt(noDataAs)
        }
        binding.swipeRefreshLayout.startRefreshWithAdapter(pagingDataAdapter = episodeAdapter)
        episodeAdapter.addLoadStateListener { loadStates ->
            mRealVM.onEpisodeListLoadStateChanged(loadStates.refresh, episodeAdapter.snapshot().items)
        }
    }

    /** 根据当前剧集展示模式显示下一步可切换的图标。 */
    private fun updateSwitchTypeIcon() {
        binding.ivSwitchType.setImageResource(
            if (Config.get().isGridMode) {
                R.drawable.ic_type_list
            } else {
                R.drawable.ic_type_grid
            }
        )
    }

    private fun applyEpisodeLayout(preserveScrollPosition: Boolean = false) {
        val recyclerView = binding.recyclerView
        val oldLayoutManager = recyclerView.layoutManager as? LinearLayoutManager
        val firstVisiblePosition = if (preserveScrollPosition) {
            oldLayoutManager?.findFirstVisibleItemPosition() ?: RecyclerView.NO_POSITION
        } else {
            RecyclerView.NO_POSITION
        }
        val firstVisibleView = if (firstVisiblePosition != RecyclerView.NO_POSITION) {
            oldLayoutManager?.findViewByPosition(firstVisiblePosition)
        } else {
            null
        }
        val firstVisibleOffset = firstVisibleView?.top ?: 0
        val gridMode = Config.get().isGridMode
        val layoutManager = if (gridMode) {
            GridLayoutManager(this, 3)
        } else {
            EventManager.onFirebaseTrack(
                FirebaseEvent.Category.EPISODE_LIST,
                FirebaseEvent.Action.EPISODE_LIST_SHOW,
                mRealVM.workId
            )
            LinearLayoutManager(this)
        }

        recyclerView.layoutManager = layoutManager
        episodeAdapter.setGridMode(gridMode)
        if (gridMode) {
            recyclerView.applyGridSpanSizeLookup()
        }

        if (firstVisiblePosition != RecyclerView.NO_POSITION) {
            recyclerView.post {
                layoutManager.scrollToPositionWithOffset(firstVisiblePosition, firstVisibleOffset)
            }
        }
    }

    private fun refresh() {
        //作品详情
        mRealVM.getWorkDtl()
    }

    override fun fragmentManager(): FragmentManager? = supportFragmentManager

    override fun getStatusHolder(): DownloadingViewModel.DownloadStatusHolder? = mRealVM

    override fun downloadVideo(watermark: Boolean) {
        requestRead2WritePermissions { granted ->
            if (granted) {
                mRealVM.downloadVideo(downloadVideoUrl, watermark)
            }
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            eventVm.events.collect { event ->
                when (event) {
                    is SeriesEpisodeEvent.Click -> {
                        EventManager.onFirebaseTrack(
                            FirebaseEvent.Category.EPISODE_LIST,
                            FirebaseEvent.Action.CREATE_CLICK,
                            event.item.data.get()?.workId,
                            event.item.data.get()?.storyId
                        )
                        //点击查看
                        clickToView(event.item.data.get(), event.position)
                    }

                    is SeriesEpisodeEvent.Download -> {
                        val episode = event.item.data.get() ?: return@collect
                        val videoUrl = episode.videoUrl?.takeIf { it.isNotEmpty() }
                            ?: return@collect
                        downloadVideoUrl = videoUrl

                        EventManager.onFirebaseTrack(
                            FirebaseEvent.Category.EPISODE_LIST,
                            FirebaseEvent.Action.DOWNLOAD_CLICK,
                            episode.workId,
                            episode.storyId
                        )

                        if (episode.hasWatermark == true) {
                            downloadVideo(false)
                        } else {
                            DownloadMethodDialog.show(this@SeriesActivity)
                        }
                    }

                    else -> {}
                }
            }
        }

        mRealVM.downloading.observe(this) { downloading ->
            if (downloading) {
                if (downloadingDialog == null) {
                    downloadingDialog = DownloadingDialog.show(this)
                }
            } else {
                downloadingDialog?.dismissAllowingStateLoss()
                downloadingDialog = null
            }
        }

        lifecycleScope.launch {
            mRealVM.episodeList().collectLatest {
                episodeAdapter.submitData(it)
            }
        }

        observeEvents {
            on<Events.SeriesUpdatedEvent> { event ->
                if (isActive && event.seriesId == mRealVM.workId) {
                    refresh()
                }
            }
            on<Events.ShortDramasEditInfoEvent> { event ->
                if (episodeAdapter.snapshot().any { it?.storyId == event.dramaId }) {
                    refresh()
                }
            }
            on<Events.DeleteTVSeriesEvent> { event ->
                if (isActive && event.workId == mRealVM.workId) {
                    refresh()
                }
            }
            on<Events.WorkPublishChangedEvent> { event ->
                if (isActive && event.workId == mRealVM.workId) {
                    refresh()
                }
            }
        }
        mRealVM.workInfo.observe(this) {
            episodeAdapter.refresh()
            if (it?.isPublishReviewFailed() == true && !isShowPublishRejectReason) bindingNullable?.tvPublishStatus?.showPublishRejectReason()
        }
        mRealVM.episodeInfo.observe(this) {
            if (it == null) {
                return@observe
            }
            val workInfo = mRealVM.workInfo.value ?: return@observe
            mRealVM.episodeInfo.value = null
            Logger.d("[SSE]", "上一集的剧集id = ${it.storyId}")
            Logger.d("[SSE]", "当前集的剧集id = ${it.sequelStoryId}")
            //灵感创作续写
            StoryEditorActivity.startContinue(
                this,
                workId = mRealVM.workId,
                workTitle = workInfo.workTitle,
                storyId = it.sequelStoryId,
                previousStoryId = it.storyId,
                chapterIndex = (it.sortNo ?: 0) + 1,
                workType = workInfo.workType,
                model = getModel(workInfo.workType),
                videoRatio = it.workRatio,
                from = StoryEditorActivity.FROM_SERIES
            )
        }
    }

    fun getModel(workType: Int?): Int? {
        return when (workType) {
            CreatorWorkInfo.INSPIRATION_CREATION -> DramaGenerateConstants.MODEL_CONTINUE_INSPIRATION
            CreatorWorkInfo.NOVEL_CREATION -> DramaGenerateConstants.MODEL_CONTINUE_STORY
            CreatorWorkInfo.NOVEL_UPLOAD -> DramaGenerateConstants.MODE_CONTINUE_WRITING_NOVEL_DOCUMENTS
            else -> null
        }
    }

    /**
     * 点击查看
     */
    private fun clickToView(episodeInfo: CreatorEpisodeInfo?, position: Int) {
        if (episodeInfo == null) return
        //[0未创建 10=剧本输入 15=剧本生成中 20=资产生成中 30=分镜生成中 32=分镜视频生成中 35=待合成 40=已完成]
        when (episodeInfo.stepProgress) {
            CreatorEpisodeInfo.PROGRESS_COMPLETED -> {
                //已经完成了，走播放
                val snapshot = episodeAdapter.snapshot().items
                onPlayEpisodeClicked(
                    mRealVM.workInfo.value,
                    snapshot,
                    position
                )
            }

            CreatorEpisodeInfo.NOT_CREATED, CreatorEpisodeInfo.PROGRESS_SUMMARY_GENERATING -> {
                // pending_creation_click：点击下一集的 Pending Creation 时上报。
                EventManager.onFirebaseTrack(
                    FirebaseEvent.Category.PUBLISH,
                    FirebaseEvent.Action.PENDING_CREATION_CLICK
                )
                val snapshot = episodeAdapter.snapshot().items
                // 1. 寻找前置阻断点
                val blocker = findEligibleEpisode(snapshot, position)

                if (blocker != null) {
                    // 2. 存在阻断，提示用户
                    getString(R.string.please_complete_episode_content, blocker.sortNo).showToast()
                } else {
                    // 3. 校验通过，允许创建
                    // 传入上一集作为上下文（如果是第一集则为 null）
                    val prevEp = snapshot.getOrNull(position - 1)
                    continueWriting(prevEp, episodeInfo)
                }
            }

            else -> {
                val workInfo = mRealVM.workInfo.value
                AppRouter.startStory(
                    workId = workInfo?.workId,
                    storyId = episodeInfo.storyId,
                    workTitle = workInfo?.workTitle,
                    workType = workInfo?.workType,
                    totalEpisodeCnt = workInfo?.totalEpisodeCnt,
                    workRatio = workInfo?.workRatio,
                    from = StoryEditorActivity.FROM_SERIES
                )
            }
        }
    }

    /**
     * 寻找断档后的第一个故障点
     */
    private fun findFirstBlocker(items: List<CreatorEpisodeInfo>, position: Int): CreatorEpisodeInfo? {
        if (position <= 0) return null

        // 在当前位置之前，找到最后一个“达标”的集数索引
        // 达标：已创建 且 进度 >= 32 (PROGRESS_STORYBOARD_GENERATING)
        val lastValidIndex = items.take(position).indexOfLast {
            it.stepProgress != CreatorEpisodeInfo.NOT_CREATED &&
                    (it.stepProgress ?: 0) >= CreatorEpisodeInfo.PROGRESS_STORYBOARD_GENERATING
        }

        // 计算拦截点索引：
        // 如果前面没有一个达标的，则卡在第 1 集 (index 0)
        // 如果有达标的，则卡在达标集数的下一集 (lastValidIndex + 1)
        val blockerIndex = if (lastValidIndex == -1) 0 else lastValidIndex + 1

        // 如果拦截点在当前点击位置之前，说明需要拦截
        return if (blockerIndex < position) {
            items.getOrNull(blockerIndex)
        } else {
            null
        }
    }

    private fun findEligibleEpisode(items: List<CreatorEpisodeInfo>, position: Int): CreatorEpisodeInfo? {
        if (position < 0) return null
        // 找点击之前的item是否存在有未创建 or 未进分镜预览阶段的剧集
        val eligibleIndex = items.take(position).indexOfFirst {
            it.stepProgress == CreatorEpisodeInfo.NOT_CREATED || (it.stepProgress ?: 0) < CreatorEpisodeInfo.PROGRESS_STORYBOARD_GENERATING
        }
        // 存在则返回剧集下标 不存在则返回点击的下标
        val blockerIndex = if (eligibleIndex == -1) position else eligibleIndex

        // 小于点击下标则返回对应剧集信息
        return if (blockerIndex < position) {
            items.getOrNull(blockerIndex)
        } else {
            null
        }
    }

    fun onPlayEpisodeClicked(workInfo: CreatorWorkInfo?, episodes: List<CreatorEpisodeInfo>, clickPosition: Int) {
        // 1. [核心步骤] 过滤未完成的数据，保持 Creator 作品详情和播放器使用同一套模型
        val validPlayList = episodes
            .filter { !it.videoUrl.isNullOrEmpty() } // 过滤条件
            .map { episode ->
                episode.copy(workId = episode.workId ?: workInfo?.workId)
            }

        // 2. 计算修正后的点击位置
        // 因为过滤了数据，原来的 position 可能变了，需要找到点击的那个 episode 在新列表里的位置
        val clickedEpisodeId = episodes.getOrNull(clickPosition)?.storyId
        val newPosition =
            validPlayList.indexOfFirst { it.storyId == clickedEpisodeId }.coerceAtLeast(0)
        val creatorWork = workInfo?.copy(workStoryItemList = validPlayList)
        val sourceKey = WorkSeriesCreatorFeedRepository.setPlayableFeed(
            source = CreatorPlaySource.WORK_SERIES,
            work = creatorWork,
            episodes = validPlayList
        ) ?: return
        CreatorPlayerActivity.start(
            source = CreatorPlaySource.WORK_SERIES,
            position = newPosition,
            sourceKey = sourceKey
        )
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.ivSwitchType -> {
                Config.get().switchMode()
                applyEpisodeLayout(preserveScrollPosition = true)
                updateSwitchTypeIcon()
                EventManager.onFirebaseTrack(
                    FirebaseEvent.Category.EPISODE_LIST,
                    FirebaseEvent.Action.VIEW_SWITCH_CLICK,
                    if (Config.get().isGridMode) FirebaseEvent.Value.SWITCH_GRID else FirebaseEvent.Value.SWITCH_LIST
                )
            }

            R.id.btnMore -> {
                mRealVM.workInfo.value ?: return
                val popupView = SeriesMorePopupView(this)
                val popup = Popup(popupView)
                popupView.binding.btnEdit.setOnSingleClickListener {
                    popup.dismiss()
                    //编辑剧本信息
                    DramaInfoEditActivity.start(
                        this,
                        LegacyCreatorBridgeMapper.toLegacyWorkInfo(mRealVM.workInfo.value)
                    )
                }
                //没有剧集
                if (!episodeAdapter.snapshot().items.any { (it.stepProgress ?: 0) > CreatorEpisodeInfo.NOT_CREATED }) {
                    popupView.binding.btnManage.isEnabled = false
                    popupView.binding.btnManage.alpha = 0.5f
                }
                popupView.binding.btnManage.setOnSingleClickListener {
                    popup.dismiss()
                    WorksManagementActivity.start(this, workId = mRealVM.workId)
                }
                val deleteLocked = mRealVM.workInfo.value?.hasPublishedOrUnderReviewEpisode() == true
                popupView.binding.btnDel.alpha = if (deleteLocked) 0.5f else 1f
                popupView.binding.btnDel.setOnSingleClickListener {
                    popup.dismiss()
                    if (deleteLocked) {
                        showToast(R.string.drama_delete_failed)
                        return@setOnSingleClickListener
                    }
                    showCommonDialog(title = getString(R.string.drama_delete_work_confirm)) {
                        mRealVM.delete()
                    }
                }
                popup.showUnder(view)
            }

            R.id.llPublishStatus -> {
                bindingNullable?.tvPublishStatus?.showPublishRejectReason()
            }

            R.id.btnSelfPublish -> {
                mRealVM.workInfo.value?.workSelfPublishProcessUrl?.let {
                    SimpleBrowserActivity.launch(this, it)
                }
            }

            R.id.btnPublish -> {
                // work_detail_publish_click：点击作品详情页底部发布按钮时上报。
                EventManager.onFirebaseTrack(
                    FirebaseEvent.Category.PUBLISH,
                    FirebaseEvent.Action.WORK_DETAIL_PUBLISH_CLICK
                )
                mRealVM.queryWorkPublishStatus(
                    openPublishPage = { openPublishPage(it) },
                    confirmNativePublish = { showPublishConfirm(it) },
                    confirmRejectionPublish = { showRejectionPublishConfirm(it) }
                )
            }
        }
    }

    private fun openPublishPage(url: String?) {
        if (url.isNullOrBlank()) {
            showToast(R.string.work_publish_page_unavailable)
            return
        }
        SimpleBrowserActivity.launch(this, url)
    }

    private fun showPublishConfirm(status: WorkPublishStatusData) {
        showConfirmPrompt(
            title = getString(
                R.string.tip_publish_ep_number_confirm,
                status.startEpisode?.toString().orEmpty(),
                status.endEpisode?.toString().orEmpty()
            ),
            tip = getString(R.string.tip_publish_submit_review_notice),
            cancelText = getString(R.string.cancel),
            confirmText = getString(R.string.drama_detail_publish),
            confirm = { _ ->
                mRealVM.submitWorkPublish(status)
            }
        )
    }

    private fun showRejectionPublishConfirm(status: WorkPublishStatusData) {
        showConfirmPrompt(
            title = getString(
                R.string.tip_publish_ep_number_confirm,
                status.startEpisode?.toString().orEmpty(),
                status.endEpisode?.toString().orEmpty()
            ),
            tip = getString(R.string.tip_publish_has_rejected_ep),
            cancelText = getString(R.string.cancel),
            confirmText = getString(R.string.drama_detail_publish),
            confirm = { _ ->
                mRealVM.submitWorkPublish(status)
            }
        )
    }

    private fun View.showPublishRejectReason() {
        val rejectReason = mRealVM.workInfo.value?.rejectReason?.takeIf { it.isNotBlank() } ?: return
        // 1. 填充布局
        val binding = PopupSeriesReleaseFailTipsBinding.inflate(LayoutInflater.from(this@SeriesActivity), null, false)
        binding.tvTips.text = rejectReason

        // 2. 创建并配置 PopupWindow
        val popupWindow = PopupWindow(
            binding.root,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true // 允许获取焦点，点击外部消失
        ).apply {
            // 设置背景透明（为了让气泡阴影或圆角正常显示）
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            isOutsideTouchable = true
        }

        // 3. 在当前 View（点击的控件）下方显示
        // xOff: 水平偏移，yOff: 垂直偏移（单位像素）
        popupWindow.showAsDropDown(this, 0, 10)
        isShowPublishRejectReason = true
    }

    private fun continueWriting(lastInfo: CreatorEpisodeInfo?, info: CreatorEpisodeInfo?) {
        val workInfo = mRealVM.workInfo.value ?: return
        if ((info?.sortNo ?: 0) > 1 && workInfo.workType == CreatorWorkInfo.NOVEL_CREATION) {
            //小说改编续写
            ContinueWritingActivity.start(
                this,
                workId = mRealVM.workId,
                storyId = info?.storyId,
                workTitle = workInfo.workTitle
            )
            return
        }
        StoryEditorActivity.startContinue(
            this,
            workId = mRealVM.workId,
            workTitle = workInfo.workTitle,
            storyId = info?.storyId,
            previousStoryId = lastInfo?.storyId,
            chapterIndex = info?.sortNo ?: 1,
            workType = workInfo.workType,
            model = getModel(workInfo.workType),
            videoRatio = workInfo.workRatio,
            from = StoryEditorActivity.FROM_SERIES
        )
        //需要拿到上一集的剧集信息，所以先调用一下
//        mRealVM.getEpisodeInfo(workInfo.workType)
    }
}
