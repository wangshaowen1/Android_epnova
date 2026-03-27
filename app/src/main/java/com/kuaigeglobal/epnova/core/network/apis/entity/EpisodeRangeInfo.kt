package com.kuaigeglobal.epnova.core.network.apis.entity

data class EpisodeRangeInfo(
    val segmentLabel: String?,       // 显示文本 如 "1 - 30"
    val segmentIndex: Int?,     // 在总列表中的起始索引
    val startSortNo: Int?,   // 起始集数
    val endSortNo: Int?      // 结束集数
)