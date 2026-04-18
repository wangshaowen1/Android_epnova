package com.kuaigeglobal.epnova.feature.settings.lang

import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.Config
import com.nhsd.basic.ext.getResString

data class LanguageItem(val code: String, val name: String)

val languagesConfigs = listOf(
    LanguageItem("en", "English"),
    LanguageItem("th", "ภาษาไทย"),
    LanguageItem("id", "Bahasa Indonesia")
)

class LanguagesViewModel : AppRepositoryViewModel<AppRepository>() {

    var selectedCode = Config.get().getSavedLanguage()

    val doneEvent = MutableLiveData<Unit>()

    val mToolbarVM = ToolbarViewModel(
        navigationAction = { finish() },
        title = R.string.title_language.getResString(),
        background = Color.TRANSPARENT,
        menuText = R.string.complete.getResString(),
        menuTextColorRes = R.color.color_ff2e55,
        menuAction = { onDone() }
    )

    private fun onDone() {
        Config.get().saveLanguage(selectedCode)
        doneEvent.value = Unit
    }
}
