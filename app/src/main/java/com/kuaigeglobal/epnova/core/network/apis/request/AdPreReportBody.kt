package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 广告观看前置上报请求体
 * @param adScene 广告场景：1-签到激励广告，2-福利任务激励广告
 * @param bizId 业务关联ID（可选）adScene=1(签到广告)时 不需要传，系统自己寻找当天任务ID（这个比较特殊，需要先签到才能看广告，如果用户签到后直接划掉app，那肯定是找不到任务ID，所以才这么设计由系统自己寻找）；adScene=2(任务广告)时传 task_record_id-广告任务列表对应的任务ID：taskRecordId；
 */
data class AdPreReportBody(
    val adScene: Int,
    val bizId: String? = null
) : UserTokenBody()
