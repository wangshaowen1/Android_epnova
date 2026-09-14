package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PopupClaimInfo(
    val showPopup: Int,
    val popupEventId: String?,
    val popupDeliveryId: String?,
    val title: String?,
    val content: String?,
    val imageUrl: String?,
    val workTitle: String?,
    val jumpType: Int?,
    val jumpUri: String?,
    val workId: String?
) : Parcelable
