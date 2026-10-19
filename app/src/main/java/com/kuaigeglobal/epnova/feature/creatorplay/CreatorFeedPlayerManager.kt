package com.kuaigeglobal.epnova.feature.creatorplay

import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorPlayableEpisode
import com.kuaigeglobal.epnova.kit.media.PlayerFactory
import com.nhsd.basic.ext.getResString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 新创作播放器的 ExoPlayer 管理器。
 *
 * 只处理播放、暂停、loading、进度和完播事件，不接旧内容播放器的解锁/互动状态。
 */
class CreatorFeedPlayerManager(
    private val scope: CoroutineScope
) {

    /**Creator 播放器独立持有 ExoPlayer，避免复用旧播放器 manager 的内容解锁状态。*/
    private val exoPlayer: ExoPlayer by lazy {
        PlayerFactory.createPlayer(withCache = true).apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
        }
    }

    /**当前绑定的 PlayerView；切换 item 时需要先解绑旧 View，防止复用串画面。*/
    private var currentPlayerView: PlayerView? = null

    /**当前播放单元的稳定 key，用于避免同一视频重复 prepare。*/
    var currentPlayingKey: String? = null
        private set

    /**用户是否主动暂停；前后台切换时需要尊重用户暂停意愿。*/
    private var isUserPaused = false

    /**拖动进度条期间不使用播放器回调覆盖 UI 上的临时拖动位置。*/
    private var isSeeking = false

    /**进度轮询任务，只在实际播放中运行。*/
    private var progressJob: Job? = null

    /**播放进度，Pair.first 是当前位置毫秒，Pair.second 是总时长毫秒。*/
    private val _progressFlow = MutableStateFlow(0L to 0L)
    val progressFlow = _progressFlow.asStateFlow()

    /**播放状态流，Adapter 根据该状态显示 loading、暂停按钮和错误提示。*/
    private val _playerState = MutableStateFlow<CreatorPlayerState>(CreatorPlayerState.Idle)
    val playerState = _playerState.asStateFlow()

    /**一次性播放器事件，目前只需要完播自动下一条。*/
    private val _playerEvent = MutableSharedFlow<PlayerEvent>()
    val playerEvent = _playerEvent.asSharedFlow()

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> _playerState.value = CreatorPlayerState.Buffering
                    Player.STATE_READY -> {
                        _playerState.value = if (exoPlayer.playWhenReady) {
                            CreatorPlayerState.Playing
                        } else {
                            CreatorPlayerState.Pause
                        }
                    }
                    Player.STATE_ENDED -> {
                        if (isRealEnded()) {
                            scope.launch { _playerEvent.emit(PlayerEvent.OnPlayEnded) }
                        }
                        _playerState.value = CreatorPlayerState.Pause
                        progressJob?.cancel()
                    }
                    Player.STATE_IDLE -> Unit
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    _playerState.value = CreatorPlayerState.Playing
                    startProgressUpdater()
                } else {
                    progressJob?.cancel()
                    if (exoPlayer.playbackState == Player.STATE_READY && !exoPlayer.playWhenReady) {
                        _playerState.value = CreatorPlayerState.Pause
                    }
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (exoPlayer.playbackState == Player.STATE_READY) {
                    _playerState.value = if (playWhenReady) {
                        CreatorPlayerState.Playing
                    } else {
                        CreatorPlayerState.Pause
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _playerState.value = CreatorPlayerState.Error(error.localizedMessage.orEmpty())
            }
        })
    }

    /**绑定当前 item 并开始播放；只消费 CreatorPlayableEpisode，不接受旧剧集模型。*/
    fun attachAndPlay(
        item: CreatorPlayableEpisode,
        playerView: PlayerView,
        startPositionMs: Long = 0L
    ) {
        val videoUrl = item.episode.videoUrl
        if (videoUrl.isNullOrEmpty()) {
            _playerState.value = CreatorPlayerState.Error(R.string.loading_failed.getResString())
            return
        }

        val mediaKey = item.feedKey ?: item.episode.storyId ?: item.workContext.workId ?: videoUrl
        currentPlayingKey = mediaKey

        if (exoPlayer.currentMediaItem?.mediaId == mediaKey &&
            exoPlayer.currentMediaItem?.localConfiguration?.uri?.toString() == videoUrl &&
            currentPlayerView == playerView
        ) {
            if (startPositionMs > 0L) {
                exoPlayer.seekTo(startPositionMs)
            }
            if (!exoPlayer.isPlaying && !isUserPaused) {
                exoPlayer.play()
            }
            return
        }

        _playerState.value = CreatorPlayerState.Idle
        isSeeking = false
        isUserPaused = false
        currentPlayerView?.player = null
        currentPlayerView = playerView
        playerView.player = exoPlayer

        val mediaItem = MediaItem.Builder()
            .setUri(videoUrl)
            .setMediaId(mediaKey)
            .build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        if (startPositionMs > 0L) {
            exoPlayer.seekTo(startPositionMs)
        }
        exoPlayer.play()
    }

    /**点击播放器时切换播放/暂停。*/
    fun togglePlay() {
        if (exoPlayer.isPlaying) {
            isUserPaused = true
            pause()
        } else {
            isUserPaused = false
            resume()
        }
    }

    /**暂停播放。*/
    fun pause() {
        exoPlayer.pause()
    }

    /**页面回到前台时恢复播放，但不打断用户主动暂停。*/
    fun resumeOnForeground() {
        if (!isUserPaused) {
            resume()
        }
    }

    /**恢复当前媒体；如果已经完播则从头开始。*/
    fun resume() {
        if (exoPlayer.playbackState == Player.STATE_ENDED) {
            exoPlayer.seekTo(0L)
            exoPlayer.prepare()
        } else if (exoPlayer.playbackState == Player.STATE_IDLE) {
            exoPlayer.prepare()
        }
        exoPlayer.play()
    }

    /**拖动进度条后跳转到指定毫秒。*/
    fun seekTo(positionMs: Long) {
        isSeeking = true
        exoPlayer.seekTo(positionMs)
        _playerState.value = CreatorPlayerState.Buffering
        val duration = if (exoPlayer.duration > 0L) exoPlayer.duration else 0L
        if (duration > 0L) {
            _progressFlow.value = positionMs to duration
        }
        scope.launch {
            delay(800L)
            isSeeking = false
        }
    }

    /**释放 ExoPlayer 和绑定 View，Activity 销毁时必须调用。*/
    fun release() {
        progressJob?.cancel()
        currentPlayerView?.player = null
        currentPlayerView = null
        exoPlayer.release()
    }

    /**判断是否真的播到末尾，避免网络抖动导致 STATE_ENDED 误触发自动下一条。*/
    private fun isRealEnded(): Boolean {
        val duration = exoPlayer.duration
        val current = exoPlayer.currentPosition
        return duration > 0L && current > duration * 0.9
    }

    /**播放中每 200ms 推一次进度，用于底部进度条。*/
    private fun startProgressUpdater() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying && !isSeeking) {
                    val duration = if (exoPlayer.duration > 0L) exoPlayer.duration else 0L
                    _progressFlow.emit(exoPlayer.currentPosition to duration)
                }
                delay(200L)
            }
        }
    }

    /**播放器一次性事件。*/
    sealed class PlayerEvent {
        /**当前播放单元完播，Activity/Controller 负责滚到下一条。*/
        object OnPlayEnded : PlayerEvent()
    }

    /**Creator 播放器 UI 状态，不包含旧内容播放器的解锁、点赞、收藏状态。*/
    sealed class CreatorPlayerState {
        /**尚未开始播放。*/
        object Idle : CreatorPlayerState()

        /**正在缓冲或 seek。*/
        object Buffering : CreatorPlayerState()

        /**正在播放。*/
        object Playing : CreatorPlayerState()

        /**已暂停。*/
        object Pause : CreatorPlayerState()

        /**播放失败，msg 只用于当前 item 的轻量错误提示。*/
        data class Error(val msg: String) : CreatorPlayerState()
    }
}
