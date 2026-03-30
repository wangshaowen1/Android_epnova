package com.kuaigeglobal.epnova.feature.login

import android.graphics.Color
import android.graphics.Typeface
import androidx.databinding.ObservableBoolean
import com.drake.spannable.addSpan
import com.drake.spannable.span.HighlightSpan
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.getInstall
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.ext.loginToast
import com.kuaigeglobal.epnova.core.network.apis.request.LoginByThirdBody
import com.kuaigeglobal.epnova.core.network.apis.response.UserHomeData
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.login.data.ThirdLoginRepository
import com.kuaigeglobal.epnova.feature.settings.SettingsActivity
import com.kuaigeglobal.epnova.feature.web.SimpleBrowserActivity
import com.kuaigeglobal.epnova.kit.SignUtils
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.getThemeColor
import com.nhsd.basic.ext.showToast

/**
 * 第三方登录
 */
class LoginViewModel : AppRepositoryViewModel<ThirdLoginRepository>() {

    /**
     * 协议是否勾选
     */
    val checkedPolicy by lazy { ObservableBoolean(false) }
    val policyText by lazy {
        R.string.i_have_read_and_agree_to.getResString()
            .addSpan(R.string.tvUserService.getResString(), HighlightSpan(topActivity?.getThemeColor(R.attr.textColor1), Typeface.DEFAULT_BOLD) {
                val activity = topActivity ?: return@HighlightSpan
                SimpleBrowserActivity.launch(activity, Config.get().serviceUrl, R.string.tvSYUserService.getResString())
            })
            .addSpan("、")
            .addSpan(R.string.tvPrivateService.getResString(), HighlightSpan(topActivity?.getThemeColor(R.attr.textColor1), Typeface.DEFAULT_BOLD) {
                val activity = topActivity ?: return@HighlightSpan
                SimpleBrowserActivity.launch(activity, Config.get().privacyUrl, R.string.tvSYPrivateService.getResString())
            })
            //不加这个空格，TextView点击会跳协议
            .addSpan(" ")
    }

    /**
     * 登录
     * @param loginType 3-微信登录 4-qq登录 5-苹果登录
     * @param map 第三方授权信息
     */
    fun loginByThird(loginType: Int, map: MutableMap<String, String>?, onLogin: ((UserHomeData?) -> Unit)?) {
        setLoading(true)
        getInstall {
            launchWithLoading(false, block = {
                val body = LoginByThirdBody(
                    loginType = loginType,
                    identityToken="",
                    inviteData = Config.get().installData
                )
                response(mRepo.loginByThird(body), result = {
                    User.get().updateFrom(data)
                    SignUtils.signIn()
                    data.loginToast()
                    onLogin?.invoke(data)
                }, error = { this?.msg?.showToast() })
            }, onError = { showToast(R.string.toast_sign_in_fail) })
        }
    }

    fun checkPolicy(){
        checkedPolicy.set(checkedPolicy.get().not())
    }

    fun debugToSettings(){
        startActivity(SettingsActivity::class.java)
    }
}