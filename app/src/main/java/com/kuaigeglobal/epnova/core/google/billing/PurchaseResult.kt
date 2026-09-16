package com.kuaigeglobal.epnova.core.google.billing

import com.android.billingclient.api.Purchase
import com.kuaigeglobal.epnova.core.network.apis.request.PaymentFailureReasonEnum

sealed class PurchaseResult {

    data class Success(val purchase: Purchase?) : PurchaseResult()

    /** 商店结果尚未收敛，本地记录会保留并由恢复任务继续查询/验单。 */
    object Pending : PurchaseResult()

    object Cancelled : PurchaseResult()

    sealed class Failed : PurchaseResult() {
        /**
         * 购买流程启动失败。
         *
         * [PaymentFailureReasonEnum.PRODUCT_UNAVAILABLE] 表示商品查询或商品配置不可用；
         * [PaymentFailureReasonEnum.STORE_LAUNCH_FAILED] 表示已经进入商店拉起阶段但未能启动。
         */
        data class LaunchFailed(
            val reasonCode: PaymentFailureReasonEnum,
        ) : Failed()

        /** 支付失败（Google 明确返回未成交结果） */
        object PaymentFailed : Failed()

        /** 支付成功但服务端权益发放失败 */
        object BenefitFailed : Failed()
    }
}
