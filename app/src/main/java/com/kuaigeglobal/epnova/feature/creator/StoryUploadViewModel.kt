package com.kuaigeglobal.epnova.feature.creator

import android.graphics.drawable.Drawable
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.formatW
import com.kuaigeglobal.epnova.common.utils.TextEncodingUtil
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.ext.requestWithLoading
import com.kuaigeglobal.epnova.core.network.apis.request.PowerCalculateBody
import com.kuaige.oss.OnUploadListener
import com.kuaige.oss.OssClient
import com.nhsd.basic.ext.getResDrawable
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast

/**
 */
class StoryUploadViewModel : AppRepositoryViewModel<AppRepository>() {

    val fileName by lazy { ObservableField<String>() }
    var fileUrl: String? = null
    val uploadStatus by lazy { ObservableInt(STATE_NONE) }
    val uploadErrorType by lazy { ObservableInt() }
    val uploadProgress by lazy { ObservableField<String>() }

//    val limitErrorText = "上传失败，字数超${TextEncodingUtil.MAX_CHAR_COUNT.formatW()}字"
    val limitErrorText = R.string.word_count_not_meet_requirements_please_retry.getResString()
    val limitText = R.string.txt_file_size_limit.getResString(TextEncodingUtil.MIN_CHAR_COUNT.formatW(), TextEncodingUtil.MAX_CHAR_COUNT.formatW())

    companion object {
        const val STATE_NONE = 0
        const val STATE_UPLOADING = 1
        const val STATE_UPLOAD_SUCCESS = 2
        const val STATE_UPLOAD_FAIL = 3

        const val UPLOAD_ERROR_TYPE_FILE_CHAT_LIMIT = 1
        const val UPLOAD_ERROR_TYPE_OTHER = 2
    }

    override fun onCreate() {
        super.onCreate()
        fileName.set(mBundle.getString(Constants.Extra.NAME))
        fileUrl = mBundle.getString(Constants.Extra.URL)
        if (!fileUrl.isNullOrEmpty()) {
            uploadStatus.set(STATE_UPLOAD_SUCCESS)
        }
    }

    fun getImageIcon(state: Int?): Drawable? {
        return when (state) {
            STATE_NONE -> R.drawable.img_text_upload
            STATE_UPLOAD_SUCCESS -> R.drawable.img_text
            STATE_UPLOAD_FAIL -> R.drawable.img_text_error
            else -> R.drawable.icon_loading2
        }.getResDrawable()
    }

    fun powerCalculate(totalCharCount: Int?, callback: (Long?) -> Unit) {
        requestWithLoading({ mRepo.api.powerCalculate(PowerCalculateBody(4, totalCharCount)) }) {
            callback.invoke(data?.pointCost)
        }
    }

    fun startUpload(path: String?) {
        if (path.isNullOrEmpty()) {
            showToast(R.string.please_select_file)
            return
        }
        uploadStatus.set(STATE_UPLOADING)
        uploadProgress.set("0.0%")
        val ossClient = OssClient.newInstance(App.instance)
        ossClient.uploadFile(path, OssClient.TYPE_ASSET, object : OnUploadListener() {

            override fun onUploadProgress(totalLength: Long, currentLength: Long) {
                val rawProgress = if (totalLength > 0) {
                    (currentLength.toDouble() / totalLength) * 100
                } else {
                    0.0
                }
                val progressText = String.format("%.1f%%", rawProgress.coerceIn(0.0, 100.0))
                uploadProgress.set(progressText)
            }

            override fun onUploadSuccess(uploadSuccess: List<String?>) {
                val url = uploadSuccess.firstOrNull()
                if (url.isNullOrEmpty()) {
                    onUploadFail(null)
                } else {
                    fileUrl = url
                    uploadStatus.set(STATE_UPLOAD_SUCCESS)
                }
            }

            override fun onUploadFail(uploadFail: MutableList<String>?) {
                super.onUploadFail(uploadFail)
                uploadErrorType.set(UPLOAD_ERROR_TYPE_OTHER)
                uploadStatus.set(STATE_UPLOAD_FAIL)
            }
        })
    }

}