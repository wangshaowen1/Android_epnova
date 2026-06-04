package com.kuaigeglobal.epnova.core.network.apis.response

import com.kuaigeglobal.epnova.common.utils.DateUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:获取积分消耗明细分页
 * @Data: 2025/9/10/周三  17:56
 * @Version: 1.0
 * *****************************************
 * {
 *     "coinsDetailCode": "金币明细编码",
 *     "pointDetailCode": "算力明细编码",
 *     "transType": "交易类型（1-收入、2-支出）",
 *     "tradeSubType": "交易子类型（Credits：1001-算力充值、1002-生图失败退款、1003-生视频失败退款、2001-生成图片、2002-生成视频、2003-算力过期；Coins：1001-金币充值、1002-福利中心完成任务赠送金币、2-章节解锁支出金币）",
 *     "tradeTitle": "交易标题",
 *     "tradeSubTitle": "交易子标题",
 *     "tradeDate": "交易时间：毫秒时间戳（UTC 瞬时，与数据库存储一致；前端可自行转本地展示）",
 *     "tradeNum": "变动的金币数/变动的虚拟货币数，单位：个"
 * }
 */
data class ComputingPowerDetailsInfo(
    /**金币明细编码*/
    val coinsDetailCode: Long?,
    /**算力明细编码*/
    val pointDetailCode: Long?,
    /**交易类型（1-收入、2-支出）*/
    val transType: Int?,
    /**交易子类型（Credits：1001-算力充值、1002-生图失败退款、1003-生视频失败退款、2001-生成图片、2002-生成视频、2003-算力过期；Coins：1001-金币充值、1002-福利中心完成任务赠送金币、2-章节解锁支出金币）*/
    val tradeSubType: Int?,
    /**交易标题*/
    val tradeTitle: String?,
    /**交易子标题*/
    val tradeSubTitle: String?,
    /**交易时间：毫秒时间戳（UTC 瞬时，与数据库存储一致；前端可自行转本地展示）*/
    val tradeDate: Long?,
    /**变动的虚拟货币数，单位：个*/
    val tradeNum: Int?
) {
    companion object {
        //积分获取
        const val SCORE_TYPE_GET = 1

        //积分消耗
        const val SCORE_TYPE_CONSUME = 2
    }

    fun getFormattedTradeDate(): String {
        return tradeDate?.let {
            DateUtil.getFormatDateTime(it, DateUtil.FORMAT_DATETIME_BIAS)
        } ?: ""
    }
}