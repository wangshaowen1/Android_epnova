package com.kuaigeglobal.epnova.feature.settings.report

import androidx.lifecycle.MutableLiveData
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.network.apis.request.ReportSubmissionBody
import com.kuaigeglobal.epnova.core.response

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:举报
 * @Data: 2026/2/2/周一  15:25
 * @Version: 1.0
 * *****************************************
 */
class ReportDialogViewModel : AppRepositoryViewModel<AppRepository>() {

    //被举报的作品用户 ID
    private val userId: String? by lazy {
        mBundle.getString(ReportDialog.USER_ID)
    }

    //被举报的作品 ID
    private val homeId: String?  by lazy {
        mBundle.getString(ReportDialog.HOME_ID)
    }

    //提交状态
    var isSubmit = MutableLiveData(false)

     fun submit(
        content: String,
        phoneNumber: String?,
    ) {
        launchWithLoading {
            response(
                mRepo.api.reportSubmission(
                    ReportSubmissionBody(
                        userId = userId,
                        homeId = homeId,
                        description = content,
                        contactInformation = phoneNumber
                    )
                )
            ) {
                isSubmit.value = true
            }
        }
    }
}