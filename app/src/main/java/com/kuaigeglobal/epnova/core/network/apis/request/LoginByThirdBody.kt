package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 第三方登录
 * @author xiaoowuu
 * @param loginType 登录类型 1, "短信验证码"， 2, "闪验"， 3, "微信登录"， 4, "QQ登录"， 5, "苹果登录"， 6, "微信小程序"
 * @param thirdPartyUnionId 第三方登陆unionId（微信登陆用到）
 * @param thirdPartyOpenId 第三方登陆openId（其他第三方登陆用到）
 * @param thirdPartyNickName 第三方昵称
 * @param thirdPartyAvatar 第三方头像
 * @param inviteData 扩展数据
 * @param uniqueId 设备/客户端唯一标识（用于风控或绑定设备）
 */
data class LoginByThirdBody(
    val loginType: Int,
    val thirdPartyUnionId: String?,
    val thirdPartyOpenId: String?,
    val thirdPartyNickName: String?,
    val thirdPartyAvatar: String?,
    val inviteData: String?,
    val uniqueId: String? = null,
) {
    companion object {
        //短信验证码
        const val LOGIN_TYPE_SMS = 1

        //闪验
        const val LOGIN_TYPE_FLASH = 2

        //微信登录
        const val LOGIN_TYPE_WECHAT = 3

        //QQ登录
        const val LOGIN_TYPE_QQ = 4
    }
}