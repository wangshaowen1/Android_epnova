package com.kuaigeglobal.epnova.widget.listmoretextview

import android.content.Context
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat

/**
 * 链接点击span
 * @author  zhaotk
 */
abstract class MyLinkClickSpan(private val context: Context) :
    ClickableSpan(), IPressedSpan {
    private var isPressed = false
    abstract fun onSpanClick(widget: View?)
    override fun onClick(widget: View) {
        if (ViewCompat.isAttachedToWindow(widget)) {
            onSpanClick(widget)
        }
    }

    override fun setPressed(pressed: Boolean) {
        isPressed = pressed
    }

    override fun updateDrawState(ds: TextPaint) {
        if (isPressed) {
            ds.bgColor = ContextCompat.getColor(context, android.R.color.holo_blue_light)
        } else {
            ds.bgColor = ContextCompat.getColor(context, android.R.color.transparent)
        }
        ds.isUnderlineText = false
    }
}