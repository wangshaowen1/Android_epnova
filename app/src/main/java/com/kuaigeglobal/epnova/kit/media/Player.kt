@file:JvmName("PlayerUtils")

package com.kuaigeglobal.epnova.kit.media

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo
import com.kuaigeglobal.epnova.kit.media.source.MediaSourceManager
import com.nhsd.basic.utils.Logger


/**
 * 获取或创建与当前播放器绑定的 MediaSourceManager
 * 这会初始化 Manager 并绑定 Player，如果需要重置数据，后续需链式调用 setInitialMedias
 */
fun PlaylistPlayer.setupMediaSourceManager(initialMediasIsCache: Boolean = false): MediaSourceManager {
    return MediaSourceManager.with(this, initialMediasIsCache)
}

/**
 * 获取当前播放器绑定的 MediaSourceManager (如果存在)
 * 不会创建新实例，也不会重置状态
 */
fun PlaylistPlayer.getMediaSourceManager(): MediaSourceManager? {
    return MediaSourceManager.get(this)
}
fun PlaylistPlayer.resumePlay(tracks: List<WorkInfo?>?) {
    if (tracks.isNullOrEmpty()) {
        return
    }
    playlist = tracks
    play()
}

fun PlaylistPlayer.resumePlay(
    tracks: List<WorkInfo>?,
    playPosition: Int,
    positionMs: Long? = null
) {

    // 确定使用的播放列表
    val currentPlaylist = tracks ?: playlist ?: return // 如果播放列表为空，则直接返回
    // 检查播放位置是否有效
    if (playPosition < 0 || currentPlaylist.size <= playPosition) {
        return // 如果播放位置无效，直接返回
    }
    // 检查当前播放的曲目是否与指定位置的曲目相同
    val isSameTrack =
        currentTrack()?.equalsWithMedias(currentPlaylist.getOrNull(playPosition)) ?: false
    // 检查是否需要更新播放列表
    val playlistUpdated = playlist != currentPlaylist
    if (playlistUpdated) {
        playlist = tracks // 更新播放列表
    }
    // 跳转到指定的播放位置
    if (getCurrentPlayingIndex() != playPosition || playlistUpdated || !isSameTrack) {
        skipTo(playPosition)
    } else if (!isPlaying) {
        // 如果播放器不在播放，开始播放
        play()
    }

    if (playlistUpdated || !isSameTrack) {
        positionMs?.takeIf { it>0 }?.let {
            // 定位到指定的时间戳
            seekTo(it)
        }

    }


}

fun Player.play(url: String?) {
    setPlayUrl(url)
    prepare()
    play()
}

fun Player.setPlayUrl(url: String?) {
    val mediaItem = MediaItem.Builder().setUri(url).build()
    setMediaItem(mediaItem)
}


fun PlaylistPlayer.resumePlay(
    playPosition: Int,
    positionMs: Long? = null
) {
    resumePlay(null, playPosition, positionMs)
}

fun PlaylistPlayer.resumePlay(tracks: List<WorkInfo>?, played: WorkInfo?) {
    if (tracks.isNullOrEmpty() || played == null) {
        return
    }
    resumePlay(tracks, tracks.indexOf(played))
}

val globalAnimPlayer by lazy {
    PlayerFactory.createPlayer(true).apply {
        repeatMode = ExoPlayer.REPEAT_MODE_ONE
        volume = 0f
    }
}

fun ExoPlayer.setPlayUrl(url: String) {
    val mediaItem = MediaItem.Builder().setUri(url).build()
    setMediaItem(mediaItem)
}

fun ExoPlayer.startPlay(url: String) {
    setPlayUrl(url)
    prepare()
    play()
}

//fun pauseGlobalPlayer() {
//    globalTrackListPlayer.pause()
//}
//
//fun DefaultPlaylistPlayer.resumePlay(track: TrackInfo?) {
//    playlist?.indexOf(track)?.let { resumePlay(it) }
//}


fun TrackPlayer.play(track: WorkInfo?, skipIntro: Boolean = true): Boolean {
    Logger.i("play track:${track?.stripedTrickTitle} current =" + this.currentTrack()?.stripedTrickTitle)
    setTrack(track)
    if (track != null) {
        if (!isPlaying) {
            val isPaused = playbackState == Player.STATE_READY
            if (isPaused) {
                play()
            } else {
                playWhenReady = true
                prepare()
                return true
            }
        }
    } else if (isPlaying) {
        stop()
    }
    return false
}

