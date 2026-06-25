package com.kuaigeglobal.epnova.feature.play

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.showCommonDialog
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.common.permissions.requestRead2WritePermissions
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
import com.kuaigeglobal.epnova.feature.play.more.AdUnlockDialog
import com.kuaigeglobal.epnova.feature.play.more.EpisodeChooseDialog
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.core.manager.FirebaseEventParam
import com.kuaigeglobal.epnova.feature.recharge.PowerRechargeDialog
import com.kuaigeglobal.epnova.feature.settings.report.ReportDialog
import com.kuaigeglobal.epnova.feature.splash.newer.FirstRechargeDialog
import com.kuaigeglobal.epnova.router.AppNavigator
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showAllowingStateLoss
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DramasPlayPagerActivity :
    BaseViewModelActivity<ActivityDramasPlayPagerBinding, DramasPlayPagerViewModel>(),
    VideoFeedAdapter.InteractionListener,
    DownloadingViewModel.DownloadStatusHolderFactory {

    companion object {
        private const val EXTRA_PLAYER_SOURCE = "extra_player_source"
        private const val EXTRA_BANNER_ID = "extra_banner_id"

        @JvmStatic
        fun start(
            workId: String?,
            position: Int? = 0,
            isSeriesMode: Boolean = true,
            source: String? = null,
            bannerId: String? = null
        ) {
            val context = topActivity ?: return
            val starter = Intent(context, DramasPlayPagerActivity::class.java).apply {
                putExtra(Constants.Extra.WORK_ID, workId)
                putExtra(Constants.Extra.POSITION, position)
                putExtra(Constants.Extra.PLAY_MODE, isSeriesMode)
                putExtra(EXTRA_PLAYER_SOURCE, source)
                putExtra(EXTRA_BANNER_ID, bannerId)
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
    private var pendingJumpAllowResumeState = true

    /** 当前集处于解锁流程中，阻止自动切集 */
    private var isEpisodeLocked = false

    /**
     * 当前播放集的最近上报节点。
     *
     * 产品口径是进度到 10%、20%... 时上报一次，不要求补发历史节点；这里不保存“本集所有已上报节点集合”，
     * 只记录最近一次节点，避免 progressFlow 每 200ms 停留在同一个 10% 档位时连续刷请求。
     * 如果用户拖回前面再重新播放到某节点，允许重复上报，服务端以最新进度覆盖即可。
     */
    private var lastProgressReportStoryId: String? = null
    private var lastProgressReportNode = 0
    private var lastFirebaseValidPlayStoryId: String? = null
    private var lastFirebaseProcessStoryId: String? = null
    private val lastFirebaseProcessNodes = mutableSetOf<Int>()

    /**
     * 播放器最近一次回调的位置。
     *
     * 暂停、退出、切集时可能拿不到新的 ExoPlayer 回调，只能用这里缓存的最后位置做兜底上报；
     * 不在这里主动读播放器，避免 Activity 直接依赖 ExoPlayer 实例。
     */
    private var lastPlaybackProgressMs = 0L
    private var lastPlaybackDurationMs = 0L
    private var playerSource: String? = null
    private var playerBannerId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRealVM.workId = intent.getStringExtra(Constants.Extra.WORK_ID)
        playerSource = intent.getStringExtra(EXTRA_PLAYER_SOURCE)
        playerBannerId = intent.getStringExtra(EXTRA_BANNER_ID)
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
                    listController.updateScrollLimit(list)
                    // 如果是从选集弹窗过来的，或者初始化指定了位置，可以在这里直接跳集！
                    if (pendingJumpPosition >= 0 && list.isNotEmpty()) {
                        val (targetPosition, resumePositionMs) = resolveInitialPlayTarget(list, pendingJumpAllowResumeState)
                        listController.startInitialPlay(targetPosition, resumePositionMs)
                        pendingJumpPosition = -1 // [关键] 用完立刻失效，防止列表刷新时乱跳
                        pendingJumpAllowResumeState = true
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
            isEpisodeLocked = false  // 切集时解除拦截
            mRealVM.currentEpisode.set(episodeInfo)
            resetProgressReportStateIfNeeded(episodeInfo.storyId)
            mRealVM.getUserPlayStatus(episodeInfo)
            val realWorkId = mRealVM.workId
            EventManager.onPlayEvent(realWorkId, episodeInfo.storyId)
            trackPlayerView(episodeInfo)
        }

        listController.onEpisodeLocked = { episode, isAutoUnlockFailed ->
            isEpisodeLocked = true  // 进入解锁流程，阻止自动切集
            if (isAutoUnlockFailed) {
                // 自动解锁因余额不足失败：先查广告次数，再决定弹首充礼包或普通充值弹窗
                // 充值弹窗关闭后在 onDismiss 里决定下一步（广告 or 手动解锁）
                lifecycleScope.launch {
                    val unlockInfo = mRealVM.fetchUnlockInfo(episode)
                    val adRemain = unlockInfo?.adUnlockRemainCount ?: 0
                    val costNums = unlockInfo?.costNums ?: 0
                    val shown = tryShowFirstRechargeDialog(episode, adRemain, costNums)
                    if (!shown) showCoinRechargeDialog(episode, costNums, adRemain)
                }
            } else {
                // 自动解锁未开启时：余额不足先充值；余额充足才展示手动解锁页。
                lifecycleScope.launch {
                    val unlockInfo = mRealVM.fetchUnlockInfo(episode)
                    if (adapter.currentList.getOrNull(listController.getCurrentPosition())?.storyId != episode.storyId) {
                        return@launch
                    }
                    val adRemain = unlockInfo?.adUnlockRemainCount ?: 0
                    val costNums = unlockInfo?.costNums ?: 0
                    val balance = unlockInfo?.userCoinsBalance ?: User.get().coins
                    val isCoinsInsufficient = costNums > balance
                    if (isCoinsInsufficient) {
                        val shown = tryShowFirstRechargeDialog(episode, adRemain, costNums)
                        if (!shown) showCoinRechargeDialog(episode, costNums, adRemain)
                    } else {
                        showUnlockOverlay(episode)
                    }
                }
            }
        }
        // 3. 事件流
        setupPlayerEvents()
    }

    private fun initView() {
        binding.btnEdit.setOnSingleClickListener {
            val story = listController.getCurrentItem() ?: return@setOnSingleClickListener
            showCommonDialog(tip = getString(R.string.redraw_cover_confirm), confirmText = getString(R.string.confirm_ok)) {
                EventManager.onFirebaseTrack(
                    FirebaseEvent.WORK_REPLAY_REDRAW_CLICK,
                    FirebaseEventParam.DRAMA_ID to (story.workId ?: mRealVM.workId),
                    FirebaseEventParam.EPISODE_ID to story.storyId
                )
                EditStoryboardVideoActivity.start(mRealVM.currentWorkContext.value?.workTitle, story.workId, story.storyId, story.workRatio, story)
            }
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
                        // 举报接口需要作品维度 ID，EpisodeInfo.workId 依赖目录接口回填，推荐流下可能为空。
                        val reportHomeId = workInfo.workId ?: mRealVM.workId ?: currentEpisode.workId
                        //举报
                        ReportDialog.newInstance(
                            supportFragmentManager,
                            workInfo.userId,
                            reportHomeId
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


    /**
     * 尝试弹出首充礼包弹窗。
     * 满足以下全部条件才弹出：未付费用户、本 UID 生命周期内首次、服务端 needShow=1。
     * 礼包支付成功 → 直接重新触发自动解锁；关闭礼包 → 接续 PowerRechargeDialog。
     * @return true=弹窗已展示（调用方不需要再弹 PowerRechargeDialog），false=未弹出
     */
    private suspend fun tryShowFirstRechargeDialog(episode: EpisodeInfo, adRemain: Int, costNums: Int): Boolean {
        val canShow = !User.get().hasPurchasedCoins() && !Config.get().isFirstRechargeShown(FirstRechargeDialog.SCENE_OTHER)
        if (!canShow) return false

        val data = mRealVM.fetchFirstRechargeInfo() ?: return false
        if (!data.isNeedShow()) return false

        // 确认弹窗成功展示后再标记，避免 show() 失败时 flag 被永久占用
        val dialog = FirstRechargeDialog.show(FirstRechargeDialog.SCENE_OTHER, data) ?: return false
        Config.get().markFirstRechargeShown(FirstRechargeDialog.SCENE_OTHER)
        dialog.onPaySuccess = { retryUnlockAfterRecharge(episode) }
        dialog.onClose = {
            // 关闭礼包弹窗 → 弹普通充值面板，接续原有充值/解锁流程
            showCoinRechargeDialog(episode, costNums, adRemain)
        }
        return true
    }

    private fun showCoinRechargeDialog(episode: EpisodeInfo, costNums: Int, adRemain: Int) {
        PowerRechargeDialog.show(
            costNums,
            onRechargeSuccess = {
                retryUnlockAfterRecharge(episode)
            },
            onDismiss = { recharged ->
                if (!recharged) showUnlockFallbackAfterRechargeDismiss(episode, adRemain)
            }
        )
    }

    private fun retryUnlockAfterRecharge(episode: EpisodeInfo) {
        // 充值成功：先设好跳转位置，等 collectLatest→submitList 回调后再 startInitialPlay
        // 避免 adapter.currentList 还是旧 lockStatus=2 时再次触发解锁弹窗
        isEpisodeLocked = false
        val pos = adapter.currentList.indexOfFirst { it.storyId == episode.storyId }
        if (pos >= 0) requestPendingJump(pos, allowResumeState = false)
        mRealVM.onEpisodeManuallyUnlocked(episode)
        listController.reset()
    }

    private fun showUnlockFallbackAfterRechargeDismiss(episode: EpisodeInfo, adRemain: Int) {
        if (adRemain > 0) showAdUnlockDialog(episode, adRemain)
        else showUnlockOverlay(episode)
    }

    private fun showUnlockOverlay(episode: EpisodeInfo) {
        // 用 storyId 找到该集在 adapter 中的真实位置，防止异步回调时用户已滑走
        val position = adapter.currentList.indexOfFirst { it.storyId == episode.storyId }
        if (position < 0 || position != listController.getCurrentPosition()) return
        val holder = binding.recyclerView.findViewHolderForAdapterPosition(position)
                as? VideoFeedAdapter.VideoViewHolder ?: return

        val controller = mRealVM.createUnlockController(episode, lifecycleScope)
        controller.onUnlockSuccess = { unlockedEpisode ->
            isEpisodeLocked = false
            val pos = adapter.currentList.indexOfFirst { it.storyId == unlockedEpisode.storyId }
            if (pos >= 0) {
                requestPendingJump(pos, allowResumeState = false)
                // 立即隐藏覆盖层；submitList 回调完成后 startInitialPlay 再触发播放
                val freshHolder = binding.recyclerView.findViewHolderForAdapterPosition(pos)
                        as? VideoFeedAdapter.VideoViewHolder
                freshHolder?.hideUnlockOverlay()
            }
            mRealVM.onEpisodeManuallyUnlocked(unlockedEpisode)
            listController.reset()
        }
        controller.onInsufficientCoins = { adRemainCount ->
            lifecycleScope.launch {
                val costNums = episode.costNums ?: 0
                val shown = tryShowFirstRechargeDialog(episode, adRemainCount, costNums)
                if (!shown) {
                    // 弹充值弹窗，关闭后决定下一步（广告 or 继续停在手动解锁覆盖层）
                    PowerRechargeDialog.show(
                        episode.costNums,
                        onRechargeSuccess = {
                            isEpisodeLocked = false
                            val pos = adapter.currentList.indexOfFirst { it.storyId == episode.storyId }
                            if (pos >= 0) requestPendingJump(pos, allowResumeState = false)
                            mRealVM.onEpisodeManuallyUnlocked(episode)
                            listController.reset()
                        },
                        onDismiss = {
                            if (adRemainCount > 0) showAdUnlockDialog(episode, adRemainCount)
                            // adRemainCount == 0：手动解锁覆盖层本身还在，无需额外操作
                        }
                    )
                }
            }
        }
        holder.showUnlockOverlay(controller)
    }

    private fun showAdUnlockDialog(episode: EpisodeInfo, adRemainCount: Int) {
        val adDialog = AdUnlockDialog.newInstance(mRealVM.workId, mRealVM.currentWorkContext.value?.workCoverUrl, episode, adRemainCount)
        adDialog.onUnlockSuccess = {
            isEpisodeLocked = false
            val position = adapter.currentList.indexOfFirst { it.storyId == episode.storyId }
            if (position >= 0) requestPendingJump(position, allowResumeState = false)
            mRealVM.onEpisodeManuallyUnlocked(episode)
            listController.reset()
        }
        adDialog.onDismissWithoutWatch = {
            showUnlockOverlay(episode)
        }
        adDialog.showAllowingStateLoss(supportFragmentManager, "ad_unlock")
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
                reportProgressNodeIfNeeded(current, total)
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

    private fun requestPendingJump(position: Int, allowResumeState: Boolean) {
        pendingJumpPosition = position
        pendingJumpAllowResumeState = allowResumeState
    }

    private fun resolveInitialPlayTarget(list: List<EpisodeInfo>, allowResumeState: Boolean): Pair<Int, Long> {
        val fallbackPosition = pendingJumpPosition.coerceIn(0, list.lastIndex)
        if (!allowResumeState) {
            // 解锁/充值成功后的显式跳集必须播放刚解锁的剧集，不能再被旧续播记录覆盖。
            return fallbackPosition to 0L
        }

        val userPlayState = mRealVM.userPlayState.value
        val resumeStoryId = userPlayState?.resumeStoryId ?: userPlayState?.lastStoryId
        val resumeSortNo = userPlayState?.resumeSortNo ?: userPlayState?.lastSortNo
        val resumePosition = when {
            !resumeStoryId.isNullOrEmpty() -> list.indexOfFirst { it.storyId == resumeStoryId }
            resumeSortNo != null -> list.indexOfFirst { it.sortNo == resumeSortNo }
            else -> -1
        }

        val targetPosition = if (resumePosition >= 0) resumePosition else fallbackPosition

        // finished=true 表示服务端已判定播到片尾，即使返回了最近集信息，也必须从 00:00 起播。
        // 没有续播记录或续播集不在当前列表里时，继续沿用入口传入的 position，避免影响选集/推荐入口。
        val resumeSecond = if (resumePosition >= 0 && userPlayState?.finished != true) {
            userPlayState?.resumeSecond ?: 0
        } else {
            0
        }
        val resumePositionMs = resumeSecond.coerceAtLeast(0).toLong() * 1000L
        return targetPosition to resumePositionMs
    }

    private fun resetProgressReportStateIfNeeded(storyId: String?) {
        if (lastProgressReportStoryId == storyId) return
        lastProgressReportStoryId = storyId
        lastProgressReportNode = 0
        lastFirebaseValidPlayStoryId = null
        lastFirebaseProcessStoryId = storyId
        lastFirebaseProcessNodes.clear()
        lastPlaybackProgressMs = 0L
        lastPlaybackDurationMs = 0L
    }

    private fun reportProgressNodeIfNeeded(current: Long, total: Long) {
        val episode = mRealVM.currentEpisode.get() ?: return
        if (episode.storyId != playerManager.currentPlayingId || total <= 0) return

        lastPlaybackProgressMs = current
        lastPlaybackDurationMs = total

        val progressPercent = ((current.coerceAtLeast(0L) * 100L) / total).toInt().coerceIn(0, 100)
        val currentNode = (progressPercent / 10) * 10
        trackFirebasePlaybackProgress(episode, progressPercent)

        // 只处理 10%~90% 的过程节点：100% 由播放结束事件带 completed=true 上报。
        // 不补发跳过的节点，例如从 5% 拖到 60% 只会上报当前所在档位，避免把 seek 当成真实观看。
        if (currentNode !in 10..90 || currentNode == lastProgressReportNode) return

        lastProgressReportStoryId = episode.storyId
        lastProgressReportNode = currentNode
        mRealVM.reportPlaybackProgress(
            episode = episode,
            progressMs = current,
            durationMs = total,
            completed = false,
            progressPercent = progressPercent,
        )
    }

    private fun reportCurrentPlayback(completed: Boolean) {
        val episode = mRealVM.currentEpisode.get() ?: return
        if (episode.storyId != playerManager.currentPlayingId) return
        val durationMs = lastPlaybackDurationMs.takeIf { it > 0 } ?: ((episode.duration ?: 0L) * 1000L)

        // completed=true 是片尾语义，不是普通进度点。
        // 服务端会据此在下次查询时返回 finished=true/resumeSecond=0，所以完播时强制用总时长和 100% 上报。
        val progressMs = if (completed && durationMs > 0) durationMs else lastPlaybackProgressMs
        val progressPercent = if (completed) {
            100
        } else if (durationMs > 0) {
            ((progressMs.coerceAtLeast(0L) * 100L) / durationMs).toInt().coerceIn(0, 100)
        } else {
            null
        }
        mRealVM.reportPlaybackProgress(
            episode = episode,
            progressMs = progressMs,
            durationMs = durationMs,
            completed = completed,
            progressPercent = progressPercent,
        )
        if (completed) {
            EventManager.onFirebaseTrack(
                FirebaseEvent.PLAYER_COMPLETE,
                FirebaseEventParam.DRAMA_ID to (episode.workId ?: mRealVM.workId),
                FirebaseEventParam.EPISODE_ID to episode.storyId
            )
        }
    }

    private fun handlePlayEnded() {
        reportCurrentPlayback(completed = true)
        if (isEpisodeLocked) return  // 解锁流程中，不自动切集
        if (adapter.itemCount == 0) return

        if (!listController.smoothScrollToNext()) {
            // 剧集列表已经滑动到底部（当前作品完播）
            if (mRealVM.currentPlayMode == DramasPlayPagerViewModel.PlayMode.RECOMMEND) {
                if (adapter.currentList.isNotEmpty()) {
                    showToast(R.string.drama_has_ended)
                    requestPendingJump(0, allowResumeState = true)
                    val endedStoryId = playerManager.currentPlayingId
                    mRealVM.loadNextRandomWork(onFailure = {
                        pendingJumpPosition = -1
                        pendingJumpAllowResumeState = true
                        if (playerManager.currentPlayingId != endedStoryId) return@loadNextRandomWork
                        playerManager.pauseAtFirstFrame()
                    })
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
        if (AppNavigator.checkLoginIntercepted(true)) {
            return
        }
        mRealVM.collectToggle()
    }

    override fun onToggleLike(position: Int) {
        if (AppNavigator.checkLoginIntercepted(true)) {
            return
        }
        val item = adapter.currentList.getOrNull(position)
        mRealVM.likeToggle(item)
    }

    override fun onReportPlayback(position: Int, episodeInfo: EpisodeInfo?, progress: Long, duration: Long) {
        mRealVM.reportPlaybackProgress(episodeInfo, progress, duration)
    }

    override fun showEpisodeSelectDialog(position: Int) {
        val dialog = EpisodeChooseDialog.newInstance(mRealVM.workId, mRealVM.episodeList.value, listController.getCurrentItem())
        dialog.onEpisodeSelected = { episodeInfo ->
            val position = episodeInfo?.storyId?.takeIf { it.isNotBlank() }?.let { storyId ->
                adapter.currentList.indexOfFirst { it.storyId == storyId }
            }?.takeIf { it >= 0 } ?: episodeInfo?.sortNo?.let { sortNo ->
                adapter.currentList.indexOfFirst { it.sortNo == sortNo }
            } ?: -1
            if (position >= 0) {
                listController.startInitialPlay(position)
            }
        }
        dialog.show(supportFragmentManager)
    }

    // --- 生命周期 ---
    override fun onPause() {
        super.onPause();
        reportCurrentPlayback(completed = false)
        // 强制当前正在显示的 ViewHolder 上报进度
        val layoutManager = binding.recyclerView.layoutManager as? LinearLayoutManager
        val firstPos = layoutManager?.findFirstVisibleItemPosition() ?: -1
        if (firstPos != -1) {
            val holder = binding.recyclerView.findViewHolderForAdapterPosition(firstPos) as? VideoFeedAdapter.VideoViewHolder
            holder?.onDetached()
        }
        playerManager.pause()
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

    private fun trackPlayerView(episodeInfo: EpisodeInfo) {
        EventManager.onFirebaseTrack(
            FirebaseEvent.PLAYER_VIEW,
            FirebaseEventParam.DRAMA_ID to (episodeInfo.workId ?: mRealVM.workId),
            FirebaseEventParam.EPISODE_ID to episodeInfo.storyId,
            FirebaseEventParam.SOURCE to playerSource,
            FirebaseEventParam.BANNER_ID to playerBannerId
        )
    }

    private fun trackFirebasePlaybackProgress(episode: EpisodeInfo, progressPercent: Int) {
        if (progressPercent >= 10 && lastFirebaseValidPlayStoryId != episode.storyId) {
            lastFirebaseValidPlayStoryId = episode.storyId
            EventManager.onFirebaseTrack(
                FirebaseEvent.PLAYER_VALID_PLAY,
                FirebaseEventParam.DRAMA_ID to (episode.workId ?: mRealVM.workId),
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
            lastFirebaseProcessStoryId = episode.storyId
            lastFirebaseProcessNodes.clear()
        }
        if (lastFirebaseProcessNodes.add(processNode)) {
            EventManager.onFirebaseTrack(
                FirebaseEvent.PLAYER_PROCESS,
                FirebaseEventParam.DRAMA_ID to (episode.workId ?: mRealVM.workId),
                FirebaseEventParam.EPISODE_ID to episode.storyId,
                FirebaseEventParam.PROCESS to processNode
            )
        }
    }
}
