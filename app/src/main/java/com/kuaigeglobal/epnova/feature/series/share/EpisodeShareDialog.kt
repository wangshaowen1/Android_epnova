package com.kuaigeglobal.epnova.feature.series.share

import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.blankj.utilcode.util.ClipboardUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.network.apis.entity.ShortDramaInfo
import com.kuaigeglobal.epnova.databinding.DialogEpisodeShareBinding
import com.kuaigeglobal.epnova.feature.play.more.DownloadMethodDialog
import com.kuaigeglobal.epnova.feature.settings.report.ReportActivity
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast

/**
 * 单集-分享
 */
class EpisodeShareDialog : BaseBottomPushVMDialogFragment<DialogEpisodeShareBinding, EpisodeShareViewModel>(), DownloadMethodDialog.DownloadTypeLauncher {
    companion object {

        /**
         * 显示分享弹窗
         */
        fun show(launcher: ShareLauncher, drama: ShortDramaInfo) {
            val fragmentManager = launcher.fragmentManager() ?: return
            EpisodeShareDialog().apply {
                arguments = bundleOf(Constants.Extra.DRAMA to drama)
            }.show(fragmentManager)
        }
    }

    private val shareLauncher by lazy { (parentFragment as? ShareLauncher) ?: (activity as? ShareLauncher) }
    private val drama by lazy { arguments?.getParcelableValue(Constants.Extra.DRAMA, ShortDramaInfo::class.java) }
    private val wxShareTitle: String?
        get() {
            return "${drama?.dramasSeries?.seriesName}丨第${drama?.workNo}章"
        }
    private val wxShareContent: String?
        get() {
            return "来快出片一键创作，解锁你的爆款小说漫"
        }
    private val pyqShareTitle: String?
        get() {
            return "${wxShareTitle}。${wxShareContent}"
        }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
//            R.id.btnWx -> {
//                shareLauncher?.uploadVideo(false) {
//                    doShare(SHARE_MEDIA.WEIXIN)
//                }
//            }
//
//            R.id.btnPyq -> {
//                shareLauncher?.uploadVideo(false) {
//                    doShare(SHARE_MEDIA.WEIXIN_CIRCLE)
//                }
//            }

            R.id.btnCopy -> {
                shareLauncher?.uploadVideo(false) {
                    mRealVM.getShareUrl { url -> copyUrl(url) }
                }
            }

            R.id.btnDownload -> {
            }

            R.id.btnReport -> {
                ReportActivity.start(1, drama?.dramasId, drama?.artist?.userToken)
            }

            R.id.btnEdit -> {
            }
        }
    }

    private fun copyUrl(url: String) {
        val copyText = "$pyqShareTitle\n$url"
        ClipboardUtils.copyText(copyText)
        R.string.copy_success.getResString().showToast()
    }

//    private fun doShare(shareMedia: SHARE_MEDIA) {
//        mRealVM.getShareUrl { shareUrl -> share(shareUrl, shareMedia) }
//    }
//
//    private fun share(shareUrl: String, shareMedia: SHARE_MEDIA) {
//        mRealVM.loading.set(true)
//        val share = { imageBitmap: Bitmap? ->
//            mRealVM.loading.set(false)
//            shareManager.doShare(
//                if (shareMedia == SHARE_MEDIA.WEIXIN_CIRCLE) pyqShareTitle else wxShareTitle,
//                wxShareContent,
//                imageBitmap,
//                R.drawable.img_default_cover_1_1,
//                shareUrl,
//                shareMedia,
//                this
//            )
//        }
//        getImageBitmap(drama?.dramasSeries?.seriesCoverUrl) { imageBitmap -> share.invoke(imageBitmap) }
//    }
//
//    override fun onStart(p0: SHARE_MEDIA?) {
//    }
//
//    override fun onResult(p0: SHARE_MEDIA?) {
//        dismissAllowingStateLoss()
//    }
//
//    override fun onError(p0: SHARE_MEDIA?, p1: Throwable?) {
//        if (p1 is KsShareException) {
//            p1.message ?: "分享失败"
//        } else {
//            "分享失败"
//        }.showToast()
//        dismissAllowingStateLoss()
//    }
//
//    override fun onCancel(p0: SHARE_MEDIA?) {
//        "已取消分享".showToast()
//        dismissAllowingStateLoss()
//    }

    override fun fragmentManager(): FragmentManager? = childFragmentManager

    override fun downloadVideo(watermark: Boolean) {
        shareLauncher?.downloadVideo(watermark)
        dismissAllowingStateLoss()
    }

    interface ShareLauncher : DownloadMethodDialog.DownloadTypeLauncher {
        fun uploadVideo(watermark: Boolean, onUpload: (url: String) -> Unit)
    }
}