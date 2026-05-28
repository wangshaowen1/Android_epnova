package com.kuaigeglobal.epnova.core.manager

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import java.util.Locale

object LocaleHelper {
    const val LANGUAGE_EN_US = "en-US"
    const val LANGUAGE_TH_TH = "th-TH"
    const val LANGUAGE_ID_ID = "id-ID"

    /** 支持的后台语言标识，与 LanguagesViewModel.languagesConfigs 保持一致 */
    private val supportedLanguages = setOf(LANGUAGE_EN_US, LANGUAGE_TH_TH, LANGUAGE_ID_ID)

    /**
     * 解析默认语言：只支持当前后台约定的语言区域码，不支持的系统语言回退英文。
     */
    fun resolveDefaultLanguage(): String {
        val systemLocale = Locale.getDefault()
        val systemTag = listOf(systemLocale.language, systemLocale.country)
            .filter { it.isNotEmpty() }
            .joinToString("-")
        return normalizeLanguageCode(systemTag)
    }

    fun normalizeLanguageCode(langCode: String?): String {
        val tag = langCode
            ?.trim()
            ?.replace('_', '-')
            ?.takeIf { it.isNotEmpty() }
            ?: return LANGUAGE_EN_US
        val language = tag.substringBefore("-").lowercase(Locale.US)
        val normalized = when (language) {
            "en" -> LANGUAGE_EN_US
            "th" -> LANGUAGE_TH_TH
            // Android 历史上可能返回 in，后台统一使用 id-ID。
            "id", "in" -> LANGUAGE_ID_ID
            else -> LANGUAGE_EN_US
        }
        return if (normalized in supportedLanguages) normalized else LANGUAGE_EN_US
    }

    fun localeFromLanguageCode(langCode: String?): Locale {
        val normalized = normalizeLanguageCode(langCode)
        val parts = normalized.split("-", limit = 2)
        return Locale(parts[0], parts.getOrElse(1) { "" })
    }

    fun updateResources(resources: Resources, language: String?) {
        val locale = localeFromLanguageCode(language)
        Locale.setDefault(locale)

        val configuration = resources.configuration
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    fun wrap(context: Context, language: String): ContextWrapper {
        val locale = localeFromLanguageCode(language)
        Locale.setDefault(locale)
        
        val resources = context.resources
        val configuration = resources.configuration
        
        // 核心：设置新语言
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale) // 支持从右往左的布局（虽然泰/印尼不需要）

        val newContext = context.createConfigurationContext(configuration)
        return ContextWrapper(newContext)
    }
}
