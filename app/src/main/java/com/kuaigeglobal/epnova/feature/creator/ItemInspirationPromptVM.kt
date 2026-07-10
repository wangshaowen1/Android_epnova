package com.kuaigeglobal.epnova.feature.creator

import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.entity.InspirationInfo

class ItemInspirationPromptVM(
    info: InspirationInfo,
    private val onClick: (InspirationInfo) -> Unit
) : AppItemViewModel<InspirationInfo>(info) {

    fun onItemClick() {
        data.get()?.let(onClick)
    }
}
