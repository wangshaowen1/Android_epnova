package com.kuaigeglobal.epnova.widget

import android.content.Context
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialog
import androidx.core.view.isVisible
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.databinding.DialogLoadingBinding

class GeneratingDialog @JvmOverloads constructor(
    context: Context,
    title: CharSequence? = null,
    tips: CharSequence? = null,
    backEnable: Boolean = false
) : AppCompatDialog(context, R.style.Dialog_Common) {

    private val binding = DialogLoadingBinding.inflate(layoutInflater)

    init {
        setContentView(binding.root)
        setCanceledOnTouchOutside(false)
        setCancelable(backEnable)
        updateContent(title, tips)
    }

    override fun onStart() {
        super.onStart()
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    /**
     * 暴露给外部调用的更新方法
     */
    fun updateContent(title: CharSequence? = null, tips: CharSequence? = null) {
        binding.title.text = title
        binding.title.isVisible = !title.isNullOrEmpty()

        binding.tips.text = tips
        binding.tips.isVisible = !tips.isNullOrEmpty()
    }
}
