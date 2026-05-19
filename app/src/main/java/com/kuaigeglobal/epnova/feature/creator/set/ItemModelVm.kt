package com.kuaigeglobal.epnova.feature.creator.set

import androidx.databinding.ObservableBoolean
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.entity.CreationModelInfo

class ItemModelVm(info: CreationModelInfo) : AppItemViewModel<CreationModelInfo>(info) {

    val checked = ObservableBoolean(false)

}
