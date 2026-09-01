package com.kuaigeglobal.epnova.feature.creatorplay

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.showCommonDialog
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.common.permissions.requestRead2WritePermissions
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorEpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlaySource
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlayableEpisode
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorWorkInfo
import com.kuaigeglobal.epnova.core.network.apis.response.PlayMenuInfo
import com.kuaigeglobal.epnova.databinding.ActivityCreatorPlayerBinding
import com.kuaigeglobal.epnova.feature.creator.continuewriting.ContinueWritingActivity
import com.kuaigeglobal.epnova.feature.creator.generate.StoryEditorActivity
import com.kuaigeglobal.epnova.feature.download.DownloadingDialog
import com.kuaigeglobal.epnova.feature.download.DownloadingViewModel
import com.kuaigeglobal.epnova.feature.editor.CreatorStoryboardEditorLauncher
import com.kuaigeglobal.epnova.feature.play.menu.ListPopupWindow
import com.kuaigeglobal.epnova.feature.play.menu.OnItemSelectedListener
import com.kuaigeglobal.epnova.feature.play.more.DownloadMethodDialog
import com.kuaigeglobal.epnova.feature.settings.report.ReportDialog
import com.kuaigeglobal.epnova.router.AppNavigator
import com.kuaigeglobal.epnova.router.AppRouter
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.setOnSingleClickListener
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 新创作播放器 Activity。
 *
 * 阶段四只搭独立播放器骨架；TopEarners/MyCreations/作品详情入口在后续阶段接入。
 */
class CreatorPlayerActivity :
    BaseViewModelActivity<ActivityCreatorPlayerBinding, CreatorPlayerViewModel>(),
    CreatorVideoFeedAdapter.InteractionListener,
    DownloadingViewModel.DownloadStatusHolderFactory,
    DownloadMethodDialog.DownloadTypeLauncher {

    companion object {
        /**播放来源 extra，使用 CreatorPlaySource.name 存储，避免 Creator 枚举跨模块序列化差异。*/
        private const val EXTRA_SOURCE = "creator_player_source"

        /**初始播放位置 extra。*/
        private const val EXTRA_POSITION = "creator_player_position"

        /**当前作品内播放 source 的作品 ID。*/
        private const val EXTRA_SOURCE_KEY = "creator_player_source_key"

        /**启动新创作播放器；阶段五会从 TopEarners item 点击入口调用。*/
        fun start(source: CreatorPlaySource, position: Int = 0, sourceKey: String? = null) {
            val context = topActivity ?: return
            context.startActivity(
                Intent(context, CreatorPlayerActivity::class.java).apply {
                    putExtra(EXTRA_SOURCE, source.name)
                    putExtra(EXTRA_POSITION, position)
                    putExtra(EXTRA_SOURCE_KEY, sourceKey)
                }
            )
        }
    }

    /**播放器 feed adapter，只绑定 CreatorPlayableEpisode。*/
    private val adapter = CreatorVideoFeedAdapter(this)

    /**Creator 独立播放器 manager，不复用旧内容播放器 FeedPlayerManager。*/
    private lateinit var playerManager: CreatorFeedPlayerManager

    /**Creator 独立列表控制器，只处理上下滑和自动下一条。*/
    private lateinit var listController: CreatorFeedListController

    /**等待 feed 首屏渲染后的初始定位；用完置为 -1，避免分页刷新时重复跳转。*/
    private var pendingInitialPosition = 0

    /**右上角更多菜单，按旧播放器/FastFilm 复用同一套 popup 组件。*/
    private var listPopupWindow: ListPopupWindow<PlayMenuInfo>? = null

    /**下载进度弹窗，下载任务由 CreatorPlayerViewModel 持有。*/
    private var downloadingDialog: DownloadingDialog? = null

    /**下载方式弹窗回调时使用的当前视频 URL。*/
    private var downloadVideoUrl: String? = null

    /** GA4 播放器展示仅在本次页面首次真正开始播放时上报。 */
    private var hasReportedPlayerShow = false

    /** GA4 播放节点按剧集去重，切集时重置。 */
    private var analyticsEpisodeKey: String? = null
    private var hasReportedValidPlay = false
    private val reportedProcessNodes = mutableSetOf<Int>()
    private var hasReportedComplete = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRealVM.playSource = readPlaySource()
        mRealVM.initialPosition = intent.getIntExtra(EXTRA_POSITION, 0)
        mRealVM.sourceKey = intent.getStringExtra(EXTRA_SOURCE_KEY)
        pendingInitialPosition = mRealVM.initialPosition
        if (mRealVM.playSource  == CreatorPlaySource.MY_CREATIONS || mRealVM.playSource  == CreatorPlaySource.WORK_SERIES) {
            EventManager.onFirebaseTrack(
                FirebaseEvent.Category.WORK_PLAY,
                FirebaseEvent.Action.WORK_PLAY_SHOW
            )
        }

        initPlayerView()
        initActionButtons()
        observeFeed()
        observePlayer()
        observeDownload()
    }

    override fun onResume() {
        super.onResume()
        playerManager.resumeOnForeground()
    }

    override fun onPause() {
        playerManager.pause()
        super.onPause()
    }

    override fun onDestroy() {
        playerManager.release()
        super.onDestroy()
    }

    override fun onTogglePlay(position: Int) {
        playerManager.togglePlay()
    }

    override fun onSeek(position: Int, progress: Long) {
        playerManager.seekTo(progress)
    }

    override fun getStatusHolder(): DownloadingViewModel.DownloadStatusHolder? {
        return mRealVM
    }

    override fun fragmentManager(): FragmentManager? = supportFragmentManager

    override fun downloadVideo(watermark: Boolean) {
        requestRead2WritePermissions { granted ->
            if (granted) {
                mRealVM.downloadVideo(downloadVideoUrl, watermark)
            }
        }
    }

    /**初始化播放器 UI、adapter 和列表控制器。*/
    private fun initPlayerView() {
        playerManager = CreatorFeedPlayerManager(lifecycleScope)
        binding.recyclerView.adapter = adapter
        listController = CreatorFeedListController(binding.recyclerView, adapter, playerManager).apply {
            onPageChanged = { item, _ ->
                mRealVM.updateCurrentPlayableEpisode(item)
                resetAnalyticsReportStateIfNeeded(item)
                EventManager.onPlayEvent(item)
            }
        }

        // 阶段四不主动提交数据；阶段五接入真实 feed 后，这里会在首屏 pages 更新后触发初始播放。
        adapter.addOnPagesUpdatedListener {
            if (pendingInitialPosition >= 0 && adapter.itemCount > 0) {
                listController.startInitialPlay(pendingInitialPosition)
                pendingInitialPosition = -1
            }
        }
    }

    /**绑定播放器操作按钮，续创条件由 ViewModel 查询并校验。*/
    private fun initActionButtons() {
        binding.btnMenu.setOnSingleClickListener {
            showPopupWindow()
        }
        binding.btnEdit.setOnSingleClickListener {
            EventManager.onFirebaseTrack(
                FirebaseEvent.Category.CREATE,
                FirebaseEvent.Action.WORK_REDRAW_CLICK
            )
            showRedrawConfirm()
        }
        binding.btnKeepCreating.setOnSingleClickListener {
            EventManager.onFirebaseTrack(
                FirebaseEvent.Category.WORK_PLAY,
                FirebaseEvent.Action.CREATING_CLICK
            )
            mRealVM.keepCreating(::openCreationEpisode)
        }
        binding.btnDownload.setOnSingleClickListener {
            EventManager.onFirebaseTrack(
                FirebaseEvent.Category.WORK_PLAY,
                FirebaseEvent.Action.DOWNLOAD_CLICK
            )
            // 下载
            val item = mRealVM.currentPlayableEpisode.get() ?: return@setOnSingleClickListener
            item.episode.videoUrl?.let {
                downloadVideoUrl = it
                if (item.episode.hasWatermark == true) {
                    downloadVideo(false)
                } else {
                    DownloadMethodDialog.show(this@CreatorPlayerActivity)
                }
            }
        }
    }

    private fun openCreationEpisode(
        work: CreatorWorkInfo,
        episode: CreatorEpisodeInfo,
        previous: CreatorEpisodeInfo?
    ) {
        if (isFinishing || isDestroyed) return
        val episodeNo = episode.sortNo ?: return
        val from = if (mRealVM.playSource == CreatorPlaySource.MY_CREATIONS) {
            StoryEditorActivity.FROM_MY_WORKS
        } else {
            StoryEditorActivity.FROM_SERIES
        }
        if (!episode.isNotCreated()) {
            AppRouter.startStory(
                workId = work.workId,
                storyId = episode.storyId,
                workTitle = work.workTitle,
                workType = work.workType,
                totalEpisodeCnt = work.totalEpisodeCnt,
                workRatio = episode.workRatio ?: work.workRatio,
                from = from
            )
            return
        }
        if (episodeNo > 1 && work.workType == CreatorWorkInfo.NOVEL_CREATION) {
            ContinueWritingActivity.start(this, work.workId, episode.storyId, work.workTitle)
            return
        }
        StoryEditorActivity.startContinue(
            context = this,
            workId = work.workId,
            workTitle = work.workTitle,
            storyId = episode.storyId,
            previousStoryId = previous?.storyId,
            chapterIndex = episodeNo,
            workType = work.workType,
            model = AppRouter.getModel(
                totalEpisodeCnt = work.totalEpisodeCnt,
                workType = work.workType,
                sortNo = episodeNo,
                firstCreated = episode.isFirstCreated()
            ),
            videoRatio = episode.workRatio ?: work.workRatio,
            from = from
        )
    }

    /**提交播放器 feed；阶段五 TopEarners 使用共享 work feed 派生出的播放单元。*/
    private fun observeFeed() {
        lifecycleScope.launch {
            mRealVM.playableFeed().collectLatest { pagingData ->
                mRealVM.updateCurrentPlayableEpisode(null)
                listController.reset()
                adapter.submitData(pagingData)
            }
        }
    }

    /**监听播放器状态、进度和完播事件，并转发给当前 ViewHolder。*/
    private fun observePlayer() {
        lifecycleScope.launch {
            playerManager.playerState.collectLatest { state ->
                listController.getCurrentHolder()?.updatePlayerUiState(state)
                if (state is CreatorFeedPlayerManager.CreatorPlayerState.Playing) {
                    reportPlayerShowIfNeeded()
                }
            }
        }
        lifecycleScope.launch {
            playerManager.progressFlow.collectLatest { (current, total) ->
                listController.getCurrentHolder()?.updateProgress(current, total)
                reportPlayerAnalyticsIfNeeded(current, total)
            }
        }
        lifecycleScope.launch {
            playerManager.playerEvent.collectLatest { event ->
                if (event is CreatorFeedPlayerManager.PlayerEvent.OnPlayEnded) {
                    reportPlayerCompleteIfNeeded()
                    val hasNext = listController.smoothScrollToNext()
                    if (!hasNext) {
                        playerManager.pause()
                    }
                }
            }
        }
        lifecycleScope.launch {
            adapter.loadStateFlow.collectLatest { loadStates ->
                if (loadStates.refresh is LoadState.NotLoading && pendingInitialPosition >= 0 && adapter.itemCount > 0) {
                    listController.startInitialPlay(pendingInitialPosition)
                    pendingInitialPosition = -1
                }
            }
        }
    }

    private fun resetAnalyticsReportStateIfNeeded(item: CreatorPlayableEpisode) {
        val episodeKey = creatorMediaKey(item)
        if (analyticsEpisodeKey == episodeKey) return
        analyticsEpisodeKey = episodeKey
        hasReportedValidPlay = false
        reportedProcessNodes.clear()
        hasReportedComplete = false
    }

    private fun reportPlayerShowIfNeeded() {
        if (hasReportedPlayerShow || mRealVM.playSource != CreatorPlaySource.TOP_EARNERS) return
        val item = mRealVM.currentPlayableEpisode.get() ?: return
        val workId = (item.workContext.workId ?: item.episode.workId)
            ?.takeIf { it.isNotBlank() } ?: return
        hasReportedPlayerShow = true
        // GA4：Top Earners 播放器首次开始自动播放。
        EventManager.onFirebaseTrack(
            FirebaseEvent.Category.PLAYER,
            FirebaseEvent.Action.PLAYER_SHOW,
            workId,
            FirebaseEvent.Value.SOURCE_TOP_EARNERS
        )
    }

    private fun reportPlayerAnalyticsIfNeeded(current: Long, total: Long) {
        val item = mRealVM.currentPlayableEpisode.get() ?: return
        if (creatorMediaKey(item) != playerManager.currentPlayingKey || total <= 0L) return
        val workId = (item.workContext.workId ?: item.episode.workId)
            ?.takeIf { it.isNotBlank() } ?: return
        val storyId = item.episode.storyId?.takeIf { it.isNotBlank() } ?: return
        resetAnalyticsReportStateIfNeeded(item)

        val progressPercent = ((current.coerceAtLeast(0L) * 100L) / total).toInt().coerceIn(0, 100)
        if (progressPercent >= 10 && !hasReportedValidPlay) {
            hasReportedValidPlay = true
            // GA4：播放进度达到 10%，记为一次有效播放。
            EventManager.onFirebaseTrack(
                FirebaseEvent.Category.PLAYER,
                FirebaseEvent.Action.PLAYER_VALID_PLAY,
                workId,
                storyId
            )
        }
        reportPlayerProcessIfNeeded(progressPercent, 25, FirebaseEvent.Value.PROCESS_25, workId, storyId)
        reportPlayerProcessIfNeeded(progressPercent, 50, FirebaseEvent.Value.PROCESS_50, workId, storyId)
        reportPlayerProcessIfNeeded(progressPercent, 75, FirebaseEvent.Value.PROCESS_75, workId, storyId)
        if (progressPercent >= 90) {
            reportPlayerCompleteIfNeeded()
        }
    }

    private fun reportPlayerProcessIfNeeded(
        progressPercent: Int,
        targetPercent: Int,
        label: String,
        workId: String,
        storyId: String
    ) {
        if (progressPercent < targetPercent || !reportedProcessNodes.add(targetPercent)) return
        // GA4：播放进度首次达到指定漏斗节点。
        EventManager.onFirebaseTrack(
            FirebaseEvent.Category.PLAYER,
            FirebaseEvent.Action.PLAYER_PROCESS,
            workId,
            storyId,
            label
        )
    }

    private fun reportPlayerCompleteIfNeeded() {
        if (hasReportedComplete) return
        val item = mRealVM.currentPlayableEpisode.get() ?: return
        if (creatorMediaKey(item) != playerManager.currentPlayingKey) return
        val workId = (item.workContext.workId ?: item.episode.workId)
            ?.takeIf { it.isNotBlank() } ?: return
        val storyId = item.episode.storyId?.takeIf { it.isNotBlank() } ?: return
        resetAnalyticsReportStateIfNeeded(item)
        hasReportedComplete = true
        // GA4：播放进度达到 90% 或收到真实完播回调。
        EventManager.onFirebaseTrack(
            FirebaseEvent.Category.PLAYER,
            FirebaseEvent.Action.PLAYER_COMPLETE,
            workId,
            storyId
        )
    }

    /** 与 CreatorFeedPlayerManager 的 mediaKey 生成顺序保持一致。 */
    private fun creatorMediaKey(item: CreatorPlayableEpisode): String? {
        return item.feedKey
            ?: item.episode.storyId
            ?: item.workContext.workId
            ?: item.episode.videoUrl
    }

    /**展示下载进度弹窗；实际下载生命周期在 ViewModel 中控制。*/
    private fun observeDownload() {
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
    }

    /**右上角更多菜单完整复用旧播放器口径：非本人作品举报，本人作品下载。*/
    private fun showPopupWindow() {
        val item = mRealVM.currentPlayableEpisode.get() ?: return
        listPopupWindow = ListPopupWindow(
            context = this,
            dataList = listOfNotNull(
                if (!item.workContext.isMine()) PlayMenuInfo(
                    reportTypeId = PlayMenuInfo.REPORT,
                    reportText = getString(R.string.report),
                    reportIcon = R.drawable.icon_report
                ) else null,
//                if (item.workContext.isMine()) PlayMenuInfo(
//                    reportTypeId = PlayMenuInfo.DOWNLOAD,
//                    reportText = getString(R.string.download),
//                    reportIcon = R.drawable.icon_menu_download
//                ) else null
            ),
            selectedItemId = PlayMenuInfo.REPORT
        )
        listPopupWindow?.setStatus(
            normalBackgroundColor = R.color.bg_color_3_dark.getResColor(),
            selectedBackgroundColor = R.color.bg_color_3_dark.getResColor(),
            normalColor = R.color.text_color_3_dark.getResColor(),
            selectedColor = R.color.text_color_3_dark.getResColor(),
            isSelectBold = false
        )
        listPopupWindow?.setAdapter()
        listPopupWindow?.setOnItemSelectedListener(object : OnItemSelectedListener<PlayMenuInfo> {
            override fun onItemSelected(selectedItem: PlayMenuInfo, position: Int) {
                listPopupWindow?.dismiss()
                listPopupWindow = null
                when (selectedItem.getItemId()) {
                    PlayMenuInfo.REPORT -> showReportDialog(item)
                    PlayMenuInfo.DOWNLOAD -> {
                        item.episode.videoUrl?.let {
                            downloadVideoUrl = it
                            if (item.episode.hasWatermark == true) {
                                downloadVideo(false)
                            } else {
                                DownloadMethodDialog.show(this@CreatorPlayerActivity)
                            }
                        }
                    }
                }
            }
        })
        listPopupWindow?.showAtLocation(binding.btnMenu, Gravity.END or Gravity.TOP, 32, 80.dp)
    }

    /**举报弹窗沿用 FastFilm ReportDialog，homeId 使用当前作品 ID，缺失时兜底剧集所属作品 ID。*/
    private fun showReportDialog(item: CreatorPlayableEpisode) {
        if (AppNavigator.checkLoginIntercepted(showToast = true, isFullScreen = false)) return
        ReportDialog.newInstance(
            supportFragmentManager,
            item.workContext.userId,
            item.workContext.workId ?: item.episode.workId
        )
    }

    /**本人作品点击编辑时先确认重绘，再进入分镜视频列表页。*/
    private fun showRedrawConfirm() {
        val item = mRealVM.currentPlayableEpisode.get() ?: return
        showCommonDialog(
            tip = getString(R.string.redraw_cover_confirm),
            confirmText = getString(R.string.confirm_ok)
        ) {
            EventManager.onFirebaseTrack(
                FirebaseEvent.Category.CREATE,
                FirebaseEvent.Action.REDRAW_CONFIRM_CLICK
            )
            CreatorStoryboardEditorLauncher.start(item)
        }
    }

    /**解析入口 source；未知值按 TopEarners 处理，避免播放器无 source 崩溃。*/
    private fun readPlaySource(): CreatorPlaySource {
        val sourceName = intent.getStringExtra(EXTRA_SOURCE)
        return runCatching { CreatorPlaySource.valueOf(sourceName ?: CreatorPlaySource.TOP_EARNERS.name) }
            .getOrDefault(CreatorPlaySource.TOP_EARNERS)
    }

}
