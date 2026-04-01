package com.kuaigeglobal.epnova.core.network.apis.response

import com.kuaigeglobal.epnova.common.utils.DateUtil

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:获取积分消耗明细分页
 * @Data: 2025/9/10/周三  17:56
 * @Version: 1.0
 * *****************************************
 * {
 *     "pointDetailCode": "算力明细编码",
 *     "transType": "交易类型（1-收入、2-支出）",
 *     "tradeSubType": "交易子类型（1001-算力充值、1002-生图失败退款、1003-生视频失败退款、2001-生成图片、2002-生成视频、2003-算力过期）",
 *     "tradeTitle": "交易标题",
 *     "tradeSubTitle": "交易子标题",
 *     "tradeDate": "交易日期",
 *     "tradeNum": "变动的算力数，单位：个"
 * }
 */
data class ComputingPowerDetailsInfo(
    /**算力明细编码*/
    val pointDetailCode: String?,
    /**交易类型（1-收入、2-支出）*/
    val transType: Int?,
    /**交易子类型（1001-算力充值、1002-生图失败退款、1003-生视频失败退款、2001-生成图片、2002-生成视频、2003-算力过期）*/
    val tradeSubType: Int?,
    /**交易标题*/
    val tradeTitle: String?,
    /**交易子标题*/
    val tradeSubTitle: String?,
    /**交易日期*/
    val tradeDate: Long?,
    /**变动的算力数，单位：个*/
    val tradeNum: String?
) {
    companion object {
        //积分获取
        const val SCORE_TYPE_GET = 1

        //积分消耗
        const val SCORE_TYPE_CONSUME = 2
    }

    fun getFormattedTradeDate(): String {
        return tradeDate?.let {
            DateUtil.getFormatDateTime(it, DateUtil.FORMAT_DATETIME)
        } ?: ""
    }
}