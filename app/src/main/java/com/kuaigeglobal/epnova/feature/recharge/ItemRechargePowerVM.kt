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
        ObservableField(info.getCountDownText())
    }
    val showGiftInfo by lazy { ObservableBoolean(info.shouldShowGiftInfo()) }
    val showGiveCoins by lazy { ObservableBoolean(info.shouldShowGiftInfo() && (info.giveCoinsNum ?: 0L) > 0L) }
    val priceText  = ObservableField<String>(info.coinsPrice)
    fun updateTime() {
        val info = data.get() ?: return
        discountText.set(info.getCountDownText())
    }
}
