package com.kuaigeglobal.epnova.feature.play.more

import android.content.Intent
import android.view.View
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.FragmentManager
import com.kuaigeglobal.epnova.common.permissions.requestRead2WritePermissions
import com.kuaigeglobal.epnova.feature.download.DownloadingDialog
import com.kuaigeglobal.epnova.feature.download.DownloadingViewModel
import com.kuaigeglobal.epnova.feature.series.share.EpisodeShareDialog
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.isActive

/**
 * 分享短剧
 */
abstract class ShareShortDramaActivity<V : ViewDataBinding, VM : ShareShortDramaViewModel<*>> : BaseViewModelActivity<V, VM>(),
    EpisodeShareDialog.ShareLauncher,
    DownloadingViewModel.DownloadStatusHolderFactory {
    private var downloadingDialog: DownloadingDialog? = null
    protected abstract val exportVideoOverlay: View?

    override fun onViewModelCreated() {
        super.onViewModelCreated()
        mRealVM.downloading.observe(this) {
            if (it) {
                if (downloadingDialog == null) {
                    downloadingDialog = DownloadingDialog.show(this)
                }
            } else {
                downloadingDialog?.dismissAllowingStateLoss()
                downloadingDialog = null
            }
        }
    }

    override fun getStatusHolder(): DownloadingViewModel.DownloadStatusHolder? = if (isActive) mRealVM else null

    override fun fragmentManager(): FragmentManager? = supportFragmentManager

    override fun downloadVideo(watermark: Boolean) {
        requestRead2WritePermissions {
            if (it) {
//                mRealVM.share(saveToAlbum = true, overlay = if (watermark) exportVideoOverlay else null)
            }
        }
    }

    override fun uploadVideo(watermark: Boolean, onUpload: (String) -> Unit) {
        val shortDrama = mRealVM.shortDrama ?: return
        val videoUrl = shortDrama.dramasVideo?.videoUrl
        if (videoUrl.isNullOrEmpty()) {
//            mRealVM.share(upload = true, onUpload = onUpload, overlay = if (watermark) exportVideoOverlay else null)
        } else {
            onUpload.invoke(videoUrl)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
//        ShareManager.onActivityResult(requestCode, resultCode, data)
    }
}