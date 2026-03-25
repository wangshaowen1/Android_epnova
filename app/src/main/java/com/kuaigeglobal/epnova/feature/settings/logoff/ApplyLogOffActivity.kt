package com.kuaigeglobal.epnova.feature.settings.logoff

import android.view.View
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.showConfirmPrompt
import com.kuaigeglobal.epnova.databinding.ActivityApplyLogOffBinding
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.showToast

/**
 * 注销账号-提交
 */
class ApplyLogOffActivity : BaseViewModelActivity<ActivityApplyLogOffBinding, ApplyLogOffViewModel>() {

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        if (view?.id == R.id.btnLogOff) {
            logOff()
        }
    }

    private fun logOff() {
        val content = mRealVM.content.value
        if (content.isNullOrEmpty() || content.length < 10) {
            getString(R.string.appeal_reason_min_length_hint).showToast()
            return
        }
        showConfirmPrompt(title = getString(R.string.confirm_submit_appeal), cancelText = getString(R.string.action_not_now), confirmText = getString(R.string.confirm_ok)) {
            mRealVM.commitLogoff(content)
        }
    }

}