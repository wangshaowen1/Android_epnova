package com.kuaigeglobal.epnova.feature.login

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.Gravity
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.blankj.utilcode.util.ScreenUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.databinding.DialogPayPolicyBinding
import com.kuaigeglobal.epnova.feature.web.SimpleBrowserActivity
import com.nhsd.basic.BaseDialogFragment
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.setOnSingleClickListener


/**
 * 登录协议弹窗
 * @author xiaoowuu
 */
class PrivacyPolicyDialog : BaseDialogFragment<DialogPayPolicyBinding>() {

    companion object {
        fun show(manager: FragmentManager): PrivacyPolicyDialog {
            val dialog = PrivacyPolicyDialog()
            dialog.show(manager)
            return dialog
        }
    }


    //点击同意或不同意回调
    var callback: ((agree: Boolean) -> Unit)? = null


    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.attributes?.let { attr ->
                attr.width = (ScreenUtils.getScreenWidth() * 293 / 375f).toInt()
                attr.gravity = Gravity.CENTER
                dialogWindow.attributes = attr
            }
        }
    }


    @SuppressLint("SetTextI18n")
    override fun onPrepareView() {
        super.onPrepareView()
        binding.btnAgree.typeface = Typeface.DEFAULT_BOLD
        binding.btnReject.typeface = Typeface.DEFAULT_BOLD

        val text = SpannableStringBuilder("阅读并同意《用户协议》和《隐私政策》")
        text.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                SimpleBrowserActivity.launch(
                    this@PrivacyPolicyDialog.activity,
                    Config.get().serviceUrl,
                    R.string.tvSYUserService.getResString()
                )
            }

            override fun updateDrawState(ds: android.text.TextPaint) {
                ds.setColor(
                    ContextCompat.getColor(
                        requireActivity(),
                        R.color.text_color_1_dark
                    )
                )
            }
        }, 5, 11, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                SimpleBrowserActivity.launch(
                    this@PrivacyPolicyDialog.activity,
                    Config.get().privacyUrl,
                    R.string.tvSYPrivateService.getResString()
                )
            }

            override fun updateDrawState(ds: android.text.TextPaint) {
                ds.setColor(
                    ContextCompat.getColor(
                        requireActivity(),
                        R.color.text_color_1_dark
                    )
                )
            }
        }, 12, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.tvContent.movementMethod = LinkMovementMethod.getInstance()
        binding.tvContent.text = text

        binding.btnReject.text = "不同意"
        binding.btnAgree.text = "同意"

        binding.btnAgree.setOnSingleClickListener {
            dismiss()
            callback?.invoke(true)
        }
        binding.btnReject.setOnSingleClickListener {
            dismiss()
            callback?.invoke(false)
        }
    }
}