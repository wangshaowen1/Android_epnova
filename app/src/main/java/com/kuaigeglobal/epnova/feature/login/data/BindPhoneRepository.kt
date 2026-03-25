package com.kuaigeglobal.epnova.feature.login.data

import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.network.apis.request.BindPhoneBody
import com.kuaigeglobal.epnova.core.network.apis.request.SendSmsValidateCodeBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 绑定手机号
 */
class BindPhoneRepository : AppRepository() {

    /**
     * 发送验证码
     */
    suspend fun sendSmsValidateCode(phoneNumber: String?) = withContext(Dispatchers.IO) {
        api.sendSmsValidateCode(
            SendSmsValidateCodeBody(
                phoneNumber,
                SendSmsValidateCodeBody.BIZ_TYPE_CHANGE_PHONE
            )
        )
    }

    /**
     * 绑定手机
     */
    suspend fun bindPhone(phoneNumber: String?, validationCode: String?) =
        withContext(Dispatchers.IO) {
            api.bindPhone(BindPhoneBody(phoneNumber, validationCode))
        }
}