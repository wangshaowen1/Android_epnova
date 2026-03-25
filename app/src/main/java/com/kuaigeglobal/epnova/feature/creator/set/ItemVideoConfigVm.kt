package com.kuaigeglobal.epnova.feature.creator.set

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.entity.VideoConfigInfo

class ItemVideoConfigVm(ratio: VideoConfigInfo) : AppItemViewModel<VideoConfigInfo>(ratio) {
    val checked = ObservableBoolean()

    val input = ObservableField<String>()
}