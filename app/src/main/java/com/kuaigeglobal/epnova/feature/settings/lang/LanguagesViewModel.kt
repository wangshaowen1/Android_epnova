package com.kuaigeglobal.epnova.feature.settings.lang

import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.manager.LocaleHelper
import com.kuaigeglobal.epnova.core.ext.request
import com.kuaigeglobal.epnova.feature.settings.lang.data.LanguagesRepository
import com.nhsd.basic.ext.getResString

data class LanguageItem(val code: String, val name: String)

val languagesConfigs = listOf(
    LanguageItem(LocaleHelper.LANGUAGE_EN_US, "English"),
    LanguageItem(LocaleHelper.LANGUAGE_TH_TH, "ภาษาไทย"),
    LanguageItem(LocaleHelper.LANGUAGE_ID_ID, "Bahasa Indonesia")
)

class LanguagesViewModel : AppRepositoryViewModel<LanguagesRepository>() {

    var selectedCode = Config.get().getSavedLanguage()

    val doneEvent = MutableLiveData<Unit>()

    val mToolbarVM = ToolbarViewModel(
        navigationAction = { finish() },
        title = R.string.settings_language.getResString(),
        background = Color.TRANSPARENT,
        menuText = R.string.complete.getResString(),
        menuTextColorRes = R.color.color_ff2e55,
        menuAction = { onDone() }
    )

    private fun onDone() {
        Config.get().saveLanguage(selectedCode)
        //服务端语言必须保存成功后再执行doneEvent.value = Unit
        request(
            apiBlock = { mRepo.setAppLanguage() },
            showToast = true
        ) {
            doneEvent.value = Unit
        }
    }
}
