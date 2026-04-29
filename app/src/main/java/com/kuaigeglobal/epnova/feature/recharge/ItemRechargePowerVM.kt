package com.kuaigeglobal.epnova.feature.recharge

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.entity.CoinConfigInfo

/**
 * 充值算力选项
 */
class ItemRechargePowerVM(info: CoinConfigInfo) : AppItemViewModel<CoinConfigInfo>(info) {

    val isChecked by lazy { ObservableBoolean(false) }
    val discountText by lazy {
        ObservableField<String>()
    }
    val priceText  = ObservableField<String>(info.coionsPrice)
    fun updateTime() {
        val info = data.get() ?: return
        val discountValid = info.isDiscountValid()
        if (discountValid) {
            val countDownText = info.getCountDownText()
            if (countDownText.isNotEmpty()) {
                discountText.set(countDownText)
            }
        }
    }
}