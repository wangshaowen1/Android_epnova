package com.kuaigeglobal.epnova.feature.settings

import android.graphics.Color
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.ext.parseMessage
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.settings.lang.languagesConfigs
import com.kuaigeglobal.epnova.kit.SignUtils
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:设置
 * @Data: 2025/4/18  16:04
 * @Version: 1.0
 * *****************************************
 */
class SettingsViewModel : AppRepositoryViewModel<AppRepository>() {
    val mToolbarVM = ToolbarViewModel(
        navigationAction = { finish() },
        title = R.string.title_settings.getResString(),
        background = Color.TRANSPARENT,
    )


    val debugIp = ObservableField<String>()

    var phone = ObservableField(User.get().phoneNum)

    val beiAn by lazy { Config.get().appConfig.beianNo }

    val language by lazy { languagesConfigs.find { it.code == Config.get().getSavedLanguage() } }
    val episodeAutoUnlock = ObservableField(Config.get().isEpisodeAutoUnlock())
    val isRealLogin = ObservableBoolean(User.get().isLogin)

    /**
     * 退出登录
     * @param autoSignOut 是否在接口成功后自动执行本地 signOut，默认 true
     */
    fun logoutApp(autoSignOut: Boolean = true, onSuccess: (() -> Unit)? = null) {
        setLoading(true)
        launch {
            try {
                response(mRepo.api.logoutApp(UserTokenBody()), {
                    if (autoSignOut) SignUtils.signOut()
                    onSuccess?.invoke()
                }, {
                    setLoading(false)
                })
            } catch (e: Exception) {
                // HTTP 401：token 已过期，服务端本就视为未登录，与退出的目标一致，直接继续执行
                // 其他网络异常：提示用户
                if ((e as? HttpException)?.code() == ApiException.AUTHORIZATION_ERROR) {
                    if (autoSignOut) SignUtils.signOut()
                    onSuccess?.invoke()
                } else {
                    setLoading(false)
                    parseMessage(e)?.showToast()
                }
            }
        }
    }

    fun guestLogin() {
        // 先调退出登录接口，但不执行本地 signOut
        // 等游客登录成功后再覆写用户数据，避免游客登录失败导致本地状态丢失
        logoutApp(autoSignOut = false) {
            Global.guestLogin({ userData ->
                // 成功：直接覆写用户数据（不触发 SIGN_OUT 事件影响其他页面）
                launch {
                    withContext(Dispatchers.IO) {
                        User.get().updateFrom(userData)
                        SignUtils.signIn()
                    }
                    showToast(R.string.app_log_out)
                    finish()
                }
            }, {
                // 失败：服务端 token 已失效，清理本地状态并引导重新登录
                setLoading(false)
                SignUtils.signOut()
                parseMessage(it)?.showToast()
            })
        }
    }

    override fun onResume() {
        super.onResume()
        phone.set(User.get().phoneNum)
        isRealLogin.set(User.get().isLogin)
    }


}