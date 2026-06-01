package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * {
 *     "availableBalanceCent": "可提现余额，单位：分。",
 *     "availableBalanceText": "可提现余额展示文案，示例：$999.00。",
 *     "totalEarnedCent": "累计收益金额，单位：分。",
 *     "totalEarnedText": "累计收益展示文案，示例：$1284.50。",
 *     "pendingAmountCent": "本月待结算金额，单位：分。",
 *     "pendingAmountText": "本月待结算金额展示文案，示例：$1284.50。",
 *     "settlementTimeZone": "收益结算时区，影响月份归属和下次结算时间。",
 *     "nextSettlementTime": "下次结算时间，毫秒时间戳。",
 *     "registerMonth": "用户注册月份，格式：yyyy-MM。",
 *     "currentMonth": "当前结算时区下的当前月份，格式：yyyy-MM。"
 * }
 */
@Parcelize
data class IncomeSummaryInfo(
    val availableBalanceCent: Long? = null,
    val availableBalanceText: String? = null,
    val totalEarnedCent: Long? = null,
    val totalEarnedText: String? = null,
    val pendingAmountCent: Long? = null,
    val pendingAmountText: String? = null,
    val settlementTimeZone: String? = null,
    val nextSettlementTime: Long? = null,
    val registerMonth: String? = null,
    val currentMonth: String? = null
) : Parcelable
