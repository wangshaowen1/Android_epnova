package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:简单点击效果的ImageView
 * @Data: 2024/12/12  18:24
 * @Version: 1.0
 * *****************************************
 */
class ScalableLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

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
