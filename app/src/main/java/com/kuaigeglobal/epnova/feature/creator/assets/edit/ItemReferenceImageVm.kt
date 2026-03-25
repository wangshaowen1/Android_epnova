package com.kuaigeglobal.epnova.feature.creator.assets.edit

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.entity.ReferenceImageInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.VideoConfigInfo

class ItemReferenceImageVm(info: ReferenceImageInfo) : AppItemViewModel<ReferenceImageInfo>(info) {
    val checked = ObservableBoolean()

}