package com.kuaigeglobal.epnova.core.network.apis.response

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:邀请收益信息
 * @Data: 2025/10/15/周三  14:12
 * @Version: 1.0
 * *****************************************
 */
data class InviteIncomePageInfo(
    //头像
    val avatarUrl: String? = null,
    //昵称
    val nickName: String? = null,
    //邀请时间
    val inviteTime: Long? = null,
    //邀请备注
    val inviteRemark: String? = null,
    //金额
    val commissionAmount: Long? = null,
    //交易类型： 1-收入，2-支出
    val tradeType: Int? = null,
)