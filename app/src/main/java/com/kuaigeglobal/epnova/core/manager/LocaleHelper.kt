package com.kuaigeglobal.epnova.core.manager

import java.util.Locale

object LocaleHelper {
    const val LANGUAGE_EN_US = "en-US" // 英语
    const val LANGUAGE_DE_DE = "de-DE" // 德语
    const val LANGUAGE_ES_ES = "es-ES" // 西班牙语
    const val LANGUAGE_FR_FR = "fr-FR" // 法语
    const val LANGUAGE_ID_ID = "id-ID" // 印尼语
    const val LANGUAGE_JA_JA = "ja-JP" // 日语
    const val LANGUAGE_PT_BR = "pt-BR" // 葡萄牙语

    /** 支持的后台语言标识，与 LanguagesViewModel.languagesConfigs 保持一致 */
    private val supportedLanguages = setOf(LANGUAGE_EN_US, LANGUAGE_DE_DE, LANGUAGE_ES_ES, LANGUAGE_FR_FR, LANGUAGE_ID_ID, LANGUAGE_JA_JA, LANGUAGE_PT_BR)

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
            "de" -> LANGUAGE_DE_DE
            "es" -> LANGUAGE_ES_ES
            "fr" -> LANGUAGE_FR_FR
            // Android 历史上可能返回 in，后台统一使用 id-ID。
            "id", "in" -> LANGUAGE_ID_ID
            "ja" -> LANGUAGE_JA_JA
            "pt" -> LANGUAGE_PT_BR
            else -> LANGUAGE_EN_US
        }
        return if (normalized in supportedLanguages) normalized else LANGUAGE_EN_US
    }

    fun shortLangCode(langCode: String?): String {
        val normalized = normalizeLanguageCode(langCode)
        val parts = normalized.split("-", limit = 2)
        return parts[0]
    }

    fun localeFromLanguageCode(langCode: String?): Locale {
        val normalized = normalizeLanguageCode(langCode)
        val parts = normalized.split("-", limit = 2)
        return Locale(parts[0], parts.getOrElse(1) { "" })
    }
}
