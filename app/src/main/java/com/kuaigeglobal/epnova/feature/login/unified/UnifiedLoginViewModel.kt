package com.kuaigeglobal.epnova.feature.login.unified

import androidx.databinding.ObservableBoolean
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.ext.loginToast
import com.kuaigeglobal.epnova.core.manager.AppsFlyerManager
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.core.network.apis.request.LoginByThirdBody
import com.kuaigeglobal.epnova.core.network.apis.response.UserHomeData
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.login.data.ThirdLoginRepository
import com.kuaigeglobal.epnova.kit.SignUtils
import com.nhsd.basic.ext.showToast

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
    val loginRewardsValid = User.get().isGuest
    val loginRewards = Config.get().loginRewards

    fun loginByThird(body: LoginByThirdBody, onLogin: ((UserHomeData?) -> Unit)? = null) {
        val loginMethod = when (body.loginType) {
            LoginByThirdBody.LOGIN_TYPE_GOOGLE -> FirebaseEvent.Value.LOGIN_GOOGLE
            else -> null
        }
        launchWithLoading (true, block = {
            response(mRepo.loginByThird(body), result = {
                loginMethod?.let {
                    reportLoginResult(it, FirebaseEvent.Value.RESULT_SUCCESS)
                }
                User.get().updateFrom(data)
                SignUtils.signIn()
                if (data?.isNewRegistration() == true) {
                    AppsFlyerManager.logCompleteRegistration()
                }
                data.loginToast()
                onLogin?.invoke(data)
            }, error = {
                loginMethod?.let {
                    reportLoginResult(it, FirebaseEvent.Value.RESULT_FAIL)
                }
                this?.msg?.showToast()
            })
        }, onError = {
            loginMethod?.let {
                reportLoginResult(it, FirebaseEvent.Value.RESULT_FAIL)
            }
            showToast(R.string.toast_sign_in_fail)
        })
    }

    private fun reportLoginResult(loginMethod: String, result: String) {
        EventManager.onFirebaseTrack(
            FirebaseEvent.Category.LOGIN,
            FirebaseEvent.Action.LOGIN_RESULT,
            loginMethod,
            result
        )
    }

}
