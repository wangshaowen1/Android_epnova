package com.kuaigeglobal.epnova.core.manager

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.kuaigeglobal.epnova.core.Config
import com.nhsd.basic.locale.BasicLocaleConfig
import java.util.Locale

object AppLanguageManager {

    /**
     * 接入 baselibrary 的统一语言入口。
     *
     * EpNova 保存的是后台约定语言码，例如 en-US/id-ID；这里统一映射成 Locale。
     * 公共库负责在 BaseApp/BaseActivity/getResString/T 等资源读取入口应用该 Locale，
     * 避免各页面自行 updateConfiguration 或 overrideConfiguration 导致 ROM 重建循环。
     */
    fun configureBasicLocale() {
        BasicLocaleConfig.configure(
            BasicLocaleConfig.Options(
                enabled = true,
                localeProvider = BasicLocaleConfig.LocaleProvider { context ->
                    LocaleHelper.localeFromLanguageCode(Config.readSavedLanguage(context))
                }
            )
        )
    }

    fun saveLanguage(langCode: String) {
        Config.get().saveLanguage(langCode)
    }

    fun applySavedLanguage() {
        configureBasicLocale()
        applyLanguage(Config.get().getSavedLanguage())
    }

    private fun applyLanguage(langCode: String) {
        val normalized = LocaleHelper.normalizeLanguageCode(langCode)
        Locale.setDefault(LocaleHelper.localeFromLanguageCode(normalized))
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(normalized))
    }
}
