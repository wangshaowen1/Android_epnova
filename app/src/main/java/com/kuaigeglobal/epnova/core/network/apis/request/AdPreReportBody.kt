package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 广告观看前置上报请求体
 * @param adScene 广告场景：1-签到激励广告，2-福利任务激励广告
 * @param bizId 业务关联ID（adScene=1时传 checkin_id，adScene=2时传 taskRecordId）
 */
data class AdPreReportBody(
    val adScene: Int,
    val bizId: Long? = null
) : UserTokenBody()
