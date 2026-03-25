package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet

/**
 * 歌词编辑控件
 */
class RecyclerEditText : XEditText {

    var onTextInputListener: OnTextInputListener? = null
    private val textWatcher by lazy {
        object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                onTextInputListener?.onTextInput(s.toString())
            }
        }
    }

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        setNoNextLine(true)
        removeTextChangedListener(textWatcher)
        addTextChangedListener(textWatcher)
    }

    interface OnTextInputListener {
        fun onTextInput(text: String)
    }
}