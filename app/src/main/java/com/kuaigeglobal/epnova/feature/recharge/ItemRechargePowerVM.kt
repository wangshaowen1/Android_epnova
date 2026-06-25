package com.kuaigeglobal.epnova.feature.recharge

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.entity.CoinConfigInfo

/**
 * 充值算力选项
 */
class ItemRechargePowerVM(info: CoinConfigInfo) : AppItemViewModel<CoinConfigInfo>(info) {

    private var remainingTime = info.coinsMarketingTime ?: 0L

    val isChecked by lazy { ObservableBoolean(false) }
    val discountText by lazy {
        ObservableField(info.getCountDownText(remainingTime))
    }
    val showGiftInfo by lazy { ObservableBoolean(info.shouldShowGiftInfo()) }
    val showGiveCoins by lazy { ObservableBoolean(info.shouldShowGiftInfo() && (info.giveCoinsNum ?: 0L) > 0L) }
    val priceText  = ObservableField<String>(info.coinsPrice)
    fun updateTime() {
        val info = data.get() ?: return
        discountText.set(info.getCountDownText(remainingTime))
        remainingTime = (remainingTime - 1000L).coerceAtLeast(0L)
    }
}
