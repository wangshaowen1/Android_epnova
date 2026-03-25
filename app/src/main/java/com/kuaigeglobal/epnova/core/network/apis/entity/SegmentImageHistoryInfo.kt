package com.kuaigeglobal.epnova.core.network.apis.entity

data class SegmentImageHistoryInfo(
    /**
     * 短剧分镜片段id
     */
    val shotSegmentId: String?,
    /**
     * 镜片段图片id
     */
    val shotSegmentImageId: String?,
    /**
     * 分镜MV片段素材URL(图片Url)
     */
    val shotSegmentImageUrl: String?,
    /**
     * [枚举]生成状态 [0=排队中/待生成 1=生成中 2=生成成功 3=生成失败]
     */
    val genStatus: Int?
)