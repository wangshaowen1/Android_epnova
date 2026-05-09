package com.kuaigeglobal.epnova.core.google.billing

import com.android.billingclient.api.Purchase

sealed class PurchaseResult {

    data class Success(val purchase: Purchase?) : PurchaseResult()

    object Cancelled : PurchaseResult()

    sealed class Failed : PurchaseResult() {
        /** 购买流程启动失败（商品查询失败 / launchBillingFlow 异常） */
        object LaunchFailed : Failed()

        /** 支付失败（Google 返回失败 / 待支付查询后仍未找到订单） */
        object PaymentFailed : Failed()

        /** 支付成功但服务端权益发放失败 */
        object BenefitFailed : Failed()
    }
}
