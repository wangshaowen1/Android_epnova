package com.kuaigeglobal.epnova.feature.series

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.showCommonDialog
import com.kuaigeglobal.epnova.common.ext.showConfirmPrompt
import com.kuaigeglobal.epnova.common.ext.startRefreshWithAdapter
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.manager.AppEvent
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo
import com.kuaigeglobal.epnova.core.network.apis.response.WorkPublishStatusData
import com.kuaigeglobal.epnova.databinding.ActivitySeriesBinding
import com.kuaigeglobal.epnova.databinding.PopupSeriesReleaseFailTipsBinding
import com.kuaigeglobal.epnova.feature.creator.continuewriting.ContinueWritingActivity
import com.kuaigeglobal.epnova.feature.creator.data.DramaGenerateConstants
import com.kuaigeglobal.epnova.feature.creator.generate.StoryEditorActivity
import com.kuaigeglobal.epnova.feature.editor.info.DramaInfoEditActivity
import com.kuaigeglobal.epnova.feature.play.DramasPlayPagerActivity
import com.kuaigeglobal.epnova.feature.play.data.FeedRepository
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
import kotlinx.coroutines.launch

/**
 * 剧集详情页
 */
class SeriesActivity : BaseViewModelActivity<ActivitySeriesBinding, SeriesViewModel>() {
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
    private val episodeAdapter by lazy { EpisodeAdapter(eventVm) }
    private var isShowPublishRejectReason = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.toolbarLayout.minimumHeight = 32.dp
        initView()
        //数据更新
        observeData()
    }

    private fun initView() {
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
            binding.recyclerView.applyGridSpanSizeLookup()
        }
        binding.loadStateViewHolder.apply {
            setGravityHorizontalTop(100.dp)
            setRetryListener { refresh() }
            setNoDataButton(noDataButtonTextAs, { refresh() })
            setNoDataPrompt(noDataAs)
        }
        binding.swipeRefreshLayout.startRefreshWithAdapter(pagingDataAdapter = episodeAdapter)
    }

    private fun refresh() {
        //作品详情
        mRealVM.getWorkDtl()
    }

    private fun observeData() {
        lifecycleScope.launch {
            eventVm.events.collect { event ->
                when (event) {
                    is SeriesEpisodeEvent.Click -> {
                        //点击查看
                        clickToView(event.item.data.get(), event.position)
                    }

                    else -> {}
                }
            }
        }

        lifecycleScope.launch {
            mRealVM.episodeList().collect {
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
            WorkInfo.INSPIRATION_CREATION -> DramaGenerateConstants.MODEL_CONTINUE_INSPIRATION
            WorkInfo.NOVEL_CREATION -> DramaGenerateConstants.MODEL_CONTINUE_STORY
            WorkInfo.NOVEL_UPLOAD -> DramaGenerateConstants.MODE_CONTINUE_WRITING_NOVEL_DOCUMENTS
            else -> null
        }
    }

    /**
     * 点击查看
     */
    private fun clickToView(episodeInfo: EpisodeInfo?, position: Int) {
        if (episodeInfo == null) return
        //[0未创建 10=剧本输入 15=剧本生成中 20=资产生成中 30=分镜生成中 32=分镜视频生成中 35=待合成 40=已完成]
        when (episodeInfo.stepProgress) {
            EpisodeInfo.PROGRESS_COMPLETED -> {
                //已经完成了，走播放
                val snapshot = episodeAdapter.snapshot().items
                onPlayEpisodeClicked(
                    mRealVM.workInfo.value,
                    snapshot,
                    position
                )
            }

            EpisodeInfo.NOT_CREATED, EpisodeInfo.PROGRESS_SUMMARY_GENERATING -> {
                val snapshot = episodeAdapter.snapshot().items
                // 1. 寻找前置阻断点
                val blocker = findFirstBlocker(snapshot, position)

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
                AppRouter.startStory(mRealVM.workInfo.value, episodeInfo.storyId, from = StoryEditorActivity.FROM_SERIES)
            }
        }
    }

    /**
     * 寻找断档后的第一个故障点
     */
    private fun findFirstBlocker(items: List<EpisodeInfo>, position: Int): EpisodeInfo? {
        if (position <= 0) return null

        // 在当前位置之前，找到最后一个“达标”的集数索引
        // 达标：已创建 且 进度 >= 32 (PROGRESS_STORYBOARD_GENERATING)
        val lastValidIndex = items.take(position).indexOfLast {
            it.stepProgress != EpisodeInfo.NOT_CREATED &&
                    (it.stepProgress ?: 0) >= EpisodeInfo.PROGRESS_STORYBOARD_GENERATING
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

    fun onPlayEpisodeClicked(workInfo: WorkInfo?, episodes: List<EpisodeInfo>, clickPosition: Int) {
        // 1. [核心步骤] 过滤未完成的数据 + 转换为 Adapter 需要的 WorkInfo
        val validPlayList = episodes
            .filter { !it.videoUrl.isNullOrEmpty() } // 过滤条件
            .map { episode ->
                episode.workId = workInfo?.workId
                episode
            }

        // 2. 计算修正后的点击位置
        // 因为过滤了数据，原来的 position 可能变了，需要找到点击的那个 episode 在新列表里的位置
        val clickedEpisodeId = episodes[clickPosition].storyId
        val newPosition =
            validPlayList.indexOfFirst { it.storyId == clickedEpisodeId }.coerceAtLeast(0)
        // 3. 存入 Repository
        FeedRepository.setPreloadedSeries(workInfo?.workId, validPlayList)
        // 4. 跳转播放页
        DramasPlayPagerActivity.start(
            workId = workInfo?.workId,
            // 传入修正后的位置
            position = newPosition
        )
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.btnMore -> {
                mRealVM.workInfo.value ?: return
                val popupView = SeriesMorePopupView(this)
                val popup = Popup(popupView)
                popupView.binding.btnEdit.setOnSingleClickListener {
                    popup.dismiss()
                    //编辑剧本信息
                    DramaInfoEditActivity.start(this, mRealVM.workInfo.value)
                }
                //没有剧集
                if (!episodeAdapter.snapshot().items.any { (it.stepProgress ?: 0) > EpisodeInfo.NOT_CREATED }) {
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
                        showToast(R.string.delete_work_published_or_review_toast)
                        return@setOnSingleClickListener
                    }
                    showCommonDialog(title = getString(R.string.delete_work_warning)) {
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
                mRealVM.queryWorkPublishStatus(
                    openPublishPage = { openPublishPage(it) },
                    confirmNativePublish = { showPublishConfirm(it) }
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
            title = getString(R.string.work_publish_episodes_title, status.episodeRangeText()),
            tip = getString(R.string.work_publish_confirm_tip),
            cancelText = getString(R.string.cancel),
            confirmText = getString(R.string.work_publish_action),
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

    private fun continueWriting(lastInfo: EpisodeInfo?, info: EpisodeInfo?) {
        val workInfo = mRealVM.workInfo.value ?: return
        if ((info?.sortNo ?: 0) > 1 && workInfo.workType == WorkInfo.NOVEL_CREATION) {
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
