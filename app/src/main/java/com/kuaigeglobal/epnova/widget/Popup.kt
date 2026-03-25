package com.kuaigeglobal.epnova.widget

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.animation.addListener
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.common.ext.updateLayoutParams

class Popup(private val view: IPopupView, private val cancelable: Boolean = true) {

    private val decorView
        get() = topActivity?.window?.decorView as? ViewGroup
    private var animator: Animator? = null

    init {
        view.root.setOnClickListener { dismiss() }
    }

    fun showUnder(anchor: View) {
        (view.root.parent as? ViewGroup)?.removeView(view.root)
        decorView?.let { decor ->
            decor.addView(view.root)
            val rect = Rect()
            anchor.getGlobalVisibleRect(rect)
            view.popup.updateLayoutParams<MarginLayoutParams> { it.topMargin = rect.bottom }
            show()
        }
    }

    fun showAlignTop(anchor: View) {
        (view.root.parent as? ViewGroup)?.removeView(view.root)
        decorView?.let { decor ->
            decor.addView(view.root)
            val rect = Rect()
            anchor.getGlobalVisibleRect(rect)
            view.popup.updateLayoutParams<MarginLayoutParams> { it.topMargin = rect.top }
            show()
        }
    }

    fun showNow() {
        (view.root.parent as? ViewGroup)?.removeView(view.root)
        decorView?.let { decor ->
            decor.addView(view.root)
            show()
        }
    }

    private fun show() {
        animator?.cancel()
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 150
            addUpdateListener { updateAnim(it.animatedValue as Float) }
            animator = this
            start()
        }
        view.onShow()
    }

    fun dismiss() {
        if (!cancelable) {
            return
        }
        animator?.cancel()
        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 150
            addUpdateListener { updateAnim(it.animatedValue as Float) }
            addListener(onEnd = { (view.root.parent as? ViewGroup)?.removeView(view.root) })
            animator = this
            start()
        }
        view.onDismiss()
    }

    private fun updateAnim(value: Float) {
        if (!view.updateView(value)) {
            //默认动画
            view.bg.alpha = value
            view.popup.pivotX = view.popup.width.toFloat()
            view.popup.scaleX = value
            view.popup.scaleY = value
        }
    }
}

interface IPopupView {
    val root: View
    val bg: View
    val popup: View
    fun updateView(animatedValue: Float): Boolean = false
    fun onShow() {}
    fun onDismiss() {}
}