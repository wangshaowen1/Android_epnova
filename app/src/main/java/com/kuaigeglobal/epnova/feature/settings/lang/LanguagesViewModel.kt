package com.kuaigeglobal.epnova.feature.settings.lang

import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.manager.AppLanguageManager
import com.kuaigeglobal.epnova.core.manager.LocaleHelper
import com.kuaigeglobal.epnova.core.ext.request
import com.kuaigeglobal.epnova.feature.settings.lang.data.LanguagesRepository
import com.nhsd.basic.ext.getResString

data class LanguageItem(val code: String, val name: String)

val languagesConfigs = listOf(
    LanguageItem(LocaleHelper.LANGUAGE_EN_US, "English"),
    LanguageItem(LocaleHelper.LANGUAGE_DE_DE, "German"),
    LanguageItem(LocaleHelper.LANGUAGE_ES_ES, "Spanish"),
    LanguageItem(LocaleHelper.LANGUAGE_FR_FR, "French"),
    LanguageItem(LocaleHelper.LANGUAGE_ID_ID, "Bahasa Indonesia"),
    LanguageItem(LocaleHelper.LANGUAGE_JA_JA, "Japanese"),
    LanguageItem(LocaleHelper.LANGUAGE_PT_PT, "Portuguese")
)

class LanguagesViewModel : AppRepositoryViewModel<LanguagesRepository>() {

    private val currentCode = Config.get().getSavedLanguage()
    private val doneText = R.string.complete.getResString()

    var selectedCode = currentCode
        private set

    val doneEvent = MutableLiveData<Unit>()

    val mToolbarVM = ToolbarViewModel(
        navigationAction = { finish() },
        title = R.string.settings_language.getResString(),
        background = Color.TRANSPARENT,
        menuText = null,
        menuTextColorRes = R.color.color_ff2e55,
        menuAction = { onDone() }
    )

    fun selectLanguage(code: String) {
        selectedCode = LocaleHelper.normalizeLanguageCode(code)
        updateDoneVisible()
    }

    private fun updateDoneVisible() {
        mToolbarVM.mMenuText.set(if (selectedCode == currentCode) null else doneText)
    }

    private fun onDone() {
        if (selectedCode == currentCode) return
        AppLanguageManager.saveLanguage(selectedCode)
        //服务端语言必须保存成功后再执行doneEvent.value = Unit
        request(
            apiBlock = { mRepo.setAppLanguage() },
            showToast = true
        ) {
            AppLanguageManager.applySavedLanguage()
            doneEvent.value = Unit
        }
    }
}
