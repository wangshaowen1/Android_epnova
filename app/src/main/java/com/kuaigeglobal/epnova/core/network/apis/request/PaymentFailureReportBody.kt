package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 支付失败事件上报请求体。
 *
 * 同一次失败重试时应复用相同的 clientEventId，服务端据此保证幂等。
 */
data class PaymentFailureReportBody(
    /** 平台内部订单号，必须来自预下单成功的订单，最长 32 个字符。 */
    val orderNo: String,
    /** 客户端事件唯一标识，最长 64 个字符，只允许字母、数字、下划线和中划线。 */
    val clientEventId: String,
    /** 商店侧失败原因码：STORE_LAUNCH_FAILED、PRODUCT_UNAVAILABLE 或 PAYMENT_METHOD_UNAVAILABLE。 */
    val failReasonCode: String,
    /** 商店返回的结构化错误码，可选，最长 128 个字符。 */
    val platformErrorCode: String? = null,
)

/**
 * 商店侧失败原因枚举
 */
enum class PaymentFailureReasonEnum {
    /** 支付拉起失败 */
    STORE_LAUNCH_FAILED,
    /** 商品不可用 */
    PRODUCT_UNAVAILABLE,
    /** 支付方式不可用 */
    PAYMENT_METHOD_UNAVAILABLE
}
