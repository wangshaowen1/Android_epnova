package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:简单点击效果的ConstraintLayout
 * @Data: 2024/12/12  18:24
 * @Version: 1.0
 * *****************************************
 */
class ScalableConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var pressEffect: PressScaleEffect? = null

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        val pressed = isEnabled && isPressed
        if (pressed && pressEffect == null) pressEffect = PressScaleEffect(this)
        pressEffect?.update(pressed)
    }

    override fun onDetachedFromWindow() {
        pressEffect?.reset()
        super.onDetachedFromWindow()
    }
}
