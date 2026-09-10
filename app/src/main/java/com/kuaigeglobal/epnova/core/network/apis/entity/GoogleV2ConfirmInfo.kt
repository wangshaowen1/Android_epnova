package com.kuaigeglobal.epnova.core.network.apis.entity

data class GoogleV2ConfirmInfo(

    /**
     * 实际归属的平台内部订单号。
     */
    val orderNo: String?,

    /**
     * 订单交易状态，待支付也包含官方交易尚未完成或服务端仍在处理的情况。
     * 0 待支付
     * 1 支付成功
     * 2 用户取消支付
     * 3 超时未支付
     * 4 支付失败
     */
    val tradeStatus: Int?,

    /**
     * 订单退款状态，支付成功时仍需结合本字段判断展示结果。
     * 0 未退款
     * 1 退款中
     * 2 全额退款
     * 3 退款失败
     * 4 部分退款
     */
    val refundStatus: Int?,

    /**
     * 是否允许结束本次 Apple 交易，仅在精确交易已处理后返回 1；Google 恒为 0。
     * 0 不允许结束交易
     * 1 允许结束交易
     */
    val finishAllowed: Int?,

    /**
     * 当前业务状态说明，仅用于展示，不作为状态分支判断依据。
     */
    val statusMessage: String?
)
