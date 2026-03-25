package com.kuaigeglobal.epnova.core.network.apis.request

/**
 * 绑定手机号
 * @param telNum 手机号
 * @param smsValidateCode 验证码
 */
data class BindPhoneBody(
    val telNum: String?,
    val smsValidateCode: String?
) : UserTokenBody()