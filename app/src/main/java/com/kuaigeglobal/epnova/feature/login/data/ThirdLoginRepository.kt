package com.kuaigeglobal.epnova.feature.login.data

import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.network.apis.request.LoginByThirdBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 第三方登录
 */
open class ThirdLoginRepository : AppRepository() {
    /**
     * 第三方登录
     */
    suspend fun loginByThird(body: LoginByThirdBody) = withContext(Dispatchers.IO) {
        api.loginByThird(body).apply {
            val isNewRegister = data?.isNewRegistration() == true
//            if (isNewRegister && Config.get().installFromOpenInstall()) {
//                OpenInstall.reportRegister()
//            }
            Global.refreshCoinsAndPoints()
        }
    }
}