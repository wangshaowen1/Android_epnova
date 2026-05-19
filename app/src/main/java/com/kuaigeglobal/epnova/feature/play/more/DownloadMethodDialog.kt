package com.kuaigeglobal.epnova.feature.play.more

import androidx.fragment.app.FragmentManager
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.databinding.DialogDownloadMethodBinding
import com.kuaigeglobal.epnova.feature.web.SimpleBrowserActivity
import com.nhsd.basic.BaseBottomPushDialogFragment
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
        binding.btnWithWatermark.setOnSingleClickListener {
            launcher?.downloadVideo(true)
            dismissAllowingStateLoss()
        }
        binding.btnWithoutWatermark.setOnSingleClickListener {
            launcher?.downloadVideo(false)
            dismissAllowingStateLoss()

        }
        binding.btnPolicy.setOnSingleClickListener {
            Config.get().appConfig.userProtocolUrl?.let { SimpleBrowserActivity.launch(activity, it) }
        }
        binding.btnClose.setOnSingleClickListener {
            dismissAllowingStateLoss()
        }
    }

    interface DownloadTypeLauncher {
        fun fragmentManager(): FragmentManager?
        fun downloadVideo(watermark: Boolean)
    }
}