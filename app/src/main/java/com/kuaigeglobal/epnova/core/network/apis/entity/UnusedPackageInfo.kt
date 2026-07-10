package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** 已购买且尚未使用的短剧创作套餐实例。 */
@Parcelize
data class UnusedPackageInfo(
    val packageInstanceId: String? = null,
    val packageConfigId: Long? = null,
    val packageType: Int? = null,
    val packageName: String? = null,
    val packageLevel: String? = null,
    val dynamicEpisodeCount: Int? = null,
    val narrationEpisodeCount: Int? = null,
    val giftPointRemaining: Long? = null,
    val displayTitle: String? = null,
    val displayDesc: String? = null,
    val defaultSelected: Int? = null,
    val episodeCount: Int? = null
) : Parcelable {
    fun hasValidInstance() = !packageInstanceId.isNullOrBlank()
    fun isDefaultSelected() = defaultSelected == 1
}
