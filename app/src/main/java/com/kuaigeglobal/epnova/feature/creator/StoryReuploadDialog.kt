package com.kuaigeglobal.epnova.feature.creator

import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.formatPlays
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.databinding.DialogStoryReuploadBinding
import com.nhsd.basic.BaseVMDialogFragment
import com.nhsd.basic.ext.setOnSingleClickListener

/**
 * 小说文档检测到敏感词
 */
class StoryReuploadDialog : BaseVMDialogFragment<DialogStoryReuploadBinding, StoryUploadViewModel>() {

    companion object {

        const val KEY_TOTAL_CHAR_COUNT = "totalCharCount"
        const val KEY_TOTAL_SENSITIVE_COUNT = "totalSensitiveCount"

        fun show(
            fileName: String?, totalCharCount: Int?, totalSensitiveCount: String?,
            callback: () -> Unit
        ) {
            val activity = topActivity as? FragmentActivity ?: return
            StoryReuploadDialog().apply {
                arguments = bundleOf(
                    Constants.Extra.NAME to fileName,
                    KEY_TOTAL_CHAR_COUNT to totalCharCount,
                    KEY_TOTAL_SENSITIVE_COUNT to totalSensitiveCount
                )
                this.reuploadCallback = callback
            }.show(activity.supportFragmentManager)
        }
    }

    var reuploadCallback: () -> Unit = {}

    override fun onPrepareView() {
        super.onPrepareView()
        arguments?.apply {
            val fileName = getString(Constants.Extra.NAME)
            val totalCharCount = getInt(KEY_TOTAL_CHAR_COUNT)
            val totalSensitiveCount = getString(KEY_TOTAL_SENSITIVE_COUNT)
            binding.fileName.text = fileName
            binding.totalCharCount.text = getString(R.string.total_char_count, totalCharCount.formatPlays())
            binding.errorTip.text = totalSensitiveCount
        }

        binding.confirm.setOnSingleClickListener {
            reuploadCallback.invoke()
            dismissAllowingStateLoss()
        }
    }
}