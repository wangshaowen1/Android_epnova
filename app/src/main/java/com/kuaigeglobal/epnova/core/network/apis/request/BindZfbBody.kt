package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 获取支付宝信息(并绑定)
 */
data class BindZfbBody(
    /**
     * 支付宝授权code
     */
    val authorizationCode: String?
) : UserTokenBody()