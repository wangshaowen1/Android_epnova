package com.kuaigeglobal.epnova.widget.listmoretextview

import android.view.View

/**
 * clickSpan 相关回调
 * @author zhaotk
 */
interface IPressedSpan {
    /**
     * 按下
     */
    fun setPressed(pressed : Boolean)

    /**
     * 点击
     */
    fun onClick(widget : View)
}