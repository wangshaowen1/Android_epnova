package com.kuaigeglobal.epnova.core.network.apis.entity

data class EpisodeRangeInfo(
    val label: String,       // 显示文本 如 "1 - 30"
    val startIndex: Int,     // 在总列表中的起始索引
    val startEpisode: Int,   // 起始集数
    val endEpisode: Int      // 结束集数
)