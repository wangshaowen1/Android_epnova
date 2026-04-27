package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 第三方登录
 * @param loginType 登录类型  5, "苹果登录"， 8, "谷歌登录"，9, "Facebook登录"
 * @param inviteData 扩展数据
 */
data class LoginByThirdBody(
    val loginType: Int,
    val identityToken: String?,//第三方令牌
    val inviteData: String? = null,
) {
    companion object {
        const val LOGIN_TYPE_GOOGLE = 8
        const val LOGIN_TYPE_FACEBOOK = 9 // TODO: 确认服务端 Facebook 登录类型值
    }
}