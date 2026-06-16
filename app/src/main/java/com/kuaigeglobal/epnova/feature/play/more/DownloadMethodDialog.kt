package com.kuaigeglobal.epnova.feature.play.more

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.fragment.app.FragmentManager
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.databinding.DialogDownloadMethodBinding
import com.kuaigeglobal.epnova.feature.web.SimpleBrowserActivity
import com.nhsd.basic.BaseBottomPushDialogFragment
import com.nhsd.basic.ext.getThemeColor
import com.nhsd.basic.ext.setOnSingleClickListener

/**
 * 下载方式弹窗
 */
class DownloadMethodDialog : BaseBottomPushDialogFragment<DialogDownloadMethodBinding>() {

    companion object {
        fun show(launcher: DownloadTypeLauncher) {
            launcher.fragmentManager()?.let { DownloadMethodDialog().show(it) }
        }
    }

    private val launcher: DownloadTypeLauncher? get() = (parentFragment as? DownloadTypeLauncher) ?: (activity as? DownloadTypeLauncher)

    override fun onPrepareView() {
        super.onPrepareView()
        setupPolicySpan()
        binding.btnWithWatermark.setOnSingleClickListener {
            launcher?.downloadVideo(true)
            dismissAllowingStateLoss()
        }
        binding.btnWithoutWatermark.setOnSingleClickListener {
            launcher?.downloadVideo(false)
            dismissAllowingStateLoss()

        }
        binding.btnClose.setOnSingleClickListener {
            dismissAllowingStateLoss()
        }
    }

    private fun setupPolicySpan() {
        val content = getString(R.string.download_video_considered_as_agreement)
        val agreement = getString(R.string.download_agreement)
        val agreementStart = content.indexOf(agreement)
        val spanText = SpannableString(content)
        if (agreementStart != -1) {
            val agreementColor = binding.btnPolicy.context.getThemeColor(R.attr.textColor1)
            spanText.setSpan(
                ForegroundColorSpan(agreementColor),
                agreementStart,
                agreementStart + agreement.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spanText.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        Config.get().appConfig.userProtocolUrl?.let { SimpleBrowserActivity.launch(activity, it) }
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.color = agreementColor
                        ds.isUnderlineText = false
                    }
                },
                agreementStart,
                agreementStart + agreement.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        binding.btnPolicy.movementMethod = LinkMovementMethod.getInstance()
        binding.btnPolicy.highlightColor = Color.TRANSPARENT
        binding.btnPolicy.text = spanText
    }

    interface DownloadTypeLauncher {
        fun fragmentManager(): FragmentManager?
        fun downloadVideo(watermark: Boolean)
    }
}
