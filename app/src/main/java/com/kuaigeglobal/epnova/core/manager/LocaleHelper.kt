package com.kuaigeglobal.epnova.core.manager

import android.content.Context
import android.content.ContextWrapper
import java.util.Locale

object LocaleHelper {
    fun wrap(context: Context, language: String): ContextWrapper {
        val locale = Locale(language)
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