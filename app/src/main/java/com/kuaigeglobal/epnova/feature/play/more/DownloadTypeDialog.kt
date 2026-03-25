package com.kuaigeglobal.epnova.feature.play.more

import androidx.fragment.app.FragmentManager

/**
 * 下载方式弹窗
 */
class DownloadTypeDialog  {

    interface DownloadTypeLauncher {
        fun fragmentManager(): FragmentManager?
        fun downloadVideo(watermark: Boolean)
    }
}