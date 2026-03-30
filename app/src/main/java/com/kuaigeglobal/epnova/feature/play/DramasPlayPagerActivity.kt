package com.kuaigeglobal.epnova.feature.play

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import androidx.lifecycle.lifecycleScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.showCommonDialog
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.common.permissions.requestRead2WritePermissions
import com.kuaigeglobal.epnova.core.manager.AppEvent
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.response.PlayMenuInfo
import com.kuaigeglobal.epnova.databinding.ActivityDramasPlayPagerBinding
import com.kuaigeglobal.epnova.feature.download.DownloadingDialog
import com.kuaigeglobal.epnova.feature.download.DownloadingViewModel
import com.kuaigeglobal.epnova.feature.editor.EditStoryboardVideoActivity
import com.kuaigeglobal.epnova.feature.play.manger.FeedListController
import com.kuaigeglobal.epnova.feature.play.manger.FeedPlayerManager
import com.kuaigeglobal.epnova.feature.play.menu.ListPopupWindow
import com.kuaigeglobal.epnova.feature.play.menu.OnItemSelectedListener
import com.kuaigeglobal.epnova.feature.play.more.EpisodeChooseDialog
import com.kuaigeglobal.epnova.feature.settings.report.ReportDialog
import com.kuaigeglobal.epnova.router.AppNavigator
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DramasPlayPagerActivity :
    BaseViewModelActivity<ActivityDramasPlayPagerBinding, DramasPlayPagerViewModel>(),
    VideoFeedAdapter.InteractionListener,
    DownloadingViewModel.DownloadStatusHolderFactory {

    companion object {
        @JvmStatic
        fun start(workId: String?, position: Int? = 0, isSeriesMode: Boolean = true) {
            val context = topActivity ?: return
            val starter = Intent(context, DramasPlayPagerActivity::class.java).apply {
                putExtra(Constants.Extra.WORK_ID, workId)
                putExtra(Constants.Extra.POSITION, position)
                putExtra(Constants.Extra.PLAY_MODE, isSeriesMode)
            }
            context.startActivity(starter)
        }
    }

    private val adapter = VideoFeedAdapter(this)
    private lateinit var playerManager: FeedPlayerManager
    private lateinit var listController: FeedListController
    private var listPopupWindow: ListPopupWindow<PlayMenuInfo>? = null

    private var downloadingDialog: DownloadingDialog? = null
    private var pendingJumpPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRealVM.workId = intent.getStringExtra(Constants.Extra.WORK_ID)
        pendingJumpPosition = intent.getIntExtra(Constants.Extra.POSITION, 0)
        val isSeriesMode = intent.getBooleanExtra(Constants.Extra.PLAY_MODE, true)
        initView()

        // 1. 初始化核心组件
        playerManager = FeedPlayerManager(this, lifecycleScope)
        binding.recyclerView.adapter = adapter
        listController = FeedListController(mRealVM, binding.recyclerView, adapter, playerManager)


        lifecycleScope.launch {
            mRealVM.episodeList.collectLatest { list ->
                if (pendingJumpPosition >= 0) {
                    listController.reset()
                }
                adapter.submitList(list) {
                    // 这是一个回调，代表数据已经完全渲染到 UI 上了
                    // 如果是从选集弹窗过来的，或者初始化指定了位置，可以在这里直接跳集！
                    if (pendingJumpPosition >= 0 && list.isNotEmpty()) {
                        listController.startInitialPlay(pendingJumpPosition)
                        pendingJumpPosition = -1 // [关键] 用完立刻失效，防止列表刷新时乱跳
                    }
                }
            }
        }


        mRealVM.downloading.observe(this) {
            if (it) {
                if (downloadingDialog == null) {
                    downloadingDialog = DownloadingDialog.show(this)
                }
            } else {
                downloadingDialog?.dismissAllowingStateLoss()
                downloadingDialog = null
            }
        }

        val initMode = if (isSeriesMode) DramasPlayPagerViewModel.PlayMode.SERIES else DramasPlayPagerViewModel.PlayMode.RECOMMEND
        mRealVM.loadDrama(mRealVM.workId, initMode)

        listController.onPageChanged = { episodeInfo, _ ->
            mRealVM.currentEpisode.set(episodeInfo)
            if (mRealVM.currentPlayMode == DramasPlayPagerViewModel.PlayMode.SERIES) {
                binding.tvTitle.text = getString(R.string.episode_number_format, episodeInfo.sortNo)
            }
            mRealVM.getUserPlayStatus(episodeInfo)
            val realWorkId = mRealVM.workId
            EventManager.onPlayEvent(realWorkId, episodeInfo.storyId)
        }
        // 3. 事件流
        setupPlayerEvents()
    }

    private fun initView() {
        binding.btnEdit.setOnSingleClickListener {
            val story = listController.getCurrentItem() ?: return@setOnSingleClickListener
            showCommonDialog(tip = getString(R.string.redraw_cover_confirm), confirmText = getString(R.string.confirm_ok)) {
                EditStoryboardVideoActivity.start(mRealVM.currentWorkContext.value?.workTitle, story.workId, story.storyId, story.workRatio, story)
            }
            EventManager.onAppTrack(AppEvent.SF_CLICK_WORK_REPLAY_REDRAW_BUTTON)
        }
        binding.btnMenu.setOnSingleClickListener {
            showPopupWindow()
        }
    }

    private fun showPopupWindow() {
        val currentEpisode = mRealVM.currentEpisode.get() ?: return
        val workInfo = mRealVM.currentWorkContext.value ?: return
        listPopupWindow = ListPopupWindow(
            context = this,
            dataList = listOfNotNull(
                if (!workInfo.isMine()) PlayMenuInfo(
                    reportTypeId = PlayMenuInfo.REPORT,
                    reportText = getString(R.string.report),
                    reportIcon = R.drawable.icon_report
                ) else null,
                if (workInfo.isMine()) PlayMenuInfo(
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
        listPopupWindow?.setOnItemSelectedListener(object :
            OnItemSelectedListener<PlayMenuInfo> {
            override fun onItemSelected(selectedItem: PlayMenuInfo, position: Int) {
                listPopupWindow?.dismiss()
                listPopupWindow = null
                when (selectedItem.getItemId()) {
                    PlayMenuInfo.REPORT -> {
                        if (AppNavigator.checkLoginIntercepted(showToast = true, isFullScreen = false)) return
                        //举报
                        ReportDialog.newInstance(
                            supportFragmentManager,
                            workInfo.userId,
                            if (mRealVM.currentPlayMode == DramasPlayPagerViewModel.PlayMode.SERIES) mRealVM.workId else currentEpisode.workId//剧集模式下workId是剧集id
                        )
                    }

                    PlayMenuInfo.DOWNLOAD -> {
                        //下载
                        requestRead2WritePermissions { granted ->
                            if (granted) {
                                currentEpisode.videoUrl?.let { mRealVM.downloadVideo(it) }
                            }
                        }
                    }
                }
            }
        })
        val gravity = Gravity.END or Gravity.TOP
        listPopupWindow?.showAtLocation(binding.btnMenu, gravity, 32, 80.dp)
    }


    private fun setupPlayerEvents() {
        lifecycleScope.launch {
            mRealVM.currentWorkContext.collectLatest { workInfo ->
                workInfo?.let {
                    adapter.updateWorkInfo(it)
                }
            }
        }
        // 进度同步 (幽灵进度问题)
        lifecycleScope.launch {
            playerManager.progressFlow.collect { (current, total) ->
                updateViewHolderSafely { holder ->
                    holder?.updateProgress(current, total)
                }
            }
        }

        lifecycleScope.launch {
            playerManager.playerState.collect { state ->
                // [优化] 直接把状态甩给 ViewHolder 处理，逻辑更聚拢
                updateViewHolderSafely { holder ->
                    holder?.updatePlayerUiState(state)
                }
                // [补充] 针对 Activity 级别的特殊处理（如 Toast）
                if (state is FeedPlayerManager.FeedPlayerState.Error) {
                    if (!state.isNoCache) {
                        // 有缓存断网，仅弹 Toast
                        state.msg.showToast()
                    }
                }
            }
        }

        // 播放结束策略
        lifecycleScope.launch {
            playerManager.playerEvent.collect { event ->
                if (event is FeedPlayerManager.PlayerEvent.OnPlayEnded) handlePlayEnded()
            }
        }
    }

    // [封装] 安全更新 ViewHolder 的辅助方法
    private fun updateViewHolderSafely(action: (VideoFeedAdapter.VideoViewHolder?) -> Unit) {
        val playingId = playerManager.currentPlayingId ?: return
        // 遍历当前 RecyclerView 所有可见的子 View
        for (i in 0 until binding.recyclerView.childCount) {
            val child = binding.recyclerView.getChildAt(i)
            val holder = binding.recyclerView.getChildViewHolder(child) as? VideoFeedAdapter.VideoViewHolder
            val item = adapter.currentList.getOrNull(holder?.bindingAdapterPosition ?: -1)

            if (item?.storyId == playingId) {
                action(holder)
                break
            }
        }
    }

    private fun handlePlayEnded() {
        if (adapter.itemCount == 0) return

        if (!listController.smoothScrollToNext()) {
            // 剧集列表已经滑动到底部（当前作品完播）
            if (mRealVM.currentPlayMode == DramasPlayPagerViewModel.PlayMode.RECOMMEND) {
                if (adapter.currentList.isNotEmpty()) {
                    "正在为您跳转新剧".showToast()
                    pendingJumpPosition = 0
                    mRealVM.loadNextRandomWork()
                }
            } else {
                // 剧集模式：不自动跳新剧，提示最后一集
                getString(R.string.already_last_episode).showToast()
                // playerManager.replay() // 根据产品需求决定是否重播
            }
        }
    }

    // --- 交互回调 ---
    override fun onTogglePlay(position: Int) = playerManager.togglePlay()
    override fun onSeek(position: Int, progress: Long) = playerManager.seekTo(progress)
    override fun onToggleCollect(position: Int) {
        mRealVM.collectToggle()
    }

    override fun onToggleLike(position: Int) {
        val item = adapter.currentList.getOrNull(position)
        mRealVM.likeToggle(item)
    }

    override fun onReportPlayback(position: Int, episodeInfo: EpisodeInfo?, progress: Long) {
        EventManager.onPlayEvent(mRealVM.workId, episodeInfo, progress)
    }

    override fun showEpisodeSelectDialog(position: Int) {
        val dialog = EpisodeChooseDialog.newInstance(mRealVM.workId, mRealVM.episodeList.value, listController.getCurrentItem())
        dialog.onEpisodeSelected = { episodeInfo ->
            val position = adapter.currentList.indexOf(episodeInfo)
            listController.startInitialPlay(position)
        }
        dialog.show(supportFragmentManager)
    }

    // --- 生命周期 ---
    override fun onPause() {
        super.onPause(); playerManager.pause()
    }

    override fun onResume() {
        super.onResume(); playerManager.resumeOnForeground()
    }

    override fun onDestroy() {
        super.onDestroy(); playerManager.release()
    }

    override fun getStatusHolder(): DownloadingViewModel.DownloadStatusHolder? {
        return mRealVM
    }
}