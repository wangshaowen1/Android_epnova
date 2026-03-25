package com.kuaigeglobal.epnova.feature.recharge

import androidx.databinding.ObservableBoolean
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.entity.PaymentInfo


/**
 * 充值支付方式
 */
class ItemRechargePaymentVM(info: PaymentInfo?) : AppItemViewModel<PaymentInfo>(info) {
    val isChecked by lazy { ObservableBoolean(false) }
}