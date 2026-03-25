package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ShortDramaSegmentSubtitleInfo(
    /**
     * 序号(从1开始)
     */
    val sequenceNumber: String?,
    /**
     * 字幕内容(旁白)
     */
    val subtitleContent: String?,
    /**
     * 字幕音频url
     */
    val subtitleAudioUrl: String?,
    /**
     * 字幕音频时长(秒)
     */
    val subtitleAudioDuration: Int?
) : Parcelable
