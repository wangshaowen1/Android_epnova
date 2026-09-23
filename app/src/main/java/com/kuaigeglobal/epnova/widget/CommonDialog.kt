package com.kuaigeglobal.epnova.widget

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.OnBackPressedCallback
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatDialog
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.databinding.DialogCommonBinding
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener

class CommonDialog @JvmOverloads constructor(context: Context, content: CharSequence? = null) :
    AppCompatDialog(context, R.style.Dialog_Common) {
    private val binding = DialogCommonBinding.inflate(layoutInflater)
    var onPositiveButtonClickListener: ((Dialog) -> Unit)? = null
    var onCancelButtonClickListener: ((Dialog) -> Unit)? = null
    var onGuideClickListener: ((Dialog) -> Unit)? = null

    constructor(context: Context, resId: Int) : this(context, context.getString(resId))


    private var backEnable: Boolean = true

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (backEnable) {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
    }

    init {
        setContentView(binding.root)
        setCanceledOnTouchOutside(backEnable)
        content?.let { setContent(it) }
        binding.confirm.setOnSingleClickListener {
            this@CommonDialog.dismiss()
            onPositiveButtonClickListener?.invoke(this@CommonDialog)
        }
        binding.cancel.setOnSingleClickListener {
            onCancelButtonClickListener?.invoke(this@CommonDialog)
            this@CommonDialog.cancel()
        }
        binding.guideTips.setOnSingleClickListener{
            onGuideClickListener?.invoke(this@CommonDialog)
        }
        binding.close.setOnClickListener { dismiss() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    fun setBackEnable(backEnable: Boolean) {
        this.backEnable = backEnable
    }


    fun setTitle(title: String?) {
        binding.title.visibility = View.VISIBLE
        binding.title.text = title
    }

    fun setContent(content: CharSequence?) {
        binding.content.visibility = View.VISIBLE
        binding.content.text = content
    }

    fun setContent(content: SpannableString?) {
        binding.content.visibility = View.VISIBLE
        binding.content.text = content
        binding.content.movementMethod = LinkMovementMethod.getInstance()
    }

    fun setContent(content: Spanned?) {
        binding.content.text = content
    }

    fun setContent(contentResId: Int) {
        binding.content.setText(contentResId)
    }
    fun setGuideTips(content: CharSequence?) {
        binding.guideTips.visibility = View.VISIBLE
        binding.guideTips.text = content
    }

    fun setConfirmStyle(confirmStyle: Boolean) {
        if (confirmStyle) {
            binding.cancel.visibility = View.GONE
            val params:MarginLayoutParams=binding.llBottom.layoutParams as MarginLayoutParams
            params.marginStart=33.dp
            params.marginEnd=33.dp

        } else {
            binding.cancel.visibility = View.VISIBLE
            val params:MarginLayoutParams=binding.llBottom.layoutParams as MarginLayoutParams
            params.marginStart=14.dp
            params.marginEnd=14.dp
        }
    }


    fun setContentGravity(gravity: Int) {
        binding.content.gravity = gravity
    }

    fun setPositiveButtonText(text: String?) {
        binding.confirm.text = text
    }

    fun setPositiveButtonEnable(enable: Boolean) {
        binding.confirm.isEnabled = enable
    }

    fun setPositiveButtonTextColor(textColor: Int) {
        binding.confirm.setTextColor(textColor)
    }

    fun setConfirmButtonBackgroundResource(@DrawableRes resId: Int) {
        binding.confirm.setBackgroundResource(resId)
    }

    fun setPositiveButtonText(resId: Int) {
        binding.confirm.setText(resId)
    }

    fun setCancelButtonText(text: String?) {
        binding.cancel.text = text
    }
}
