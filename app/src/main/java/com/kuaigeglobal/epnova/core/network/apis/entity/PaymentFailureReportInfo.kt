package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * 支付失败事件上报结果。
 */
data class PaymentFailureReportInfo(
    /** 支付失败事件业务 ID。 */
    val failEventId: String?,
)
