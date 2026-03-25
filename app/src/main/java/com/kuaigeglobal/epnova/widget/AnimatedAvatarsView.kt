package com.kuaigeglobal.epnova.widget

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.core.animation.addListener
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.core.databinding.loadThumbnailUri
import com.kuaigeglobal.epnova.databinding.LayoutAnimatedAvatarsBinding
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResDrawable

class AnimatedAvatarsView : FrameLayout {

    private val binding by lazy { LayoutAnimatedAvatarsBinding.inflate(LayoutInflater.from(context), this, true) }
    private val avatarViews by lazy { listOf(binding.ivAvatar1, binding.ivAvatar2, binding.ivAvatar3) }
    private val animateNext by lazy { Runnable { animateNext() } }
    private val animateDistance by lazy { 10.dp  }
    private var attached = false
        set(value) {
            if (field != value) {
                field = value
                reset()
            }
        }
    private var animator: Animator? = null
    private var index = 0
    var avatars: List<String?>? = if (isInEditMode) listOf("", "", "", "") else null
        set(value) {
            field = value
            reset()
        }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        binding
    }

    private fun reset() {
        animator?.cancel()
        removeCallbacks(animateNext)
        setCurrent(0)
    }

    private fun setCurrent(index: Int) {
        this.index = index
        val avatars = avatars
        val size = avatars?.size
        if (size == null || size == 0) {
            visibility = GONE
            return
        }
        visibility = VISIBLE
        avatarViews.forEachIndexed { idx, avatarView ->
            avatarView.translationX = 0f
            avatarView.alpha = 1f
            if (idx < size) {
                val avatarIndex = (this.index + idx) % size
                avatarView.visibility = VISIBLE
                loadThumbnailUri(avatarView, avatars[avatarIndex], R.drawable.ic_default_avatar_big.getResDrawable())
            } else {
                avatarView.visibility = GONE
            }
        }
        binding.ivNext.alpha = 0f
        if (size > avatarViews.size && attached) {
            val next = (index + 3) % size
            loadThumbnailUri(binding.ivNext, avatars[next], R.drawable.ic_default_avatar_big.getResDrawable())
            postDelayed(animateNext, 1000)
        }
    }

    private fun animateNext() {
        animator?.cancel()
        ValueAnimator.ofFloat(0f, 1f).apply {
            interpolator = LinearInterpolator()
            duration = 500
            addUpdateListener {
                val animatedValue = it.animatedValue as Float
                binding.ivNext.alpha = animatedValue
                avatarViews.forEachIndexed { idx, avatarView ->
                    avatarView.translationX = animatedValue * animateDistance
                }
                avatarViews.first().alpha = 1f - animatedValue
            }
            addListener(onEnd = { setCurrent(index + 1) })
        }.let {
            animator = it
            it.start()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attached = true
    }

    override fun onDetachedFromWindow() {
        attached = false
        super.onDetachedFromWindow()
    }

}
