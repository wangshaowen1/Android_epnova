package com.kuaigeglobal.epnova.feature.download

import java.io.File

interface DownloadListener {
    fun onDownloadStart()
    fun onDownloadProgress(progress: Int)
    fun onDownloadFail(reason: String)
    fun onDownloadSuccess(file: File)
}