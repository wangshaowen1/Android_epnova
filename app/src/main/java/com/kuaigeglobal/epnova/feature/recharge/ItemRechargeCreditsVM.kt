package com.kuaigeglobal.epnova.feature.recharge

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.entity.PointConfigInfo

class ItemRechargeCreditsVM(info: PointConfigInfo) : AppItemViewModel<PointConfigInfo>(info) {

    val isChecked by lazy { ObservableBoolean(false) }
    val discountText by lazy { ObservableField<String>() }
    val priceText = ObservableField<String>(info.pointPrice)

    fun updateTime() {
        val info = data.get() ?: return
        if (info.isDiscountValid()) {
            val text = info.getCountDownText()
            if (text.isNotEmpty()) discountText.set(text)
        }
    }
}
