package com.kuaigeglobal.epnova.feature.download

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.kuaigeglobal.epnova.databinding.DialogDownloadingBinding
import com.nhsd.basic.BaseVMDialogFragment

/**
 * 下载中-进度
 */
class DownloadingDialog : BaseVMDialogFragment<DialogDownloadingBinding, DownloadingViewModel>() {
    companion object {
        fun show(factory: DownloadingViewModel.DownloadStatusHolderFactory): DownloadingDialog? {
            return when (factory) {
                is Fragment -> DownloadingDialog().apply { show(factory.childFragmentManager, null) }
                is FragmentActivity -> DownloadingDialog().apply { show(factory.supportFragmentManager, null) }
                else -> null
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onPrepareView() {
        super.onPrepareView()
        val factory = (parentFragment as? DownloadingViewModel.DownloadStatusHolderFactory)
            ?: (activity as? DownloadingViewModel.DownloadStatusHolderFactory)
        mRealVM.statusHolder.set(factory?.getStatusHolder())
    }
}