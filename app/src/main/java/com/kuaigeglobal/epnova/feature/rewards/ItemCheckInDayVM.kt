package com.kuaigeglobal.epnova.feature.rewards

import androidx.databinding.ObservableBoolean
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.entity.CheckInTaskItem

class ItemCheckInDayVM(data: CheckInTaskItem?) : AppItemViewModel<CheckInTaskItem>(data) {
    val isChecked = ObservableBoolean(data?.status == 1)
    val isToday   = ObservableBoolean(data?.status == 2)
    val coins: Int = data?.rewardNum ?: 0
    val day: Int   = data?.day ?: 0
}
