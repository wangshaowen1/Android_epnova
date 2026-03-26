package com.kuaigeglobal.epnova.feature.mine.child

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.viewModelScope
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkVOInfo
import com.kuaigeglobal.epnova.feature.works.ItemMultipleChoiceEventsViewModel
import kotlinx.coroutines.launch

class ItemMineChildVM(
    private val eventVm: ItemMultipleChoiceEventsViewModel<MyWorksVOEvent, String>,
    workInfo: WorkVOInfo?
) : AppItemViewModel<WorkVOInfo>(workInfo) {


    //当前显示状态是查看(true)还是编辑(false)
    var isView = ObservableBoolean(true)

    //是否选中
    val isSelected = ObservableBoolean(false)

    fun onClick() {
        val info = data.get()
        if (isView.get()) {
            eventVm.postEvent(MyWorksVOEvent.ViewDetails(info))
            return
        }
        if (isSelected.get()) {
            isSelected.set(false)
            eventVm.removeSelectedItem(info?.workId ?: "")
        } else {
            isSelected.set(true)
            eventVm.addSelectedItem(info?.workId ?: "")
        }
        eventVm.postEvent(MyWorksVOEvent.ComposeState(isSelected.get(), info))
    }

    init {
        viewModelScope.launch {
            eventVm.switchStatus.collect {
                isView.set(it)
            }
        }

        viewModelScope.launch {
            eventVm.selectedItemIds.collect { selectedIds ->
                isSelected.set(selectedIds.contains(workInfo?.workId))
            }
        }
    }
}