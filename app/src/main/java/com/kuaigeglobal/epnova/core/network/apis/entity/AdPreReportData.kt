package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * 广告观看前置上报响应
 */
data class AdPreReportData(
    /** 广告流水ID（雪花算法生成），客户端需将此值设为 AdMob 的 customRewardString */
    val adFlowId: Long?
)
