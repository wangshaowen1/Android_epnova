package com.kuaigeglobal.epnova.widget

import android.text.InputFilter
import android.text.Spanned
import java.util.regex.Pattern

/**
 * @param maxIntegerDigits 整数部分最大位数 (例如 2)
 * @param maxDecimalDigits 小数部分最大位数 (例如 2)
 */
class DecimalInputFilter(
    private val maxIntegerDigits: Int,
    private val maxDecimalDigits: Int
) : InputFilter {

    // 正则表达式逻辑：
    // ^\\d{0,M}             -> 匹配 0 到 M 位整数
    // (\\.\\d{0,N})?        -> 匹配可选的小数点和 0 到 N 位小数
    private val pattern = Pattern.compile(
        "^\\d{0,${maxIntegerDigits.coerceAtLeast(0)}}" +
            if (maxDecimalDigits > 0) "(\\.\\d{0,$maxDecimalDigits})?$" else "$"
    )

    override fun filter(
        source: CharSequence, // 新输入的字符串
        start: Int,           // 新输入的起始位置
        end: Int,             // 新输入的结束位置
        dest: Spanned,        // 原有的文本内容
        dstart: Int,          // 替换的起始位置
        dend: Int             // 替换的结束位置
    ): CharSequence? {
        
        // 1. 拼接出"如果这次输入成功后"的完整字符串
        val builder = StringBuilder(dest)
        builder.replace(dstart, dend, source.subSequence(start, end).toString())
        val result = builder.toString()

        // 2. 特殊处理：如果用户第一位输入小数点 "."，自动补全为 "0."
        if (result == "." && start < end && dstart == 0 && dend == dest.length &&
            maxIntegerDigits > 0 && maxDecimalDigits > 0
        ) {
            return "0."
        }
        
        // 3. 使用正则验证完整字符串是否符合格式
        return if (pattern.matcher(result).matches()) {
            null // 返回 null 表示接受输入
        } else {
            // 拒绝选区替换或删除时保留原片段，返回空串会误删选中的内容。
            dest.subSequence(dstart, dend)
        }
    }
}
