package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 第三方登录
 * @param loginType 登录类型  5, "苹果登录"， 8, "谷歌登录"，10, "Facebook登录"
 * @param inviteData 扩展数据
 */
data class LoginByThirdBody(
    val loginType: Int,
    val identityToken: String? = null,//第三方令牌
    val accessToken: String? = null,//2026043新增Facebook 访问令牌（Facebook登录时前端传入）
    val inviteData: String? = null,
) {
    companion object {
        const val LOGIN_TYPE_GOOGLE = 8
        const val LOGIN_TYPE_FACEBOOK = 10
    }
}