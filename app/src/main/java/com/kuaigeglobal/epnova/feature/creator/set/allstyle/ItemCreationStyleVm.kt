package com.kuaigeglobal.epnova.feature.creator.set.allstyle

import androidx.databinding.ObservableBoolean
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.entity.VideoStyleConfigInfo

class ItemCreationStyleVm(info: VideoStyleConfigInfo) : AppItemViewModel<VideoStyleConfigInfo>(info) {

    val checked= ObservableBoolean(false)

}