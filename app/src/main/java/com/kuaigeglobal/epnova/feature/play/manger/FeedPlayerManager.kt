package com.kuaigeglobal.epnova.feature.play.manger

import android.content.Context
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.exoplayer.ExoPlayer
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.kit.media.PlayerFactory
import com.kuaigeglobal.epnova.widget.CoverPlayerView
import com.nhsd.basic.ext.getResString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FeedPlayerManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val exoPlayer: ExoPlayer by lazy {
        PlayerFactory.createPlayer(withCache = true).apply {
            repeatMode = Player.REPEAT_MODE_OFF // 由业务层控制循环
            playWhenReady = true
        }
    }

    private var currentCoverPlayerView: CoverPlayerView? = null

    // [新] 记录当前播放的 ID，用于防止重复初始化
    var currentPlayingId: String? = null
        private set

    // [新] 记录用户是否主动暂停
    private var isUserPaused = false

    // 状态流
    private val _progressFlow = MutableStateFlow(Pair(0L, 0L))
    val progressFlow = _progressFlow.asStateFlow()

    // [修改] 替换原有的 boolean flow，使用更丰富的状态流
    private val _playerState = MutableStateFlow<FeedPlayerState>(FeedPlayerState.Idle)
    val playerState = _playerState.asStateFlow()

    sealed class PlayerEvent {
        object OnPlayEnded : PlayerEvent()
    }

    private val _playerEvent = MutableSharedFlow<PlayerEvent>()
    val playerEvent = _playerEvent.asSharedFlow()

    private var isSeeking = false
    private var progressJob: Job? = null

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> {
                        _playerState.value = FeedPlayerState.Buffering
                    }
                    Player.STATE_READY -> {
                        // [关键修改]：只要准备好了，且用户没点暂停，就立刻设为 Playing
                        if (exoPlayer.playWhenReady) {
                            _playerState.value = FeedPlayerState.Playing
                        } else {
                            _playerState.value = FeedPlayerState.Pause
                        }
                    }
                    Player.STATE_ENDED -> {
                        // 只有当播放进度接近总时长时，才认为是真正的完播
                        val duration = exoPlayer.duration
                        val current = exoPlayer.currentPosition
                        if (duration > 0 && current > duration * 0.9) {
                            scope.launch { _playerEvent.emit(PlayerEvent.OnPlayEnded) }
                        }
                        _playerState.value = FeedPlayerState.Pause
                        progressJob?.cancel()
                    }

                    Player.STATE_IDLE -> {
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    // 只要 isPlaying 为 true，说明视频已经在动了，必须隐藏 loading
                    // 这能解决缓存命中时，STATE_READY 没能及时更新 UI 的问题
                    _playerState.value = FeedPlayerState.Playing
                    startProgressUpdater()
                } else {
                    progressJob?.cancel()
                    if (exoPlayer.playbackState == Player.STATE_READY && !exoPlayer.playWhenReady) {
                        _playerState.value = FeedPlayerState.Pause
                    }
                    // 注意：这里不要盲目设为 Pause，因为 isPlaying=false 可能是 Buffering
                }
            }

            // [修复2] 改用 playWhenReady 驱动 UI。
            // 只要 playWhenReady=true，就认为应该显示"播放中"状态(隐藏暂停图标)，忽略 Buffering 导致的 isPlaying=false
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (exoPlayer.playbackState == Player.STATE_READY) {
                    _playerState.value = if (playWhenReady) FeedPlayerState.Playing else FeedPlayerState.Pause
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val currentPos = exoPlayer.currentPosition
                val duration = exoPlayer.duration

                // 判断逻辑：
                // 如果当前进度很小（例如 < 500ms）或者从未加载出 Duration，说明是起步就挂了 -> 无缓存
                // 否则说明看了一半挂了 -> 有缓存，仅提示
                val isStartupError = currentPos < 500 && (duration <= 0 || currentPos < duration / 20)

                if (isStartupError) {
                    // 对应右图：全屏错误缺省页
                    _playerState.value = FeedPlayerState.Error(isNoCache = true, msg = R.string.toast_empty_network.getResString())
                } else {
                    // 对应左图逻辑延伸：画面保持（ExoPlayer特性），UI 保持 Buffering 或 Toast 提示
                    // 此时不切换成全屏 Error，以免用户丢失观看上下文
                    _playerState.value = FeedPlayerState.Error(isNoCache = false, msg = R.string.toast_empty_network.getResString())
                }
            }
        })
    }

    fun attachAndPlay(track: EpisodeInfo, coverPlayerView: CoverPlayerView) {
        currentPlayingId = track.storyId
        // 乐观更新状态，防止切视频闪烁
        _playerState.value = FeedPlayerState.Idle
        // 防止重复 attach 同一个视频导致画面闪烁
        if (exoPlayer.currentMediaItem?.mediaId == track.storyId && currentCoverPlayerView == coverPlayerView) {
            if (!exoPlayer.isPlaying && !isUserPaused) {
                exoPlayer.play()
            }
            return
        }

        isSeeking = false
        isUserPaused = false // 切视频重置暂停状态

        // 解绑旧 View
        if (currentCoverPlayerView != null && currentCoverPlayerView != coverPlayerView) {
            currentCoverPlayerView?.setPlayer(null)
        }
        currentCoverPlayerView = coverPlayerView

        // 绑定新 View
        coverPlayerView.setTrack(track)
        coverPlayerView.setPlayer(exoPlayer)

        val videoUrl = track.videoUrl ?: return

        // [拓展] 构建带字幕的 MediaItem
        val mediaItem = buildMediaItem(track, videoUrl)

        val currentUri = exoPlayer.currentMediaItem?.localConfiguration?.uri.toString()
        val currentMediaId = exoPlayer.currentMediaItem?.mediaId
        if (currentUri != videoUrl || currentMediaId != track.storyId) {
            // 1. 如果是不同的视频，正常加载
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        } else {
            // 2. [关键修复] 如果是同一个视频（URL相同），检查是否处于播放结束状态
            // 如果上次播完了停在 ENDED，直接 play() 可能无法从头播，或者会瞬间触发 ENDED 事件
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.seekTo(0)
                exoPlayer.prepare()
            }
            // 3. 如果是从错误状态恢复，也建议重置一下
            if (exoPlayer.playerError != null) {
                exoPlayer.prepare()
            }
        }
        //在 prepare 之后，play 之前，才更新 ID
        // 这样可以确保之前的任何回调（如 stop 触发的）拿到的都是旧 ID
        currentPlayingId = track.storyId
        exoPlayer.play()
    }

    /**
     * [拓展点] 字幕/多音轨构建逻辑
     */
    private fun buildMediaItem(track: EpisodeInfo, url: String): MediaItem {
        val builder = MediaItem.Builder().setUri(url).setMediaId(track.storyId?:"")

        // 示例：如果未来 TrackInfo 有 subtitleUrl 字段
        // track.subtitleUrl?.let { subUrl ->
        //     val subtitleConfig = SubtitleConfiguration.Builder(Uri.parse(subUrl))
        //         .setMimeType(MimeTypes.APPLICATION_SUBRIP) // .srt
        //         .setLanguage("zh")
        //         .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
        //         .build()
        //     builder.setSubtitleConfigurations(listOf(subtitleConfig))
        // }

        return builder.build()
    }

    fun seekTo(position: Long) {
        isSeeking = true
        exoPlayer.seekTo(position)
        // 拖动时临时切为 Buffering 状态
        _playerState.value = FeedPlayerState.Buffering
        // 立即欺骗 UI 更新
        val validDuration = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
        if (validDuration > 0) _progressFlow.value = Pair(position, validDuration)

        scope.launch {
            delay(800)
            isSeeking = false
            // 恢复后如果是暂停状态，手动触发一次更新确保位置正确
//            if (!exoPlayer.isPlaying) {
//                val current = exoPlayer.currentPosition
//                val duration = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
//                _progressFlow.emit(Pair(current, duration))
//            }
        }
    }

    fun togglePlay() {
        if (exoPlayer.isPlaying) {
            isUserPaused = true
            pause()
        } else {
            isUserPaused = false
            resume()
        }
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun resume() {
        // 1. 如果是播放结束，从头播
        if (exoPlayer.playbackState == Player.STATE_ENDED) {
            replay()
            return
        }

        // 2. 错误恢复逻辑
        // 注意：is FeedPlayerState.Error 判断非常重要，防止 UI 是 Error 但 ExoPlayer 状态未同步
        if (exoPlayer.playerError != null || _playerState.value is FeedPlayerState.Error) {
            exoPlayer.prepare()
        }

        // 3. 播放
        // 只要不是 IDLE 状态，play() 通常有效；如果是 IDLE (被释放或严重错误)，需要 prepare
        if (exoPlayer.playbackState == Player.STATE_IDLE) {
            exoPlayer.prepare()
        }
        exoPlayer.play()
    }

    // 给 Activity onResume 用，遵守用户暂停意愿
    fun resumeOnForeground() {
        if (!isUserPaused) {
            resume()
        }
    }

    fun replay() {
        isSeeking = false
        isUserPaused = false
        exoPlayer.seekTo(0)
        exoPlayer.prepare() // 确保错误后能重置
        exoPlayer.play()
    }

    @OptIn(UnstableApi::class)
    fun preload(url: String?) {
        if (url.isNullOrEmpty()) return
        scope.launch(Dispatchers.IO) {
            try {
                val dataSpec = DataSpec(url.toUri(), 0, 1024 * 1024, null)
                val dataSource = PlayerFactory.cacheDataSourceFactory.createDataSource()
                dataSource.open(dataSpec)
                dataSource.close()
            } catch (e: Exception) {
            }
        }
    }

    private fun startProgressUpdater() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying && !isSeeking) {
                    val current = exoPlayer.currentPosition
                    val duration = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
                    _progressFlow.emit(Pair(current, duration))
                }
                delay(200)
            }
        }
    }

    fun release() {
        progressJob?.cancel()
        currentCoverPlayerView?.setPlayer(null)
        currentCoverPlayerView = null
        exoPlayer.release()
    }

    // [新增] UI 状态定义
    sealed class FeedPlayerState {
        object Idle : FeedPlayerState()
        object Buffering : FeedPlayerState() // 对应左图：画面停留在最后一帧 + 转圈
        object Playing : FeedPlayerState()
        object Pause : FeedPlayerState()

        // isNoCache = true 对应右图：全屏错误页； false = 仅 Toast 提示
        data class Error(val isNoCache: Boolean, val msg: String) : FeedPlayerState()
    }
}