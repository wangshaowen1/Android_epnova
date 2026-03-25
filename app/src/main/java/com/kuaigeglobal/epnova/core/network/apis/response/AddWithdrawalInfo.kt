package com.kuaigeglobal.epnova.core.network.apis.response

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:余额提现
 * @Data: 2025/10/14/周二  16:32
 * @Version: 1.0
 * *****************************************
 */
data class AddWithdrawalInfo(
    //结果描述
    val msg: String? = null,
    //提现次数
    val withdrawalSum: Int? = null,
)