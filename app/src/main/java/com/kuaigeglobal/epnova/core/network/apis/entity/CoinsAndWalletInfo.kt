package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * {
 *     "coinsTotalNum": "金币总数量 单位：个",
 *     "coinsNum": "金币数量 单位：个",
 *     "coinsUsedNum": "已使用金币数量 单位：个",
 *     "giveCoinsNum": "赠送金币数量 单位：个",
 *     "giveCoinsUsedNum": "已使用赠送金币数量 单位：个",
 *     "giveCoinsExpiredNum": "赠送金币过期数量 单位：个",
 *     "pointsTotalNum": "算力总数量 单位：个",
 *     "pointsNum": "算力数量 单位：个",
 *     "pointsUsedNum": "已使用算力数量 单位：个",
 *     "givePointsNum": "赠送算力数量 单位：个",
 *     "givePointsUsedNum": "已使用赠送算力数量 单位：个",
 *     "givePointsExpiredNum": "赠送算力过期数量 单位：个"
 * }
 */
@Parcelize
data class CoinsAndWalletInfo(
    val coinsTotalNum: Long? = null,
    val coinsNum: Long? = null,
    val coinsUsedNum: Long? = null,
    val giveCoinsNum: Long? = null,
    val giveCoinsUsedNum: Long? = null,
    val giveCoinsExpiredNum: Long? = null,
    val pointsTotalNum: Long? = null,
    val pointsNum: Long? = null,
    val pointsUsedNum: Long? = null,
    val givePointsNum: Long? = null,
    val givePointsUsedNum: Long? = null,
    val givePointsExpiredNum: Long? = null
) : Parcelable
