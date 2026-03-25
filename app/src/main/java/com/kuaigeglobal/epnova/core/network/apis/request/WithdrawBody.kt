package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 余额提现
 */
data class WithdrawBody(
    /**
     * 提现余额
     */
    val balance: Float,
    /**
     * 提现类型:1=微信 2=支付宝
     */
    val accountType: Int
) : UserTokenBody()