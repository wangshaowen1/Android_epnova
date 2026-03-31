package com.kuaigeglobal.epnova.feature.splash

import android.app.Dialog
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Gravity
import com.blankj.utilcode.util.ScreenUtils
import com.drake.spannable.addSpan
import com.drake.spannable.replaceSpan
import com.drake.spannable.span.HighlightSpan
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.databinding.singleClick
import com.kuaigeglobal.epnova.databinding.DialogPolicyBinding
import com.kuaigeglobal.epnova.feature.web.SimpleBrowserActivity
import com.nhsd.basic.BaseDialogFragment
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.getThemeColor
import kotlin.math.min


/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:隐私政策和用户协议
 * @Data: 2025/9/9/周二  15:33
 * @Version: 1.0
 * *****************************************
 */
class PolicyDialog : BaseDialogFragment<DialogPolicyBinding>() {

    private var highlightColor = R.color.theme_color_2_dark.getResColor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
        activity?.getThemeColor(R.attr.themeColor2)?.let { highlightColor = it }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.attributes?.let { attr ->
                val screenWidth = min(ScreenUtils.getScreenWidth(), ScreenUtils.getScreenHeight())
                attr.width = (293 / 375f * screenWidth).toInt()
                attr.gravity = Gravity.CENTER
                dialogWindow.attributes = attr
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext(), R.style.Dialog_Common).apply {
            setCanceledOnTouchOutside(false)
        }
    }

    /**
     * 《隐私政策》
     */
    private fun CharSequence.addPrivacyPolicy() = addSpan(
        R.string.tvPrivateService.getResString(),
        HighlightSpan(highlightColor) {
            activity?.let {
                SimpleBrowserActivity.launch(
                    it,
                    Config.get().privacyUrl,
                    R.string.tvSYPrivateService.getResString()
                )
            }
        })

    /**
     * 《用户协议》
     */
    private fun CharSequence.addUserPolicy() = addSpan(
        R.string.tvUserService.getResString(),
        HighlightSpan(highlightColor) {
            activity?.let {
                SimpleBrowserActivity.launch(
                    it,
                    Config.get().serviceUrl,
                    R.string.tvSYUserService.getResString()
                )
            }
        })

    override fun onPrepareView() {
        super.onPrepareView()
        binding.tvContent.movementMethod = LinkMovementMethod.getInstance()
        val fullText = getString(R.string.policy_content)

        // 3. 使用 replaceSpan 进行匹配替换
        binding.tvContent.text = fullText
            // 匹配《用户协议》关键词并替换为高亮 Span
            .replaceSpan(R.string.tvUserService.getResString()) {
                HighlightSpan(highlightColor) {
                    activity?.let {
                        SimpleBrowserActivity.launch(
                            it,
                            Config.get().serviceUrl,
                            R.string.tvSYUserService.getResString()
                        )
                    }
                }
            }
            // 匹配《隐私政策》关键词并替换为高亮 Span
            .replaceSpan(R.string.tvPrivateService.getResString()) {
                HighlightSpan(highlightColor) {
                    activity?.let {
                        SimpleBrowserActivity.launch(
                            it,
                            Config.get().privacyUrl,
                            R.string.tvSYPrivateService.getResString()
                        )
                    }
                }
            }
        singleClick(binding.tvAgree) { (activity as? PolicyCallback)?.onAgreePolicy(this) }
        singleClick(binding.tvReject) { (activity as? PolicyCallback)?.onRejectPolicy(this) }
    }

    interface PolicyCallback {
        fun onAgreePolicy(dialog: PolicyDialog)
        fun onRejectPolicy(dialog: PolicyDialog)
    }
}