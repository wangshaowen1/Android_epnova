package com.kuaigeglobal.epnova.core.network.apis.entity


data class OrderStatusInfo(
    //订单号
    val orderNo: String? = null,
    //订单状态 0-支付中 1-支付成功
    val status: Int? = null,
)