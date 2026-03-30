package com.kuaigeglobal.epnova.feature.mine.child

import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo
import com.kuaigeglobal.epnova.feature.works.ItemMultipleChoiceEventsViewModel

class ItemMineChildVM(
    private val eventVm: ItemMultipleChoiceEventsViewModel<MyWorksVOEvent, String>,
    workInfo: WorkVOInfo?,
    val collect: Boolean
) : AppItemViewModel<WorkVOInfo>(workInfo) {


    fun onClick() {
        val info = data.get()
        eventVm.postEvent(MyWorksVOEvent.ViewDetails(info))
    }
}