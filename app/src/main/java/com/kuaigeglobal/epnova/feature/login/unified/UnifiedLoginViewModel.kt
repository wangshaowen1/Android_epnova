package com.kuaigeglobal.epnova.feature.login.unified

import androidx.databinding.ObservableBoolean
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.getInstall
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.ext.loginToast
import com.kuaigeglobal.epnova.core.network.apis.request.LoginByThirdBody
import com.kuaigeglobal.epnova.core.network.apis.response.UserHomeData
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.login.data.ThirdLoginRepository
import com.kuaigeglobal.epnova.kit.SignUtils
import com.nhsd.basic.ext.showToast
import kotlin.text.get

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:聚合登录
 * @Data: 2026/2/11/周三  11:37
 * @Version: 1.0
 * *****************************************
 */
class UnifiedLoginViewModel : AppRepositoryViewModel<ThirdLoginRepository>() {

    /**
     * 协议是否勾选
     */
    val checkedPolicy = ObservableBoolean(false)

    fun loginByThird(body: LoginByThirdBody, onLogin: ((UserHomeData?) -> Unit)? = null) {
        launchWithLoading (true, block = {
            response(mRepo.loginByThird(body), result = {
                User.get().updateFrom(data)
                SignUtils.signIn()
                data.loginToast()
                onLogin?.invoke(data)
            }, error = { this?.msg?.showToast() })
        }, onError = { showToast(R.string.toast_sign_in_fail) })
    }



}