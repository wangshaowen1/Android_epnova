package com.kuaigeglobal.epnova.core.manager

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.kuaigeglobal.epnova.core.Config
import java.util.Locale

object AppLanguageManager {

    fun saveLanguage(langCode: String) {
        Config.get().saveLanguage(langCode)
    }

    fun applySavedLanguage() {
        applyLanguage(Config.get().getSavedLanguage())
    }

    private fun applyLanguage(langCode: String) {
        val normalized = LocaleHelper.normalizeLanguageCode(langCode)
        Locale.setDefault(LocaleHelper.localeFromLanguageCode(normalized))
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(normalized))
    }
}
