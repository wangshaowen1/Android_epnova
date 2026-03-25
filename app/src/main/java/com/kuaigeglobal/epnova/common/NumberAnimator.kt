package com.kuaigeglobal.epnova.common

import android.animation.ValueAnimator
import android.widget.TextView
import java.text.NumberFormat
import java.util.Locale

object NumberAnimator {

    fun animateTextView(textView: TextView, start: Int, end: Int,suffix:String, duration: Long = 1500) {
        // 停止任何正在进行的动画
        textView.tag?.let {
            if (it is ValueAnimator) {
                it.cancel()
            }
        }

        val animator = ValueAnimator.ofInt(start, end).apply {
            this.duration = duration
            addUpdateListener { animation ->
                val currentValue = animation.animatedValue as Int
                textView.text = formatCurrency(currentValue,suffix)
            }
            start()
        }

        // 将动画器存储在 tag 中以便后续取消
        textView.tag = animator
    }

    private fun formatCurrency(value: Int,suffix:String): String {
        val formatter = NumberFormat.getNumberInstance(Locale.CHINA)
        return "${formatter.format(value)}${suffix}"
    }

    fun animateTextView(textView: TextView, endValue: Int,suffix: String) {
        val currentText = textView.text.toString()
        val cleanText = currentText.replace("[^\\d]".toRegex(), "")
        val startValue = cleanText.toIntOrNull() ?: 0
        animateTextView(textView, startValue, endValue,suffix)
    }
}