package com.kuaigeglobal.epnova.widget

import android.animation.ValueAnimator
import android.view.View

/** 只拥有缩放动画，不取消调用方在同一 View 上的位移或透明度动画。 */
internal class PressScaleEffect(private val view: View) {
    private var animator: ValueAnimator? = null
    private var pressed = false
    private var active = false
    private var baseX = 1f
    private var baseY = 1f

    fun update(pressed: Boolean) {
        if (this.pressed == pressed) return
        this.pressed = pressed
        animator?.cancel()
        if (pressed && !active) {
            baseX = view.scaleX
            baseY = view.scaleY
            active = true
        }
        val fromX = view.scaleX
        val fromY = view.scaleY
        val toX = baseX * if (pressed) 0.95f else 1f
        val toY = baseY * if (pressed) 0.95f else 1f
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 150L
            addUpdateListener {
                val fraction = it.animatedValue as Float
                view.scaleX = fromX + (toX - fromX) * fraction
                view.scaleY = fromY + (toY - fromY) * fraction
                if (fraction == 1f && !this@PressScaleEffect.pressed) active = false
            }
            start()
        }
    }

    fun reset() {
        animator?.cancel()
        animator = null
        if (active) {
            view.scaleX = baseX
            view.scaleY = baseY
        }
        active = false
        pressed = false
    }
}
