package com.kuaigeglobal.epnova.feature.play.more

import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.arthenica.ffmpegkit.FFmpegKit
import com.kuaige.oss.OnUploadListener
import com.kuaige.oss.OssClient
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.BaseShareViewModel
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.network.apis.entity.ShortDramaInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.ShortDramaVideoInfo
import com.kuaigeglobal.epnova.core.network.apis.request.SynthesisUploadBody
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.download.DownloadingViewModel
import com.kuaigeglobal.epnova.feature.download.transform.BaseTransform
import com.kuaigeglobal.epnova.feature.download.transform.MicroAnimShortDramaTransform.Companion.durationRegex
import com.kuaigeglobal.epnova.feature.download.transform.TransformException
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 分享短剧
 */
abstract class ShareShortDramaViewModel<T : AppRepository> : BaseShareViewModel<T>(), DownloadingViewModel.DownloadStatusHolder {

    var sharingShortDramaInfo: ShortDramaInfo? = null
    open val shortDrama: ShortDramaInfo? get() = sharingShortDramaInfo
    private var transform: BaseTransform? = null
    override val downloadProgress by lazy { ObservableInt() }
    val downloading by lazy { MutableLiveData(false) }

    /**
     * 分享
     * @param media 分享媒体
     * @param otherMedia Umeng默认不支持的其他渠道
     * @param saveToAlbum 保存到相册
     * @param upload 是否上传视频
     * @param overlay 内容蒙层
     */
//    fun share(
//        media: SHARE_MEDIA? = null,
//        otherMedia: OtherShareMedia? = null,
//        saveToAlbum: Boolean = false,
//        upload: Boolean = false,
//        overlay: View? = null,
//        onUpload: ((String) -> Unit)? = null,
//    ) {
//        val shortDrama = shortDrama ?: return
//        val transform = MicroAnimShortDramaTransform(shortDrama, overlay, shortDrama.downloadRatio)
//        transform.transformListener = object : TransformListener {
//            override fun onTransformStart() {
//                downloading.value = true
//                downloadProgress.set(0)
//            }
//
//            override fun onTransformProgress(progress: Int) {
//                downloadProgress.set(progress)
//            }
//
//            override fun onTransformFail(reason: String) {
//                exportFailed(reason)
//                downloading.value = false
//            }
//
//            override fun onTransformSuccess(file: File) {
//                if (saveToAlbum) {
////                    saveToAlbum(shortDrama, file, media)
//                    downloading.value = false
//                } else if (upload) {
//                    uploadVideo(shortDrama, file, onUpload)
//                } else {
////                    shareVideo(shortDrama, file, media, otherMedia)
//                }
//            }
//        }
//        this.transform = transform
//        transform.transform()
//    }

    protected fun exportFailed(reason: String) {
        reason.showToast()
    }

//    protected fun saveToAlbum(shortDrama: ShortDramaInfo, file: File, media: SHARE_MEDIA?) {
//        launch {
//            val destFile = File(PathUtils.getExternalDownloadsPath(), file.name)
//            val copied = if (destFile.exists()) true else withContext(Dispatchers.IO) { FileUtils.copy(file.putAIGCMetadata(shortDrama), destFile) }
//            if (copied) {
//                FileUtils.notifySystemToScan(destFile)
//                if (arrayOf(SHARE_MEDIA.WEIXIN, SHARE_MEDIA.WEIXIN_CIRCLE).contains(media)) {
//                    "视频已保存至相册，打开微信后，请从相册上传分享。".showToast()
//                } else {
//                    topActivity?.showConfirmPrompt(
//                        title = "保存成功",
//                        tip = "在手机的“相册-视频”中可查看",
//                        confirmText = "我知道了",
//                        confirmStyle = true
//                    )
//                }
//            } else {
//                "保存失败".showToast()
//            }
//        }
//    }

//    protected fun shareVideo(shortDrama: ShortDramaInfo, file: File, media: SHARE_MEDIA?, otherMedia: OtherShareMedia?) {
//        launch {
//            val screenshot = withContext(Dispatchers.IO) {
//                runCatching {
//                    val retriever = MediaMetadataRetriever()
//                    retriever.setDataSource(file.path)
//                    retriever.getFrameAtTime(0)?.toCache()
//                }.onFailure {
//                    it.printStackTrace()
//                }.getOrNull()
//            }
//            downloading.value = false
//            val shareContent = shortDrama.shortDramaVideoShareContent()
////            shareManagerFactory?.getShareManager()?.shareVideo(shareContent, file, screenshot, media, otherMedia, shareManagerFactory?.getShareListener())
//        }
//    }

    private fun uploadVideo(shortDrama: ShortDramaInfo, file: File, onUpload: ((String) -> Unit)?) {
        val ossClient = OssClient.newInstance(App.instance)
        ossClient.uploadFile(file.absolutePath, OssClient.TYPE_ASSET, object : OnUploadListener() {
            override fun onUploadSuccess(uploadSuccess: List<String?>) {
                val videoUrl = uploadSuccess.firstOrNull()
                if (videoUrl.isNullOrEmpty()) {
                    onUploadFail(null)
                } else {
                    launch(
                        block = {
                            val duration = withContext(Dispatchers.IO) {
                                val command = "-i ${file.absolutePath} -hide_banner"
                                val session = FFmpegKit.execute(command)
                                val output = session.allLogsAsString
                                val match = durationRegex.find(output)
                                if (match == null) {
                                    throw TransformException("获取片段时长失败")
                                }
                                val (h, m, s) = match.destructured
                                // 图片时长 = 时 * 3600 + 分 * 60 + 秒
                                (h.toInt() * 3600 + m.toInt() * 60 + s.toFloat()).toInt()
                            }
                            response(
                                bean = mRepo.api.shortDramasSynthesisUpload(SynthesisUploadBody(shortDrama.dramasId, videoUrl, duration)),
                                result = {
                                    shortDrama.dramasVideo = ShortDramaVideoInfo(videoUrl, duration)
                                    downloading.value = false
                                    onUpload?.invoke(videoUrl)
                                },
                                error = {
                                    downloading.value = false
                                    showToast(R.string.upload_failed)
                                }
                            )
                        },
                        onError = {
                            downloading.value = false
                            showToast(R.string.upload_failed)
                        })
                }
            }

            override fun onUploadFail(uploadFail: MutableList<String>?) {
                super.onUploadFail(uploadFail)
                downloading.value = false
                showToast(R.string.upload_failed)
            }
        })
    }

    override fun cancelDownload() {
        transform?.cancel()
        downloading.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelDownload()
    }

}