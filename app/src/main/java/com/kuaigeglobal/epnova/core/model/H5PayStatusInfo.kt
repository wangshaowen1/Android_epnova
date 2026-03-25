package com.kuaigeglobal.epnova.core.model

/**
 * H5 调用APP支付协议，由app调用接口获取支付参数并支付
 * APP 通过payWay 区分支付方式
 * @param code 200：成功，400：失败
 * @param data 支付后的接口回调（订单ID）
 */
data class H5PayStatusInfo(
    /**
     * 200：成功，400：失败
     */
    val code: Int,
    /**
     * 支付后的接口回调（订单ID）
     */
    val data: String?
)