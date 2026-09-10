package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import kotlin.text.forEach

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:只允许输入中文汉字、英文字母和空格
 * @Data: 2025/6/5  13:38
 * @Version: 1.0
 * *****************************************
 */
class RestrictedEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    //特殊支持字符
    val specialCharacters = listOf('-', '_', '.',' ')
    private var restrictionWatcher: TextWatcher? = null


    /**
     * 设置输入过滤器
     * @param noSpace Boolean
     * @param maxLength Int
     * @return InputFilter
     */
    fun createInputFilter(noSpace: Boolean, maxLength: Int) {
        restrictionWatcher?.let { removeTextChangedListener(it) }
        restrictionWatcher = null
        if (noSpace) {
            textRestrictions()
        }
        filters = (filters.filterNot { it is InputFilter.LengthFilter } +
            InputFilter.LengthFilter(maxLength.coerceAtLeast(0))).toTypedArray()
    }

    private fun textRestrictions() {
        // 添加文本变化监听器执行额外过滤
        val watcher = object : TextWatcher {
            private var selfChange = false

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(editable: Editable?) {
                if (selfChange || editable == null) return

                val validChars = StringBuilder()
                editable?.forEach { char ->
                    if (isChineseCharacter(char)//中文汉字
                        || isEnglishLetter(char)//英文字母
                        || isDigit(char)//数字
                        || specialCharacters(char)//特殊字符
                    ) {
                        validChars.append(char)
                    }
                }

                val filteredText = validChars.toString()
                if (editable.toString() != filteredText) {
                    selfChange = true
                    try {
                        editable.replace(0, editable.length, filteredText)
                    } finally {
                        selfChange = false
                    }
                }
            }
        }
        restrictionWatcher = watcher
        addTextChangedListener(watcher)
    }


    // 检查是否是中文字符（简体中文和繁体中文）
    private fun isChineseCharacter(c: Char): Boolean {
        val ub = Character.UnicodeBlock.of(c)
        return ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
                ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
                ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B ||
                ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C ||
                ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D ||
                ub === Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
                ub === Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT
    }

    // 检查是否是英文字母（大小写）
    private fun isEnglishLetter(c: Char): Boolean {
        return (c in 'A'..'Z') || (c in 'a'..'z')
    }

    // 新增：检查是否是数字
    private fun isDigit(c: Char): Boolean {
        return c in '0'..'9'
    }

    //一些容许的特殊字符
    private fun specialCharacters(c: Char): Boolean {
        return specialCharacters.contains(c)
    }
}

