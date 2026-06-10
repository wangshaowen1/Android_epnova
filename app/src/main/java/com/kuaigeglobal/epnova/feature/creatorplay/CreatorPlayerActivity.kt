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
import com.kuaigeglobal.epnova.core.manager.FirebaseEventParam
import com.kuaigeglobal.epnova.core.manager.FirebaseEventValue
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlaySource
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlayableEpisode
import com.kuaigeglobal.epnova.core.network.apis.response.PlayMenuInfo
import com.kuaigeglobal.epnova.databinding.ActivityCreatorPlayerBinding
import com.kuaigeglobal.epnova.feature.download.DownloadingDialog
import com.kuaigeglobal.epnova.feature.download.DownloadingViewModel
import com.kuaigeglobal.epnova.feature.editor.CreatorStoryboardEditorLauncher
import com.kuaigeglobal.epnova.feature.play.menu.ListPopupWindow
import com.kuaigeglobal.epnova.feature.play.menu.OnItemSelectedListener
import com.kuaigeglobal.epnova.feature.play.more.DownloadMethodDialog
import com.kuaigeglobal.epnova.feature.settings.report.ReportDialog
import com.kuaigeglobal.epnova.router.AppNavigator
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
    private var lastFirebaseValidPlayStoryId: String? = null
    private var lastFirebaseProcessStoryId: String? = null
    private val lastFirebaseProcessNodes = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRealVM.playSource = readPlaySource()
        mRealVM.initialPosition = intent.getIntExtra(EXTRA_POSITION, 0)
        mRealVM.sourceKey = intent.getStringExtra(EXTRA_SOURCE_KEY)
        pendingInitialPosition = mRealVM.initialPosition

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
                EventManager.onPlayEvent(item)
                resetFirebasePlaybackState(item.episode.storyId)
                trackPlayerView(item)
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

    /**绑定右上角操作：本人作品显示编辑按钮，更多菜单内按本人/非本人切换下载或举报。*/
    private fun initActionButtons() {
        binding.btnMenu.setOnSingleClickListener {
            showPopupWindow()
        }
        binding.btnEdit.setOnSingleClickListener {
            showRedrawConfirm()
        }
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
            }
        }
        lifecycleScope.launch {
            playerManager.progressFlow.collectLatest { (current, total) ->
                listController.getCurrentHolder()?.updateProgress(current, total)
                val item = mRealVM.currentPlayableEpisode.get() ?: return@collectLatest
                trackFirebasePlaybackProgress(item, current, total)
            }
        }
        lifecycleScope.launch {
            playerManager.playerEvent.collectLatest { event ->
                if (event is CreatorFeedPlayerManager.PlayerEvent.OnPlayEnded) {
                    mRealVM.currentPlayableEpisode.get()?.let { trackPlayerComplete(it) }
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
                if (item.workContext.isMine()) PlayMenuInfo(
                    reportTypeId = PlayMenuInfo.DOWNLOAD,
                    reportText = getString(R.string.download),
                    reportIcon = R.drawable.icon_menu_download
                ) else null
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
                FirebaseEvent.WORK_REPLAY_REDRAW_CLICK,
                FirebaseEventParam.DRAMA_ID to (item.workContext.workId ?: item.episode.workId),
                FirebaseEventParam.EPISODE_ID to item.episode.storyId
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

    private fun resetFirebasePlaybackState(storyId: String?) {
        if (lastFirebaseProcessStoryId == storyId) return
        lastFirebaseValidPlayStoryId = null
        lastFirebaseProcessStoryId = storyId
        lastFirebaseProcessNodes.clear()
    }

    private fun trackPlayerView(item: CreatorPlayableEpisode) {
        EventManager.onFirebaseTrack(
            FirebaseEvent.PLAYER_VIEW,
            FirebaseEventParam.DRAMA_ID to (item.workContext.workId ?: item.episode.workId),
            FirebaseEventParam.EPISODE_ID to item.episode.storyId,
            FirebaseEventParam.SOURCE to item.firebasePlayerSource()
        )
    }

    private fun trackFirebasePlaybackProgress(item: CreatorPlayableEpisode, current: Long, total: Long) {
        if (total <= 0) return
        val episode = item.episode
        val progressPercent = ((current.coerceAtLeast(0L) * 100L) / total).toInt().coerceIn(0, 100)
        if (progressPercent >= 10 && lastFirebaseValidPlayStoryId != episode.storyId) {
            lastFirebaseValidPlayStoryId = episode.storyId
            EventManager.onFirebaseTrack(
                FirebaseEvent.PLAYER_VALID_PLAY,
                FirebaseEventParam.DRAMA_ID to (item.workContext.workId ?: episode.workId),
                FirebaseEventParam.EPISODE_ID to episode.storyId
            )
        }
        val processNode = when {
            progressPercent >= 75 -> 75
            progressPercent >= 50 -> 50
            progressPercent >= 25 -> 25
            else -> return
        }
        if (lastFirebaseProcessStoryId != episode.storyId) {
            resetFirebasePlaybackState(episode.storyId)
        }
        if (lastFirebaseProcessNodes.add(processNode)) {
            EventManager.onFirebaseTrack(
                FirebaseEvent.PLAYER_PROCESS,
                FirebaseEventParam.DRAMA_ID to (item.workContext.workId ?: episode.workId),
                FirebaseEventParam.EPISODE_ID to episode.storyId,
                FirebaseEventParam.PROCESS to processNode
            )
        }
    }

    private fun trackPlayerComplete(item: CreatorPlayableEpisode) {
        EventManager.onFirebaseTrack(
            FirebaseEvent.PLAYER_COMPLETE,
            FirebaseEventParam.DRAMA_ID to (item.workContext.workId ?: item.episode.workId),
            FirebaseEventParam.EPISODE_ID to item.episode.storyId
        )
    }

    private fun CreatorPlayableEpisode.firebasePlayerSource(): String? {
        return when (source) {
            CreatorPlaySource.TOP_EARNERS -> FirebaseEventValue.SOURCE_TOP_EARNERS
            else -> null
        }
    }
}
