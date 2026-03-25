package com.kuaigeglobal.epnova.common.ext

import android.graphics.Typeface
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.widget.TextView
import com.kuaigeglobal.epnova.core.network.apis.response.Role
import com.kuaigeglobal.epnova.widget.editor.RoleSpan
import com.nhsd.basic.ext.getResColor


// TextView扩展
fun TextView.setScale(ratio: Float) {
    this.scaleX = ratio
    this.scaleY = ratio
}

fun TextView.scaleTextSize(ratio: Float, smallTextSize: Int = 18, bigTextSize: Int = 20) {
    setTextSize(TypedValue.COMPLEX_UNIT_SP, smallTextSize + (bigTextSize - smallTextSize) * ratio)
}

fun TextView.setColoredText(text: String, span: String, color: Int) {
    setText(getColoredSpanText(text, span, color))
}

fun TextView.setColoredText(text: String, span: String, color: Int, bold: Boolean) {
    setText(getColoredSpanText(text, span, color, bold))
}

fun TextView.setColoredText(text: String, span: String, textSize: Int, color: Int, bold: Boolean) {
    setText(getColoredSpanText(text, span, textSize, color, bold))
}

fun TextView.setTextColorRes(resId: Int) = setTextColor(resId.getResColor())
fun TextView.setTextColorListRes(resId: Int) = setTextColor(context.getColorStateList(resId))


/** 只改变颜色 */
fun getColoredSpanText(text: String, spanText: String, color: Int): SpannableString {
    return SpannableString(text).apply {
        val start = text.indexOf(spanText)
        if (start != -1) {
            setSpan(
                ForegroundColorSpan(color),
                start, start + spanText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
}

/** 性别标识：front 和 back 两个位置加颜色+粗体 */
fun getColoredSpanGenderText(text: String, front: String, back: String, color: Int): SpannableString {
    return SpannableString(text).apply {

        val frontIndex = text.indexOf(front)
        if (frontIndex != -1) {
            setSpan(ForegroundColorSpan(color), frontIndex, frontIndex + front.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(StyleSpan(Typeface.BOLD), frontIndex, frontIndex + front.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        }

        val backIndex = text.lastIndexOf(back)
        if (backIndex != -1) {
            setSpan(ForegroundColorSpan(color), backIndex, backIndex + back.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(StyleSpan(Typeface.BOLD), backIndex, backIndex + back.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        }
    }
}

/** 颜色 + 字号 */
fun getColoredSpanText(text: String, spanText: String, color: Int, textSizeDip: Int): SpannableString {
    return getColoredSpanText(text, spanText, color, textSizeDip, false)
}

/** 颜色 + 字号 + 可选粗体 */
fun getColoredSpanText(
    text: String,
    spanText: String,
    color: Int,
    textSizeDip: Int,
    bold: Boolean
): SpannableString {
    return SpannableString(text).apply {
        val start = text.indexOf(spanText)
        if (start != -1) {
            val end = start + spanText.length
            setSpan(ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(AbsoluteSizeSpan(textSizeDip, true), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (bold) {
                setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }
        }
    }
}

/** 颜色 + 可选粗体 */
fun getColoredSpanText(text: String, spanText: String, color: Int, bold: Boolean): SpannableString {
    return SpannableString(text).apply {
        val start = text.indexOf(spanText)
        if (start != -1) {
            val end = start + spanText.length
            setSpan(ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (bold) {
                setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }
        }
    }
}

/**
 * 角色标签解析 <role:@张三> => 张三
 */
fun parseRoleTagsModernPlain(textWithTags: String?): SpannableStringBuilder? {
    if (textWithTags.isNullOrEmpty()) return null

    val regex = Regex("<role:@(.*?)>")
    val spannable = SpannableStringBuilder()
    var lastIndex = 0

    for (match in regex.findAll(textWithTags)) {
        val startIndex = match.range.first
        if (startIndex > lastIndex) {
            spannable.append(textWithTags.substring(lastIndex, startIndex))
        }
        val name = match.groupValues[1]
        spannable.append(name)
        lastIndex = match.range.last + 1
    }

    if (lastIndex < textWithTags.length) {
        spannable.append(textWithTags.substring(lastIndex))
    }

    return spannable
}

/**
 * 角色标签解析，带RoleSpan字颜色之类的 <role:@张三> => @张三
 */
fun parseRoleTagsModern(textWithTags: String?, roles: List<Role>? = null): SpannableStringBuilder? {
    if (textWithTags.isNullOrEmpty()) {
        return null
    }
    val regex = Regex("<role:@(.*?)>")
    val spannable = SpannableStringBuilder()
    var lastIndex = 0

    for (match in regex.findAll(textWithTags)) {
        val startIndex = match.range.first
        if (startIndex > lastIndex) {
            spannable.append(textWithTags.substring(lastIndex, startIndex))
        }
        val name = match.groupValues[1]
        val role = roles?.find { it.roleName == name } ?: Role("", name)
        val roleText = "@$name"
        val start = spannable.length
        spannable.append(roleText)
        spannable.setSpan(RoleSpan(role), start, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        lastIndex = match.range.last + 1
    }

    if (lastIndex < textWithTags.length) {
        spannable.append(textWithTags.substring(lastIndex))
    }
    return spannable
}

/** 解析服务器格式文本并渲染 */
fun TextView.setTextWithRoles(textWithTags: String, roles: List<Role>? = null) {
    text = parseRoleTagsModern(textWithTags = textWithTags, roles = roles)
}
