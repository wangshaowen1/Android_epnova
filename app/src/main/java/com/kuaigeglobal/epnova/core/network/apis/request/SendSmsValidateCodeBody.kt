package com.kuaigeglobal.epnova.core.network.apis.request

import com.kuaigeglobal.epnova.common.utils.CheckSumBuilder


/**
 * 发送短信验证码
 * @param telNum 接收短信的手机号码
 * @param bizType 业务类型 1=手机号登录 2=换绑手机号
 */
data class SendSmsValidateCodeBody(
    val telNum: String?,
    val bizType: Int
) {
    companion object {
        //手机号登录
        const val BIZ_TYPE_LOGIN = 1
        //换绑手机号
        const val BIZ_TYPE_CHANGE_PHONE = 2
    }
    /**
     * 校验值
     */
    val encodeData = CheckSumBuilder.getCheckSum(telNum)
}
