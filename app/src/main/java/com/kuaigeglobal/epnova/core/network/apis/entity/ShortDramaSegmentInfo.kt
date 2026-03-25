package com.kuaigeglobal.epnova.core.network.apis.entity

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class ShortDramaSegmentInfo(
    /**
     * 分镜片段id
     */
    val shotSegmentId: String?,
    /**
     * 序号(从1开始)
     */
    val sequenceNumber: String?,
    /**
     * 预估时长(秒)
     */
    val estimatedDuration: Int?,
    /**
     * 实际时长(秒)
     */
    val actualDuration: Int?,
    /**
     * 分镜片段图片id
     */
    val shotSegmentImageId: String?,
    /**
     * 分镜片段图片URL(图片Url)
     */
    val shotSegmentImageUrl: String?,
    /**
     * 分镜画面描述
     */
    val shotImageDesc: String?,
    /**
     * [枚举]生成状态 [0=排队中/待生成 1=生成中 2=生成成功 3=生成失败]
     */
    val genStatus: Int?,
    /**
     * 分镜片段字幕集合
     */
    val shotSegmentSubtitleList: List<ShortDramaSegmentSubtitleInfo>?,
    /**
     * 分镜片段角色集合
     */
    val roleList: List<ShortDramaRoleInfo>?
) : Parcelable {

    fun isGenerated() = genStatus == 2

    fun isGenerating() = genStatus == 0 || genStatus == 1

    fun isFailed() = genStatus == 3

//    fun isFailed2Regenerate() = genStatus == 3 && (genSuccessCount == null || genSuccessCount == 0)

    fun isImage() = true

    fun isVideo() = false

    fun isVideoExists() = false

    val durationMs get() = (actualDuration ?: 0) * 1000L


    /**
     * 播放开始时间
     */
    @IgnoredOnParcel
    var startPositionMs = 0L

    /**
     * 播放结束时间
     */
    val endPositionMs get() = startPositionMs + durationMs

    fun mediaUrls(): List<String>? {
        return shotSegmentSubtitleList?.mapNotNull { it.subtitleAudioUrl }
    }
}