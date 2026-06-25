package com.kuaigeglobal.epnova.feature.recharge

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.entity.PointConfigInfo

class ItemRechargeCreditsVM(info: PointConfigInfo) : AppItemViewModel<PointConfigInfo>(info) {

    private var remainingTime = info.pointMarketingTime ?: 0L

    val isChecked by lazy { ObservableBoolean(false) }
    val discountText by lazy { ObservableField(info.getCountDownText(remainingTime)) }
    val priceText = ObservableField<String>(info.pointPrice)

    fun updateTime() {
        val info = data.get() ?: return
        discountText.set(info.getCountDownText(remainingTime))
        remainingTime = (remainingTime - 1000L).coerceAtLeast(0L)
    }
}
