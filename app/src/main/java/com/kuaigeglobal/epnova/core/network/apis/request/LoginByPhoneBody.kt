package com.kuaigeglobal.epnova.core.network.apis.request

import com.kuaigeglobal.epnova.common.utils.CheckSumBuilder

/**
 * 手机登录接口--验证码方式
 * @author xiaoowuu
 * @param telNum 手机号码
 * @param smsValidateCode 验证码
 * @param inviteData 邀请数据--openinstall参数  encodeData
 */
data class LoginByPhoneBody(
    val telNum: String?,
    val smsValidateCode: String?,
    //业务类型 1=手机号登录 2=换绑手机号
    val bizType: Int?,
    val inviteData: String?
){
    companion object{
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