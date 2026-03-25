package com.kuaigeglobal.epnova.feature.settings.logoff

import androidx.lifecycle.MutableLiveData
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import com.kuaigeglobal.epnova.core.response
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast


/**
 * 注销账号-提交
 */
class ApplyLogOffViewModel : AppRepositoryViewModel<AppRepository>() {
    var toolbar = ToolbarViewModel(navigationAction = { back() }, title = R.string.app_cancel_the_account.getResString())
    val content by lazy { MutableLiveData<String>() }

    /**
     * 提交注销申请
     */
    fun commitLogoff(content: String) {
        launchWithLoading {
            response(mRepo.api.logOff(UserTokenBody())) {
                R.string.account_deletion_notice_period.getResString().showToast()
                finish()
            }
        }
    }
}