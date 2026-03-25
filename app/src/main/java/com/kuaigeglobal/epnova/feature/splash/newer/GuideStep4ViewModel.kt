package com.kuaigeglobal.epnova.feature.splash.newer

import androidx.databinding.ObservableBoolean
import androidx.media3.common.Player
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.network.apis.entity.ExampleWorkInfo
import com.kuaigeglobal.epnova.kit.media.PlayerFactory
import com.kuaigeglobal.epnova.kit.media.play
import com.nhsd.basic.ext.getParcelableValue

class GuideStep4ViewModel : AppRepositoryViewModel<AppRepository>() {

    val example by lazy { mBundle.getParcelableValue(Constants.Extra.DATA, ExampleWorkInfo::class.java) }
    val playing by lazy { ObservableBoolean(true) }

    val player by lazy {
        PlayerFactory.createPlayer(true, autoAudioFocus = true).apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        seekTo(1)
                        playWhenReady = false
                        prepare()
                    }
                }
            })
        }
    }

    override fun onCreate() {
        super.onCreate()
        play()
    }

    fun play() {
        if (player.isPlaying) {
            player.pause()
            playing.set(false)
        } else {
            when (player.playbackState) {
                Player.STATE_IDLE -> {
                    val videoUrl = example?.videoUrl
                    if (videoUrl.isNullOrEmpty().not()) {
                        player.play(videoUrl)
                        playing.set(true)
                    }
                }

                Player.STATE_ENDED -> {
                    player.seekTo(0)
                    player.play()
                    playing.set(true)
                }

                else -> {
                    player.play()
                    playing.set(true)
                }
            }
        }
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }

}