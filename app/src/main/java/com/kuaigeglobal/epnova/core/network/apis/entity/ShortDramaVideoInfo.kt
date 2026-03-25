package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 短剧视频信息
 */
@Parcelize
data class ShortDramaVideoInfo(
    /**
     * 视频URL
     */
    val videoUrl: String?,
    /**
     * 视频时长(秒)
     */
    val videoDuration: Int?
) : Parcelable
