package com.kuaigeglobal.epnova.common.utils

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import com.kuaigeglobal.epnova.R
import com.nhsd.basic.ext.getResColor
import java.util.regex.Pattern

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:敏感词高亮工具类
 * @Data: 2026/2/28/周六  15:39
 * @Version: 1.0
 * *****************************************
 */
object SensitiveHighlighter {

    fun highlight(
        text: String?,
        keywords: List<String>?,
        color: Int = R.color.func_failure_dark.getResColor(),
    ): SpannableStringBuilder {
        if (text.isNullOrEmpty() || keywords.isNullOrEmpty()) {
            return SpannableStringBuilder(text ?: "")
        }
        val spannable = SpannableStringBuilder(text)

        // 定义“干扰字符”正则：匹配任意非字母、非数字的字符（涵盖了所有中英文标点、符号、空格和换行）
        val noisePattern = "[^\\p{L}\\p{N}]*"

        keywords
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { keyword ->
                // 1. 过滤掉 API 敏感词本身可能携带的干扰符号，只保留纯文字或数字
                val validChars = keyword.filter { it.isLetterOrDigit() }
                if (validChars.isEmpty()) return@forEach

                // 2. 将敏感词拆解，并在每个字符之间插入“干扰字符”正则
                // 例如 "分裂中国" 会变成: 分[^\\p{L}\\p{N}]*裂[^\\p{L}\\p{N}]*中[^\\p{L}\\p{N}]*国
                val regexPattern = validChars
                    .map { Pattern.quote(it.toString()) } // 防御：对特殊字符转义
                    .joinToString(separator = noisePattern)

                // 3. 构建正则对象，忽略大小写
                val regex = Regex(regexPattern, RegexOption.IGNORE_CASE)

                // 4. 在原文本中查找所有匹配项并高亮
                regex.findAll(text).forEach { matchResult ->
                    spannable.setSpan(
                        ForegroundColorSpan(color),
                        matchResult.range.first,       // 起始索引（包含）
                        matchResult.range.last + 1,    // 结束索引（排除，Kotlin Range 是闭区间，所以需要 +1）
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        return spannable
    }
}