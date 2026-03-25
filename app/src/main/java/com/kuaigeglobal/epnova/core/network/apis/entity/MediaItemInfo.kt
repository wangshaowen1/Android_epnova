package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaItemInfo(
    var mediaCoverUrl: String?,
    val mediaPhotoUrls: List<String?>?,
    val mediaPlayNum: String?,
    val mediaSourceUrl: String?,
    val mediaType: Int?//媒体类型，1：音乐，2：视频，3：图片序列
) : Parcelable
