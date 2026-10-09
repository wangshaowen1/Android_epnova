package com.kuaigeglobal.epnova.core.manager

import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.core.manager.LocaleHelper.LANGUAGE_DE_DE
import com.kuaigeglobal.epnova.core.manager.LocaleHelper.LANGUAGE_EN_US
import com.kuaigeglobal.epnova.core.manager.LocaleHelper.LANGUAGE_ES_ES
import com.kuaigeglobal.epnova.core.manager.LocaleHelper.LANGUAGE_FR_FR
import com.kuaigeglobal.epnova.core.manager.LocaleHelper.LANGUAGE_ID_ID
import com.kuaigeglobal.epnova.core.manager.LocaleHelper.LANGUAGE_JA_JA
import com.kuaigeglobal.epnova.core.manager.LocaleHelper.LANGUAGE_PT_PT
import com.kuaigeglobal.epnova.feature.settings.lang.LanguageItem
import com.nhsd.basic.ext.getResString

/** 国际化 - App适配语言
 * @createtime 2026年10月09日 10:47
 * @author TooWhiteT
 */

object AppSupportLanguageManager {

    val languagesConfigs by lazy {
        listOf(
            LanguageItem(LANGUAGE_EN_US, R.string.setting_language_en.getResString()),
            LanguageItem(LANGUAGE_DE_DE, R.string.setting_language_de.getResString()),
            LanguageItem(LANGUAGE_ES_ES, R.string.setting_language_es.getResString()),
            LanguageItem(LANGUAGE_FR_FR, R.string.setting_language_fr.getResString()),
            LanguageItem(LANGUAGE_ID_ID, R.string.setting_language_id.getResString()),
            LanguageItem(LANGUAGE_JA_JA, R.string.setting_language_ja.getResString()),
            LanguageItem(LANGUAGE_PT_PT, R.string.setting_language_pt.getResString())
        )
    }

    /**
     * 防止重启App后缓存是上次的记录
     */
    fun reloadLanguagesConfig() {
        languagesConfigs.forEach {
            it.name = codeToName(it.code)
        }
    }

    private fun codeToName(code: String): String = when (code) {
        LANGUAGE_DE_DE -> R.string.setting_language_de.getResString()
        LANGUAGE_ES_ES -> R.string.setting_language_es.getResString()
        LANGUAGE_FR_FR -> R.string.setting_language_fr.getResString()
        LANGUAGE_ID_ID -> R.string.setting_language_id.getResString()
        LANGUAGE_JA_JA -> R.string.setting_language_ja.getResString()
        LANGUAGE_PT_PT -> R.string.setting_language_pt.getResString()
        else -> R.string.setting_language_en.getResString()
    }

}