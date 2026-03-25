@file:JvmName("StringUtils")

package com.kuaigeglobal.epnova.common.ext

import android.os.Build
import android.text.Html
import com.kuaigeglobal.epnova.widget.htmltag.SpanExtTagHandler
import com.nhsd.basic.ext.isNull

// 字符串处理扩展
fun String.toShort(maxLength: Int): String {
    if (length > maxLength) {
        return substring(0, maxLength).plus("…")
    }
    return this
}

fun String?.ellipse(maxLength: Int): String {
    this ?: return ""
    return if (length > maxLength) "${substring(0, maxLength)}..." else this
}

fun String?.clearBlank() = this?.replace("(?m)\\s*\\n\\s*\\n+".toRegex(), "\n")
    ?.replace("(?m)^\\s+".toRegex(), "")

fun String?.firstLine() = this?.split("\n")?.firstOrNull { it.isNotEmpty() }

fun String?.fromHtml(): CharSequence? {
    if (this.isNullOrEmpty()) {
        return ""
    }
    val text = this.replace("span", SpanExtTagHandler.CUSTOM_TAG, true)

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY, null, SpanExtTagHandler())
        } else {
            return Html.fromHtml(text)
        }
    } catch (e: Exception) {
    }
    return text
}

fun String.removeHttpUrlParam(vararg keys: String): String {
    if (!this.startsWith("http")) {
        return this
    }

    val urlParts = this.split("?", limit = 2)
    if (urlParts.size < 2) {
        return this
    }

    val baseUrl = urlParts[0]
    val queryString = urlParts[1]

    val params = queryString.split("&")
    val keysToRemove = keys.toSet() // 转换为 Set 提高查找效率
    val filteredParams = params.filterNot { param ->
        val (paramKey, _) = param.split("=", limit = 2)
        keysToRemove.contains(paramKey)
    }

    return if (filteredParams.isEmpty()) {
        baseUrl
    } else {
        "$baseUrl?${filteredParams.joinToString("&")}"
    }
}

fun String.getUrlFileExtension(): String {
    return substringBefore("?").substringAfterLast("/").substringAfterLast(".", "").substringBefore("#")
}

fun String.removePrefix(prefix: String): String = if (this == prefix) "" else this
fun String.removeLeadingZeros(): String = if (length > 1 && startsWith("0") && !startsWith("0.") && this[1].isDigit()) substring(1) else this
fun String.removeDuplicateDots(): String = if (count { it == '.' } > 1) {
    val firstDot = indexOf(".")
    substring(0, firstDot + 1) + substring(firstDot + 1).replace(".", "")
} else this

fun String.limitDecimalPlaces(maxPlaces: Int): String {
    if (!contains(".")) return this
    val parts = split(".")
    return if (parts[1].length > maxPlaces) {
        parts[0] + "." + parts[1].substring(0, maxPlaces)
    } else {
        this
    }
}


fun String?.remove(text: String) = this?.replace(text, "")

/**
 * 判断是否是汉字或字母或数字
 */
fun CharSequence.checkTrackTitle(): Boolean {
    val regex = "^[\\u4e00-\\u9fa5a-zA-Z0-9]+$".toRegex()
    return this.matches(regex)
}

fun Any ?.orEmpty(): String {
    return if (isNull()) "" else toString()
}