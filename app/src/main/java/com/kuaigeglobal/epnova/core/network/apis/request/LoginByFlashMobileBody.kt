package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 手机登录接口--闪验方式
 * @author xiaoowuu
 * @param flashValidateToken 闪验的token
 * @param inviteData 邀请数据--openinstall参数
 */
data class LoginByFlashMobileBody(
    val flashValidateToken: String?,
    val inviteData: String?
)