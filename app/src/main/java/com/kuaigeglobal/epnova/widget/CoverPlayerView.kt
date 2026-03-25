package com.kuaigeglobal.epnova.widget

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo
import com.nhsd.basic.utils.Logger


class CoverPlayerView : PlayerView, Player.Listener {
    private var restorePlayer: Player? = null
    private var alphaAnimator: ObjectAnimator? = null
    private var coverImage: AppCompatImageView? = null
    val scaleTypeArray: Array<ImageView.ScaleType> = arrayOf(
        ImageView.ScaleType.MATRIX,
        ImageView.ScaleType.FIT_XY,
        ImageView.ScaleType.FIT_START,
        ImageView.ScaleType.FIT_CENTER,
        ImageView.ScaleType.FIT_END,
        ImageView.ScaleType.CENTER,
        ImageView.ScaleType.CENTER_CROP,
        ImageView.ScaleType.CENTER_INSIDE
    )

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        attrs?.let { initCover(context, it) }
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        attrs?.let { initCover(context, it) }
    }

    private fun initCover(context: Context, attrs: AttributeSet) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.CoverPlayerView)
        val scaleType = a.getInt(R.styleable.CoverPlayerView_coverScaleType, 3)
        coverImage = AppCompatImageView(context)
        coverImage?.scaleType = scaleTypeArray[scaleType]
        coverImage?.layoutParams =
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(coverImage)
        a.recycle()
    }


    private fun startHideCoverAnim() {
        if (isCoverVisible() && player != null) {
            Logger.i("CoverPlayerView  hide image with amin=" + workInfo?.stripedTrickTitle)
            if (alphaAnimator?.isRunning == true) {
                return
            }
            alphaAnimator?.removeAllListeners()
            // 使用 ObjectAnimator 来实现 alpha 动画
            alphaAnimator = createAnimation()
            alphaAnimator?.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    // 动画开始时的操作
                }

                override fun onAnimationEnd(animation: Animator) {
                    // 动画结束时，隐藏 coverImage 并重置其 alpha 值
                    coverImage?.visibility = GONE
                    coverImage?.alpha = 1f
                    alphaAnimator = null
                }

                override fun onAnimationCancel(animation: Animator) {
                    // 动画取消时的操作
                }

                override fun onAnimationRepeat(animation: Animator) {
                    // 动画重复时的操作
                }
            })
            alphaAnimator?.start()
        }
    }

    private fun createAnimation(): ObjectAnimator {
        return ObjectAnimator.ofFloat(coverImage, "alpha", 1f, 0f).apply {
            duration = 350
            interpolator = AccelerateInterpolator()
        }

    }

    private fun isCoverVisible() = coverImage?.visibility == VISIBLE && coverImage?.alpha == 1f


    private fun resetCoverImageVisible() {
        Logger.i("CoverPlayerView reset cover visible.................")
        showCoverImage()
        alphaAnimator?.cancel()
        alphaAnimator = null
    }

    private fun showCoverImage() {
        coverImage?.apply {
            visibility = VISIBLE
            alpha = 1f
        }
    }


    fun setCoverHeight(height: Int) {
        coverImage?.let {
            it.layoutParams.height = height
            it.requestLayout()  // 通知界面更新
        }
    }

    fun getCoverImage() = coverImage

    var workInfo: WorkInfo? = null

    fun setTrack(track: WorkInfo?) {
        workInfo = track
    }


    override fun setPlayer(player: Player?) {
        restorePlayer?.removeListener(this)
        this.restorePlayer = player
        player?.addListener(this)
        Logger.i("CoverPlayerView_____________ player changed to $player, current track:${workInfo?.workTitle}, id: ${workInfo?.workId}")
        if (isAttached) {
            super.setPlayer(player)
            // 添加监听器到新播放器
        }

        // 根据播放器状态更新封面图片的可见性
        if (player?.isPlaying == true) {
            if (player.currentPosition != 0L) {
                coverImage?.visibility = GONE
                Logger.i("CoverPlayerView  hide image no anim for playing video")
            } else {
                startHideCoverAnim()
            }
        } else {
            Logger.i("CoverPlayerView reset cover visible due to player not playing or changed")
            resetCoverImageVisible()
        }
    }


    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)

        when (playbackState) {
            Player.STATE_READY -> {

            }

            Player.STATE_BUFFERING, Player.STATE_ENDED, Player.STATE_IDLE -> {
                Logger.i(
                    App.TAG,
                    "CoverPlayerView video player  state=${playbackState} " + player?.currentPosition + "," + workInfo?.stripedTrickTitle + ",cover visible=${coverImage?.visibility == VISIBLE}"
                )

                if (player?.currentPosition == 0L && coverImage?.visibility != VISIBLE) {
                    Logger.i("CoverPlayerView reset cover visible 3")
                    resetCoverImageVisible()
                }
            }

        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        Logger.i("CoverPlayerView onIsPlayingChanged is playing=$isPlaying")
        if (isPlaying) {
            Logger.i("CoverPlayerView isPlayingChange hide cover")
            startHideCoverAnim()
        } else {
            if (player?.playbackState != ExoPlayer.STATE_READY && player?.playbackState != ExoPlayer.STATE_BUFFERING && player?.isPlaying != true) {
                Logger.i("CoverPlayerView show cover")

                resetCoverImageVisible()
            }

        }
    }

    var isAttached = false

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isAttached = true
        restorePlayer?.addListener(this)
        super.setPlayer(restorePlayer)
        Logger.i(App.TAG, "CoverPlayerView onattatch " + workInfo?.stripedTrickTitle)
//        globalMvPlayer.addVideoErrorListener(this)
//        globalMvPlayer.addSurfaceTextureListener(this)

    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isAttached = false
        releasePlayer()

        Logger.i(App.TAG, "CoverPlayerView onDetachedFromWindow  " + workInfo?.stripedTrickTitle)
//        globalMvPlayer.removeVideoErrorListener(this)
//        globalMvPlayer.removeSurfaceTextureListener(this)
//        Logger.i("CoverPlayerView reset cover visible 4")
//        resetCoverImageVisible()
    }


    fun releasePlayer() {
        Logger.i(
            "CoverPlayerView_____________ release player........" + workInfo?.stripedTrickTitle + "isCommandAvailable........" + (this.player?.isCommandAvailable(
                Player.COMMAND_SET_VIDEO_SURFACE
            ))
        )
        restorePlayer?.removeListener(this)
        alphaAnimator?.cancel()
        alphaAnimator?.removeAllListeners()  // 释放动画监听器
        alphaAnimator = null
        coverImage?.alpha = 1f
        super.setPlayer(null)
    }

    fun releaseCover() {
        removeView(coverImage)
    }

    companion object {
        const val DEFAULT_VIDEO_HEIGHT = 1280
        const val DEFAULT_VIDEO_WIDTH = 720
    }
}
