package com.kuaigeglobal.epnova.core.network.apis.request

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CreateTaskBody(
    val workInspiration: String? = null,
    val content: String? = null,
    val workTitle: String? = null,
    val totalEpisodeCntCode: String? = null,
    val totalEpisodeCnt: String? = null,
    val workRatio: String? = null,
    val workStyleCode: String? = null,
    val reportType: Int? = null,
    val reportId: String? = null,
    val packageInstanceId: String? = null,
) : Parcelable


@Parcelize
data class ContinueTaskBody(
    val workInspiration: String? = null,//作品灵感（小说内容）小说续写得时候传
    val workId: String? = null,
    val storyId: String? = null,
    val forceCreation: Boolean? = null,//是否强制创建，默认为false
) : Parcelable
