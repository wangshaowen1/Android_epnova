package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 好评弹窗已展示上报请求体。
 */
data class RatingPopupDisplayReportBody(
    /** 弹窗展示入口：1-第二集剧本生成页，2-剧集列表页 */
    val popupLocationCode: Int
)

/**
 * 好评评分上报请求体。
 */
data class RatingScoreReportBody(
    /** 客户端上报的评分值，不限制固定星级集合 */
    val ratingScore: Double,
    /** 完成评分时的弹窗入口：1-第二集剧本生成页，2-剧集列表页 */
    val popupLocationCode: Int
)

/**
 * 好评弹窗入口枚举
 *
 * 0-关闭，1-第二集剧本生成页，2-剧集列表页，3-两个入口
 */
enum class RatingEnterEnum {
    DISABLE,
    SCRIPT,
    SERIES,
    ALL
}
