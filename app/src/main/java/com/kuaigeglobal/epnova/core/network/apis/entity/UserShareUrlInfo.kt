package com.kuaigeglobal.epnova.core.network.apis.entity

/**
 * 获取用户分享链接
 */
class UserShareUrlInfo {
    /**
     * 用户分享链接
     */
    val userShareUrl: String? = null
}

data class UserSharePosterInfo(
    /**
     * 邀请海报背景图
     */
    val sharePosterBgImgUrls: List<String>? = null,
    val shareQrCodeUrl: String? = null//邀请二维码
)