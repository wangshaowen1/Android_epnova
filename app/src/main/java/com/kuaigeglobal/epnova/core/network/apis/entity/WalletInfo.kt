package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * {
 *     "pointNum": "算力数量 单位：个",
 *     "pointUsedNum": "已使用算力数量 单位：个"
 * }
 */
@Parcelize
data class WalletInfo(
    /**
     * 算力数量 单位：个
     */
    val pointNum: Int?,
    /**
     * 已使用算力数量 单位：个
     */
    val pointUsedNum: Int?
) : Parcelable