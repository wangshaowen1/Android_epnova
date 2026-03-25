package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 获取用户分享链接
 * @param shareUrlType 分享类型 1.作品详情分享 2.剧集分享
 * @param shareDataId
 * @param beSharedUserToken 资源被被分享者的用户的token（如果是自己的，则和userToken相同）
 * @param shareSonDataId 分享子id
 */
data class UserShareUrlBody(
    val shareUrlType: Int?,//
    val shareDataId: String?,
    val beSharedUserToken: String? = null,
    val shareSonDataId: String? = null
) : UserTokenBody() {
    companion object {
        //绘本分享
        const val SHARE_TYPE_BOOK = 1

        /**
         * 1.作品详情分享
         */
        const val SHARE_TYPE_EPISODE = 1

        /**
         * 2.剧集分享
         */
        const val SHARE_TYPE_SERIES = 2
    }
}