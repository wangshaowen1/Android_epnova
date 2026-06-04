package com.kuaigeglobal.epnova.feature.works

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.viewModelScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorWorkInfo
import kotlinx.coroutines.launch

class ItemMyWorkVM(
    private val eventVm: ItemMultipleChoiceEventsViewModel<MyWorksEvent, String>,
    workInfo: CreatorWorkInfo?
) : AppItemViewModel<CreatorWorkInfo>(workInfo) {


    //当前显示状态是查看(true)还是编辑(false)
    var isView = ObservableBoolean(true)

    //是否选中
    val isSelected = ObservableBoolean(false)

    fun hasPublishBadge(): Boolean {
        return !data.get()?.publishBadgeText().isNullOrEmpty()
    }

    fun publishBadgeText(): String {
        return data.get()?.publishBadgeText().orEmpty()
    }

    @ColorRes
    fun publishBadgeTextColor(): Int {
        return data.get()?.publishBadgeTextColor() ?: R.color.fixed_white_1
    }

    @DrawableRes
    fun publishBadgeBackground(): Int {
        return data.get()?.publishBadgeBackground() ?: R.drawable.bg_60_000000_round
    }

    fun onClick() {
        val info = data.get()
        if (isView.get()) {
            eventVm.postEvent(MyWorksEvent.ViewDetails(info))
            return
        }
        if (isSelected.get()) {
            isSelected.set(false)
            eventVm.removeSelectedItem(info?.workId?:"")
        } else {
            isSelected.set(true)
            eventVm.addSelectedItem(info?.workId?:"")
        }
        eventVm.postEvent(MyWorksEvent.ComposeState(isSelected.get(), info))
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
