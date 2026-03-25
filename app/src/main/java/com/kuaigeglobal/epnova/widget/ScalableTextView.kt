package com.kuaigeglobal.epnova.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:简单点击效果的TextView
 * @Data: 2024/12/12  18:24
 * @Version: 1.0
 * *****************************************
 */
class ScalableTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val scaleFactor = 0.95f // 缩放比例

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 缩小 ImageView
                animate()
                    .scaleX(scaleFactor)
                    .scaleY(scaleFactor)
                    .setDuration(150)
                    .start()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // 还原 ImageView
                animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .start()
            }
        }
        return super.onTouchEvent(event)
    }
}
