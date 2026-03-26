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
        fun start(workId: String?, position: Int? = 0) {
            val context = topActivity ?: return
            val starter = Intent(context, DramasPlayPagerActivity::class.java).apply {
                putExtra(Constants.Extra.WORK_ID, workId)
                putExtra(Constants.Extra.POSITION, position)
            }
            context.startActivity(starter)
        }
    }

    private val adapter = VideoFeedAdapter(this)
    private lateinit var playerManager: FeedPlayerManager
    private lateinit var listController: FeedListController
    private var listPopupWindow: ListPopupWindow<PlayMenuInfo>? = null

    private var workId: String? = null
    private var downloadingDialog: DownloadingDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workId = intent.getStringExtra(Constants.Extra.WORK_ID)
        val initialPos = intent.getIntExtra(Constants.Extra.POSITION, 0)

        initView()

        // 1. 初始化核心组件
        playerManager = FeedPlayerManager(this, lifecycleScope)
        binding.recyclerView.adapter = adapter
        listController = FeedListController(binding.recyclerView, adapter, playerManager)

        // 2. 数据流
        lifecycleScope.launch {
            mRealVM.feedFlow.collectLatest {
                adapter.submitData(it)
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

        // 告知 Controller 初始位置 (需要在 FeedListController 中加个方法，见下文)
        listController.setInitialPosition(initialPos)

        // 加载数据
        // 因为 Repository 是单例且缓存了 Flow，如果列表页也用了 FeedRepository，
        // 这里会立刻拿到数据，不需要网络请求
        mRealVM.loadFeed(!workId.isNullOrEmpty(), workId)

        listController.onPageChanged = { workInfo, position ->
            mRealVM.currentWork.set(workInfo)
            if (mRealVM.currentPlayMode == DramasPlayPagerViewModel.PlayMode.SERIES) {
                binding.tvTitle.text = getString(R.string.episode_number_format, workInfo.lastEpisodeNo)
            }
            val realWorkId =
                if (mRealVM.currentPlayMode == DramasPlayPagerViewModel.PlayMode.SERIES) workId else workInfo.workId
            EventManager.onPlayEvent(workInfo.userId, realWorkId, workInfo.getEpisodeId())
        }
        // 3. 事件流
        setupPlayerEvents()
    }

    private fun initView() {
        binding.btnEdit.setOnSingleClickListener {
            val currentWork = mRealVM.currentWork.get() ?: return@setOnSingleClickListener
            val story = currentWork.workStoryItemList?.firstOrNull() ?: return@setOnSingleClickListener
            showCommonDialog(tip = getString(R.string.redraw_cover_confirm), confirmText = getString(R.string.confirm_ok)) {
                EditStoryboardVideoActivity.start(currentWork.workTitle, story.workId, story.storyId, story.workRatio, story)
            }
            EventManager.onAppTrack(AppEvent.SF_CLICK_WORK_REPLAY_REDRAW_BUTTON)
        }
        binding.btnMenu.setOnSingleClickListener {
            showPopupWindow()
        }
    }

    private fun showPopupWindow() {
        val currentWork = mRealVM.currentWork.get() ?: return
        listPopupWindow = ListPopupWindow(
            context = this,
            dataList = listOfNotNull(
                if (!currentWork.isMine()) PlayMenuInfo(
                    reportTypeId = PlayMenuInfo.REPORT,
                    reportText = getString(R.string.report),
                    reportIcon = R.drawable.icon_report
                ) else null,
                if (currentWork.isMine()) PlayMenuInfo(
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
                            currentWork.userId,
                            if (mRealVM.currentPlayMode == DramasPlayPagerViewModel.PlayMode.SERIES) workId else currentWork.workId//剧集模式下workId是剧集id
                        )
                    }

                    PlayMenuInfo.DOWNLOAD -> {
                        //下载
                        requestRead2WritePermissions { granted ->
                            if (granted) {
                                currentWork.getPlayUrl()?.let { mRealVM.downloadVideo(it) }
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
        // 进度同步 (幽灵进度问题)
        lifecycleScope.launch {
            playerManager.progressFlow.collect { (current, total) ->
                updateViewHolderSafely { holder ->
                    holder.updateProgress(current, total)
                }
            }
        }

        lifecycleScope.launch {
            playerManager.playerState.collect { state ->
                // [优化] 直接把状态甩给 ViewHolder 处理，逻辑更聚拢
                updateViewHolderSafely { holder ->
                    holder.updatePlayerUiState(state)
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
    private fun updateViewHolderSafely(action: (VideoFeedAdapter.VideoViewHolder) -> Unit) {
        val pos = listController.getCurrentPosition()
        if (pos >= 0) {
            val holder =
                binding.recyclerView.findViewHolderForAdapterPosition(pos) as? VideoFeedAdapter.VideoViewHolder
            val item = adapter.peek(pos)

            // [关键] 只有当 ViewHolder 当前展示的数据 ID 与 播放器正在播的 ID 一致时，才允许更新 UI
            if (holder != null && item?.workId == playerManager.currentPlayingId) {
                action(holder)
            }
        }
    }

    private fun handlePlayEnded() {
        if (!listController.smoothScrollToNext()) {
            getString(R.string.already_last_episode).showToast()
            //playerManager.replay()
        }
//        if (mRealVM.currentPlayMode == DramasPlayPagerViewModel.PlayMode.RECOMMEND) {
//            if (!listController.smoothScrollToNext()) {
//                "已经是最后一集了".showToast()
//                playerManager.replay()
//            }
//        } else {
//            playerManager.replay()
//        }
    }

    // --- 交互回调 ---
    override fun onTogglePlay(position: Int) = playerManager.togglePlay()
    override fun onSeek(position: Int, progress: Long) = playerManager.seekTo(progress)
    override fun showEpisodeSelectDialog(position: Int) {
        EpisodeChooseDialog.newInstance(null, null, null, 0, 0).show(supportFragmentManager)
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