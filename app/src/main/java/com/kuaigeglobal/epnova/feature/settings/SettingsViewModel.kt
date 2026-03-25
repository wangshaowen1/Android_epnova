package com.kuaigeglobal.epnova.feature.settings

import android.graphics.Color
import androidx.databinding.ObservableField
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import com.kuaigeglobal.epnova.core.response
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResString

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

    /**
     * 退出登录
     */
    fun logoutApp() {
        launch { response(mRepo.api.logoutApp(UserTokenBody())) {} }
    }

    override fun onResume() {
        super.onResume()
        phone.set(User.get().phoneNum)
    }
}