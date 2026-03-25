package com.kuaigeglobal.epnova.core.network.apis.entity

data class ShortDramaSegmentSimpleInfo(
    val storyBoardMvSegmentId: String?,//分镜mv片段Id，实际返回String类型
    val storyBoardMvId: String?,//分镜MVid
    val genStatus: Int?//[枚举]生成状态 [0=排队中/待生成 1=生成中 2=生成成功 3=生成失败]
) {
    fun isGenerating(): Boolean {
        return genStatus == 0 || genStatus == 1
    }
}