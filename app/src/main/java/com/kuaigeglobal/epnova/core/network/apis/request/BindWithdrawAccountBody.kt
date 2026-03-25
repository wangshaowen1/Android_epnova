package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 绑定体现账号
 */
data class BindWithdrawAccountBody(
    /**
     * 账号类型:1=微信 2=支付宝
     */
    val accountType: Int,
    /**
     * 账户id (微信openid/支付宝openid)
     */
    val accountId: String?,
    /**
     * 头像
     */
    val accountAvatar: String?,
    /**
     * 昵称
     */
    val accountName: String?
) : UserTokenBody()