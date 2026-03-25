package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * *****************************************
 * 项目名称: GoldenFruit
 * @Author: SZYMN
 * @Introduce:用户收益信息
 * @Data: 2025/11/5/周三  13:47
 * @Version: 1.0
 * *****************************************
 */
data class UserRevenueInformationInfo(
    //累计提现金额，单位：里
    val totalWithdrawAmount: Long? = null,
    //收益余额，单位：里
    val totalProfitAmount: Long? = null,
    //当前创作收益分成比例百分比，如80表示80%
    val profitRatio: Int? = null,
    //下一等级提现比例百分比，如80表示80%
    val nextProfitRatio: Int? = null,
    //是否最高等级提现比例，0-否，1-是
    val isMaxProfitRatio: Int? = null,
    //收益说明 10积分=1元
    val profitDesc: String? = null,
){
    fun isMaxProfitRatio(): Boolean{
        return isMaxProfitRatio == 1
    }
}