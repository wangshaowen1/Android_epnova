package com.kuaigeglobal.epnova.feature.settings.report

import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.network.apis.entity.KeyValueInfo
import com.kuaigeglobal.epnova.core.network.apis.request.TipOffBody
import com.kuaigeglobal.epnova.core.response
import com.kuaige.oss.OnUploadListener
import com.kuaige.oss.OssClient
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:
 * @Data: 2024/7/19 星期五 19:44
 * @Version: 1.0
 * *****************************************
 */

class ReportViewModel : AppRepositoryViewModel<AppRepository>() {
    var mToolbarVM = ToolbarViewModel(navigationAction = { back() }, title = R.string.report.getResString(), paddingTop = 0)

    val phoneNumber = MutableLiveData<String>()
    val photoSize = MutableLiveData(0)
    val type = ObservableField<KeyValueInfo?>()
    val typeNames by lazy { Config.get().appConfig.userTipoffTypeList }

    //被举报人ID
    val reportUserToken by lazy {
        mBundle.getString(Constants.Extra.REPORT_USER_ID)
    }
    val reportScene by lazy { mBundle.getInt(Constants.Extra.REPORT_SCENE, 1) }
    val reportId by lazy { mBundle.getString(Constants.Extra.REPORT_ID) }
    val tipOffContent by lazy { mBundle.getString(Constants.Extra.CONTENT) }
    override fun onCreate() {
        super.onCreate()
//        type.set(typeNames?.getOrNull(0))
        photoSize.value = 0
    }

    fun report(content: String, images: List<String>?) {
        setLoading(true)
        if (images.isNullOrEmpty()) {
            submit(content, null)
            return
        }
        val ossClient = OssClient.newInstance(App.instance)
        ossClient.uploadFile(images, OssClient.TYPE_NORMAL, object : OnUploadListener() {
            override fun onUploadSuccess(uploadSuccess: List<String?>) {
                val images = uploadSuccess.filterNotNull()
                if (images.isEmpty()) {
                    onUploadFail(null)
                } else {
                    submit(content, images)
                }
            }

            override fun onUploadFail(uploadFail: MutableList<String>?) {
                super.onUploadFail(uploadFail)
                showToast(R.string.toast_img_upload_fail)
                setLoading(false)
            }
        })
    }


    /**
     * 举报
     * @param content String 举报内容
     */
    private fun submit(content: String, images: List<String>?) {
        launch(
            block = {
                response(
                    bean = mRepo.api.tipOff(
                        TipOffBody(
                            otherUserToken = reportUserToken,
                            tipoffScene = reportScene,
                            tipoffItemId = reportId,
                            tipOffType = type.get()?.key,
                            tipoffReasonContent = content,
                            tipoffContent = content,
                            images?.filterNotNull()?.joinToString("@"),
                            contact = phoneNumber.value
                        )
                    ),
                    result = {
                        showToast(R.string.toast_commit_success)
                        finish()
                    },
                    error = {
                        this?.msg?.showToast()
                        setLoading(false)
                    }
                )
            },
            onError = {
                setLoading(false)
            },
            onComplete = {
                setLoading(false)
                null
            }
        )
    }

}