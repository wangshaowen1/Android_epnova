package com.kuaigeglobal.epnova.feature.login.data

import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.network.apis.request.BindPhoneBody
import com.kuaigeglobal.epnova.core.network.apis.request.LoginByFlashMobileBody
import com.kuaigeglobal.epnova.core.network.apis.request.LoginByPhoneBody
import com.kuaigeglobal.epnova.core.network.apis.request.SendSmsValidateCodeBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 闪验登录
 * @author xiaoowuu
 */
class FlashLoginRepository : ThirdLoginRepository() {
    /**
     * 闪验登录
     * @param flashValidateToken 闪验token
     */
    suspend fun loginByFlashMobile(flashValidateToken: String?) = withContext(Dispatchers.IO) {
        api.loginByFlashMobile(LoginByFlashMobileBody(flashValidateToken, Config.get().installData))
            .apply {
                val isNewRegister = data?.isNewRegistration() == true
//                if (isNewRegister && Config.get().installFromOpenInstall()) {
//                    OpenInstall.reportRegister()
//                }
            }
    }

    suspend fun sendSmsValidateCode(
        phoneNumber: String?,
        bizType: Int = SendSmsValidateCodeBody.BIZ_TYPE_LOGIN
    ) = withContext(Dispatchers.IO) {
        api.sendSmsValidateCode(
            SendSmsValidateCodeBody(
                phoneNumber,
                bizType = bizType
            )
        )
    }

    suspend fun loginByPhone(phoneNumber: String?, validationCode: String?) =
        withContext(Dispatchers.IO) {
            api.loginByPhone(
                LoginByPhoneBody(
                    telNum=phoneNumber,
                    smsValidateCode=validationCode,
                    bizType = LoginByPhoneBody.BIZ_TYPE_LOGIN,
                    Config.get().installData,
                )
            ).apply {
                val isNewRegister = data?.isNewRegistration() == true
//                if (isNewRegister && Config.get().installFromOpenInstall()) {
//                    OpenInstall.reportRegister()
//                }
            }
        }

    /**
     * 绑定手机
     */
    suspend fun bindPhone(phoneNumber: String?, validationCode: String?) =
        withContext(Dispatchers.IO) {
            api.bindPhone(BindPhoneBody(phoneNumber, validationCode))
        }
}