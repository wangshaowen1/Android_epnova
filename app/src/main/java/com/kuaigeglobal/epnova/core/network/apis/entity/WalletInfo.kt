package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * {
 *         "coinsTotal": "金币总数量 单位：个",
 *         "coionsNum": "金币数量 单位：个",
 *         "coionsUsedNum": "已使用金币数量 单位：个",
 *         "giveCoionsNum": "赠送金币数量 单位：个",
 *         "giveCoionsUsedNum": "已使用赠送金币数量 单位：个",
 *         "giveCoinsExpiredNum": "赠送金币过期数量 单位：个"
 *     }
 */
@Parcelize
data class WalletInfo(
    val coinsTotalNum: Long? = null,
    val coionsNum: Long? = null,
    val coionsUsedNum: Long? = null,
    val giveCoinsExpiredNum: Long? = null,
    val giveCoionsNum: Long? = null,
    val giveCoionsUsedNum: Long? = null
) : Parcelable