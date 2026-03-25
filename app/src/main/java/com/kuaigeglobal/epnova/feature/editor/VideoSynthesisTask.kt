package com.kuaigeglobal.epnova.feature.editor

import androidx.databinding.ObservableFloat
import androidx.databinding.ObservableInt
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.common.ext.ioScope
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.entity.SimpleStoryboardInfo
import com.kuaigeglobal.epnova.core.network.apis.request.VideoSynthesisBody
import com.kuaige.oss.OnUploadListener
import com.kuaige.oss.OssClient
import com.kuaige.videoedit.core.TransformListener
import com.kuaige.videoedit.ffmpeg.FFmpegHelper
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.launchNoCrash
import com.nhsd.basic.utils.Logger
import kotlinx.parcelize.IgnoredOnParcel
import java.io.File

/**
 * 合成视频任务
 */
data class VideoSynthesisTask(
    val workId: String?,
    val storyId: String?,
    private val storyboards: List<SimpleStoryboardInfo>,
    var status: Int = STATUS_INIT,
    var reason: String? = ""
) {

    companion object {
        const val STATUS_INIT = 0
        const val STATUS_VIDEO_GENERATING = 1
        const val STATUS_VIDEO_GENERATE_FAIL = 2
        const val STATUS_UPLOADING = 3
        const val STATUS_UPLOAD_FAIL = 4
        const val STATUS_SAVING = 5
        const val STATUS_SAVE_FAIL = 6
        const val STATUS_SUCCESS = 7
    }

    @IgnoredOnParcel
    val observableStatus by lazy { ObservableInt(status) }

    @IgnoredOnParcel
    val progress by lazy { ObservableFloat(0f) }

    @IgnoredOnParcel
    private var transform: VideoSynthesisTransform? = null
    private var canceled = false

    fun start() {
        synthesisVideo()
    }

    private fun synthesisVideo() {
        changeStatus(STATUS_VIDEO_GENERATING)
        transform?.cancel()
        transform = VideoSynthesisTransform(storyboards).apply {
            transformListener = object : TransformListener {
                override fun onTransformStart() {
                }

                override fun onTransformProgress(progress: Int) {
                }

                override fun onTransformFail(reason: String) {
                    fail(STATUS_VIDEO_GENERATE_FAIL, reason)
                }

                override fun onTransformSuccess(file: File) {
                    upload(file)
                }
            }
        }
        transform?.transform()
    }

    private fun upload(file: File) {
        if (canceled) return
        changeStatus(STATUS_UPLOADING)
        val ossClient = OssClient.newInstance(App.instance)
        ossClient.uploadFile(file.path, OssClient.TYPE_ASSET, object : OnUploadListener() {
            override fun onUploadSuccess(uploadSuccess: List<String?>) {
                val url = uploadSuccess.firstOrNull()
                if (url.isNullOrEmpty()) {
                    onUploadFail(null)
                } else {
                    save(file, url)
                }
            }

            override fun onUploadFail(uploadFail: MutableList<String>?) {
                super.onUploadFail(uploadFail)
                fail(STATUS_UPLOAD_FAIL, "上传失败")
            }

            override fun onUploadProgress(totalLength: Long, currentLength: Long) {
                super.onUploadProgress(totalLength, currentLength)
            }
        })
    }

    private fun save(file: File, videoUrl: String) {
        if (canceled) return
        changeStatus(STATUS_SAVING)
        ioScope.launchNoCrash(
            block = {
                val duration = runCatching { FFmpegHelper.parseDuration(file.path) / 1000 }.getOrNull()
                val response = ApiFactory.apiService.workStoryboardVideoSynthesis(VideoSynthesisBody(workId, storyId, videoUrl, duration))
                if (response.isSuccess) {
                    changeStatus(STATUS_SUCCESS)
                    VideoSynthesisManager.delete(this@VideoSynthesisTask)
                    FlowEventBus.postSync(Events.VideoSynthesisSuccessEvent(workId, storyId))
                } else {
                    fail(STATUS_SAVE_FAIL, response.msg ?: "调用保存接口失败")
                }
            },
            onError = {
                fail(STATUS_SAVE_FAIL, "调用保存接口失败")
            }
        )
    }

    private fun fail(status: Int, reason: String) {
        this.reason = reason
        changeStatus(status)
        progress.set(0f)
        Logger.i("$this fail($status, $reason)")
    }

    fun isFailed() = arrayOf(STATUS_VIDEO_GENERATE_FAIL, STATUS_UPLOAD_FAIL, STATUS_SAVE_FAIL).contains(status)

    fun isSynthesising() = arrayOf(STATUS_VIDEO_GENERATING, STATUS_UPLOADING, STATUS_SAVING).contains(status)

    fun changeStatus(newStatus: Int) {
        status = newStatus
        observableStatus.set(newStatus)
        VideoSynthesisManager.saveCache()
        Logger.i("$this changeStatus($newStatus)")
    }

    fun cancel() {
        transform?.cancel()
        canceled = true
    }
}