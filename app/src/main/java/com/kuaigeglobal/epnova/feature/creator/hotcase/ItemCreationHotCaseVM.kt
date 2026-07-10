package com.kuaigeglobal.epnova.feature.creator.hotcase

import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.nhsd.basic.item.BaseItemEventsViewModel

class ItemCreationHotCaseVM(
    data: CreationHotCaseInfo?,
    private val itemClickVm: BaseItemEventsViewModel<CreationHotCaseEvent, CreationHotCaseInfo>
) : AppItemViewModel<CreationHotCaseInfo>(data) {

    fun onCreateSame() {
        itemClickVm.postEvent(CreationHotCaseEvent.CreateSame(this))
    }
}
