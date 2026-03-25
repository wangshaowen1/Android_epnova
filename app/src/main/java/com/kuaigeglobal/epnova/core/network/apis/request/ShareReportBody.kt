package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 用户分享操作上报
 * @param shareDataId 分享数据id(歌曲id/mvId)
 * @param shareTargetPlatformType 分享目标平台大类 1.好友(微信好友、朋友圈)  2.其他平台(抖音、小红书、快手)
 * @param shareTargetPlatformSubType 分享目标平台小类 10-微信好友 11-朋友圈 20-抖音 21-小红书 22-快手
 */
data class ShareReportBody(
    val shareDataId: String?,
    val shareTargetPlatformType: Int,
    val shareTargetPlatformSubType: Int
) : UserTokenBody()