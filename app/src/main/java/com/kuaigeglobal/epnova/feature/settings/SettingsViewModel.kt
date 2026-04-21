package com.kuaigeglobal.epnova.feature.settings

import android.graphics.Color
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
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.settings.lang.languagesConfigs
import com.kuaigeglobal.epnova.kit.SignUtils
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

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


    /**
     * 退出登录
     */
    fun logoutApp() {
        launch { response(mRepo.api.logoutApp(UserTokenBody())) {} }
    }

    fun guestLogin() {
        setLoading(true)
        Global.guestLogin({
            setLoading(false)
            logoutApp()
            launch {
                withContext(Dispatchers.IO) {
                    SignUtils.signOut()
                    delay(200)
                    User.get().updateFrom(it)
                    SignUtils.signIn()
                }
                showToast(R.string.app_log_out)
                finish()
            }
        }, {
            setLoading(false)
            parseMessage(it)?.showToast()
        })
    }

    override fun onResume() {
        super.onResume()
        phone.set(User.get().phoneNum)
    }


}