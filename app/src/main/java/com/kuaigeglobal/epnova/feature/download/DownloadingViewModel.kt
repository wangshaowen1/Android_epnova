package com.kuaigeglobal.epnova.feature.download

import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.kuaigeglobal.epnova.base.viewmodel.AppViewModel

/**
 * 下载中-进度
 */
class DownloadingViewModel : AppViewModel() {
    val statusHolder by lazy { ObservableField<DownloadStatusHolder>() }

    interface DownloadStatusHolderFactory {
        fun getStatusHolder(): DownloadStatusHolder?
    }

    interface DownloadStatusHolder {
        val downloadProgress: ObservableInt
        fun cancelDownload()
    }
}