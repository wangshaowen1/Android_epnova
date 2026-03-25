package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * *****************************************
 * 项目名称: GoldenFruit
 * @Author: SZYMN
 * @Introduce:收益明细列表
 * @Data: 2025/11/5/周三  14:03
 * @Version: 1.0
 * *****************************************
 */
data class ProfitDetailInfo(
    //交易类型 1-收入 2-支出
    val tradeType: Int? = null,
    //交易的金额，单位：厘
    val tradeAmount: Long? = null,
    //交易标题
    val tradeTitle: String? = null,
    //交易子标题
    val tradeSubTitle: String? = null,
    //创建时间
    val createTime: Long? = null,
){

    fun income(): Boolean {
        return tradeType == 1
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProfitDetailInfo

        if (tradeType != other.tradeType) return false
        if (tradeAmount != other.tradeAmount) return false
        if (tradeTitle != other.tradeTitle) return false
        if (tradeSubTitle != other.tradeSubTitle) return false
        if (createTime != other.createTime) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tradeType ?: 0
        result = 31 * result + (tradeAmount?.hashCode() ?: 0)
        result = 31 * result + (tradeTitle?.hashCode() ?: 0)
        result = 31 * result + (tradeSubTitle?.hashCode() ?: 0)
        result = 31 * result + (createTime?.hashCode() ?: 0)
        return result
    }
}