package com.kuaigeglobal.epnova.widget.listmoretextview

import android.content.Context

object PublicMethod {

    fun isEmojiCharacter(codePoint: Char): Boolean {
        return !(codePoint.code == 0x0 ||
                codePoint.code == 0x9 ||
                codePoint.code == 0xA ||
                codePoint.code == 0xD ||
                codePoint.code in 0x20..0xD7FF ||
                codePoint.code in 0xE000..0xFFFD ||
                codePoint.code in 0x10000..0x10FFFF)// codePoint.code in 0x100000..0x10FFFF)
    }

    /**
     * 转换sp为px
     *
     * @param context
     * @param spValue
     * @return
     */
    fun sp2px(context: Context, spValue: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (spValue * fontScale + 0.5f).toInt()
    }

    fun dp2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }
}