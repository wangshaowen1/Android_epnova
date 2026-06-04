package com.kuaigeglobal.epnova.feature.creatorplay

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlaySource
import com.kuaigeglobal.epnova.databinding.ActivityCreatorPlayerBinding
import com.nhsd.basic.BaseViewModelActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 新创作播放器 Activity。
 *
 * 阶段四只搭独立播放器骨架；TopEarners/MyCreations/作品详情入口在后续阶段接入。
 */
class CreatorPlayerActivity :
    BaseViewModelActivity<ActivityCreatorPlayerBinding, CreatorPlayerViewModel>(),
    CreatorVideoFeedAdapter.InteractionListener {

    companion object {
        /**播放来源 extra，使用 CreatorPlaySource.name 存储，避免 Creator 枚举跨模块序列化差异。*/
        private const val EXTRA_SOURCE = "creator_player_source"

        /**初始播放位置 extra。*/
        private const val EXTRA_POSITION = "creator_player_position"

        /**启动新创作播放器；阶段五会从 TopEarners item 点击入口调用。*/
        fun start(source: CreatorPlaySource, position: Int = 0) {
            val context = topActivity ?: return
            context.startActivity(
                Intent(context, CreatorPlayerActivity::class.java).apply {
                    putExtra(EXTRA_SOURCE, source.name)
                    putExtra(EXTRA_POSITION, position)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRealVM.playSource = readPlaySource()
        mRealVM.initialPosition = intent.getIntExtra(EXTRA_POSITION, 0)
        pendingInitialPosition = mRealVM.initialPosition

        initPlayerView()
        observePlayer()
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

    /**初始化播放器 UI、adapter 和列表控制器。*/
    private fun initPlayerView() {
        playerManager = CreatorFeedPlayerManager(lifecycleScope)
        binding.recyclerView.adapter = adapter
        listController = CreatorFeedListController(binding.recyclerView, adapter, playerManager).apply {
            onPageChanged = { item, _ ->
                mRealVM.currentPlayableEpisode.set(item)
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
            }
        }
        lifecycleScope.launch {
            playerManager.playerEvent.collectLatest { event ->
                if (event is CreatorFeedPlayerManager.PlayerEvent.OnPlayEnded) {
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

    /**解析入口 source；未知值按 TopEarners 处理，避免播放器无 source 崩溃。*/
    private fun readPlaySource(): CreatorPlaySource {
        val sourceName = intent.getStringExtra(EXTRA_SOURCE)
        return runCatching { CreatorPlaySource.valueOf(sourceName ?: CreatorPlaySource.TOP_EARNERS.name) }
            .getOrDefault(CreatorPlaySource.TOP_EARNERS)
    }
}
