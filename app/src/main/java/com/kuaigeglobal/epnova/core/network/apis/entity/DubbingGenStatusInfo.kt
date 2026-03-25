package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * 字幕配音生成状态
 */
data class DubbingGenStatusInfo(
    /**
     * 生成状态 为null时说明没有购买此音色 0=待生成 1=生成中 2=生成成功 3=生成失败
     */
    val genStatus: Int?,
)