package com.kuaigeglobal.epnova.core.network.apis.entity

import java.io.Serializable

/**
 * 提现账户信息
 */
data class WithdrawAccountInfo(
    /**
     * 账号类型 1=微信 2=支付宝
     */
    val accountType: Int?,
    /**
     * 头像
     */
    val accountAvatar: String?,
    /**
     * 昵称
     */
    val accountName: String?
) : Serializable {
    companion object {
        /**
         * 账号类型 1=微信
         */
        const val ACCOUNT_TYPE_WX = 1

        /**
         * 账号类型 2=支付宝
         */
        const val ACCOUNT_TYPE_ZFB = 2
        private const val serialVersionUID = -81088187580549L
    }

}