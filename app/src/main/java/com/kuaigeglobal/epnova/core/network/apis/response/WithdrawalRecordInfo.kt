package com.kuaigeglobal.epnova.core.network.apis.response

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:用户提现记录（余额）
 * @Data: 2025/10/15/周三  17:02
 * @Version: 1.0
 * *****************************************
 */
data class WithdrawalRecordInfo(
    val id: String? = null,
    //提现状态 0-提现中 1-提现成功 2-提现失败
    val withdrawalStatus: Int? = null,
    //提现人民币金额，单位：厘
    val withdrawalAmount: Long? = null,
    //提现方式 1-微信 2-支付宝
    val withdrawalWay: Int? = null,
    //交易时间
    val createTime: Long? = null,
    //提现描述
    val withdrawalDesc: String? = null
){
    companion object{
        //微信
        const val WITHDRAWAL_WAY_WECHAT = 1
        //支付宝
        const val WITHDRAWAL_WAY_ALIPAY = 2
        //提现中
        const val WITHDRAWAL_STATUS_WAIT = 0
        //提现成功
        const val WITHDRAWAL_STATUS_SUCCESS = 1
        //提现失败
        const val WITHDRAWAL_STATUS_FAIL = 2
    }
}