package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.kuaigeglobal.epnova.databinding.PopupSeriesMoreBinding

/**
 * 剧集更多
 */
class SeriesMorePopupView(context: Context) : IPopupView {
    val binding by lazy { PopupSeriesMoreBinding.inflate(LayoutInflater.from(context), null, false) }

    override val root: View
        get() = binding.root
    override val bg: View
        get() = binding.bg
    override val popup: View
        get() = binding.popup
}