package com.kuaigeglobal.epnova.widget

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class ToastView : AppCompatTextView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var showAnimator: Animator? = null
    private var hideAnimator: Animator? = null
    private val hideRunnable = Runnable { hide() }
    private var toast = ""
        set(value) {
            field = value
            text = value
            if (value.isEmpty()) {
                removeCallbacks(hideRunnable)
                hideAnimator?.cancel()
                alpha = 0f
            } else {
                show()
            }
        }

    fun showToast(text: String) {
        this.toast = text
    }

    private fun show() {
        if (showAnimator == null) {
            hideAnimator?.cancel()
            hideAnimator = null
            showAnimator = ObjectAnimator.ofFloat(this, "alpha", alpha, 1f).apply {
                duration = 300
                start()
            }
        }
        removeCallbacks(hideRunnable)
        postDelayed(hideRunnable, 2000)
    }

    private fun hide() {
        if (hideAnimator == null) {
            showAnimator?.cancel()
            showAnimator = null
            hideAnimator = ObjectAnimator.ofFloat(this, "alpha", alpha, 0f).apply {
                duration = 300
                start()
            }
        }
    }
}