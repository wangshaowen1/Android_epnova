package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * 提现页信息
 */
data class WithdrawInfo(
    /**
     * 待到账(余额：元)
     */
    val treatAccountBalance: Float?,
    /**
     * 可提现余额(元)
     */
    val balance: Float?,
    /**
     * 提现中(余额：元)
     */
    val withdrawalBalance: Float?,
    /**
     * 提现规则说明
     */
    val withdrawalRule: String?,
    /**
     * 提现账号（未绑定时为空）
     */
    val withdrawalAccountList: List<WithdrawAccountInfo>?,
    /**
     * 最下面提现须知
     */
    val withdrawalRuleMsgList: List<String?>?,
    /**
     * 用户端-单笔提现最低金额，单位:元
     */
    val minWithdrawalAmount: Float?,
    /**
     * 用户端-单笔提现最高金额，单位:元
     */
    val maxWithdrawalAmount: Float?,
    /**
     * 支付宝授权链接
     */
    val aliPayStr: String?
) {

    fun withdrawalRuleMsgList() = withdrawalRuleMsgList?.joinToString("\n")

}