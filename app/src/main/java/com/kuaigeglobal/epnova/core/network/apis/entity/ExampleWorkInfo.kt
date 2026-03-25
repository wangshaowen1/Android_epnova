package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 *  {
 *             "workId": "作品ID",
 *             "workTitle": "作品名称",
 *             "workCoverUrl": "封面图",
 *             "workDesc": "作品简介",
 *             "workRatio": "视频比例：16:9、4:3、1:1、3:4、9:16、21:9、adaptive(自动)",
 *             "videoUrl": "当前视频url",
 *             "duration": "视频时长(秒)"
 *         }
 */
@Parcelize
data class ExampleWorkInfo(
    val duration: Int? = null,
    val videoUrl: String? = null,
    val workCoverUrl: String? = null,
    val workDesc: String? = null,
    val workId: String? = null,
    val workRatio: String? = null,
    val workTitle: String? = null
) : Parcelable