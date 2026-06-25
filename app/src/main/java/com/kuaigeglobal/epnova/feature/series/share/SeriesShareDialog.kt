package com.kuaigeglobal.epnova.feature.series.share

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.blankj.utilcode.util.ClipboardUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.network.apis.entity.DramasSeriesInfo
import com.kuaigeglobal.epnova.databinding.DialogSeriesShareBinding
import com.kuaigeglobal.epnova.feature.settings.report.ReportActivity
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast

/**
 * 剧集-分享
 */
class SeriesShareDialog : BaseBottomPushVMDialogFragment<DialogSeriesShareBinding, SeriesShareViewModel>() {
    companion object {

        fun show(fragmentManager: FragmentManager?, series: DramasSeriesInfo) {
            show(fragmentManager, series.authorUserId, series.dramasSeriesId, series.seriesName, series.seriesCoverUrl)
        }

        /**
         * 显示剧集分享弹窗
         * @param authorUserId 剧集作者
         * @param seriesId 剧集ID
         * @param seriesName 剧集名称
         * @param seriesCoverUrl 剧集封面
         */
        fun show(fragmentManager: FragmentManager?, authorUserId: String?, seriesId: String?, seriesName: String?, seriesCoverUrl: String?) {
            val fragmentManager = fragmentManager ?: (topActivity as? FragmentActivity)?.supportFragmentManager ?: return
            SeriesShareDialog().apply {
                arguments = Bundle().apply {
                    putString(Constants.Extra.ID, seriesId)
                    putString(Constants.Extra.USER_TOKEN, authorUserId)
                    putString(Constants.Extra.NAME, seriesName)
                    putString(Constants.Extra.COVER, seriesCoverUrl)
                }
            }.show(fragmentManager)
        }
    }

    private val seriesId by lazy { arguments?.getString(Constants.Extra.ID) }
    private val authorUserId by lazy { arguments?.getString(Constants.Extra.USER_TOKEN) }
    private val seriesName by lazy { arguments?.getString(Constants.Extra.NAME) }
    private val seriesCoverUrl by lazy { arguments?.getString(Constants.Extra.COVER) }
    private val wxShareTitle: String?
        get() {
            return seriesName
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
//                doShare(SHARE_MEDIA.WEIXIN)
//            }
//
//            R.id.btnPyq -> {
//                doShare(SHARE_MEDIA.WEIXIN_CIRCLE)
//            }

            R.id.btnCopy -> {
                mRealVM.getShareUrl { url -> copyUrl(url) }
            }

            R.id.btnReport -> {
                ReportActivity.start(2, seriesId, authorUserId)
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
//        getImageBitmap(seriesCoverUrl) { imageBitmap -> share.invoke(imageBitmap) }
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

}