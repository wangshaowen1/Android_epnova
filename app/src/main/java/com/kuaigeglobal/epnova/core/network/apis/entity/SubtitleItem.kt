package com.kuaigeglobal.epnova.core.network.apis.entity

import com.google.gson.annotations.SerializedName

// 定义JSON对应的实体类
data class SubtitleItem(
    val text: String?,
    @SerializedName("start_time")
    val startTime: Int?,
    @SerializedName("end_time")
    val endTime: Int?,
    val words: List<WordItem>?
)

data class WordItem(
    val text: String?,
    @SerializedName("start_time")
    val startTime: Int?,
    @SerializedName("end_time")
    val endTime: Int?
)
