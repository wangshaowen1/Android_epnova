package com.kuaigeglobal.epnova.feature.settings.logoff

import android.app.Activity
import android.content.Intent
import android.view.View
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.showConfirmPrompt
import com.kuaigeglobal.epnova.databinding.ActivityCloseAccountBinding
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.showToast

/**
 * *****************************************
 * 项目名称: 小元音乐圈
 * @Author: SZYMN
 * @Introduce: 注销账号页面
 * @Data: 2025/4/22  13:54
 * @Version: 1.0
 * *****************************************
 */
class LogoffActivity :
    BaseViewModelActivity<ActivityCloseAccountBinding, LogoffViewModel>() {
    companion object {
        fun start(activity: Activity?) {
            activity ?: return
            val intent = Intent(activity, LogoffActivity::class.java)
            activity.startActivity(intent)
        }
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        if (view?.id == R.id.btnCancel) {
            when (mRealVM.removeAccountState.get()) {
                LogoffViewModel.REMOVE_ACCOUNT_STATE_NOT_APPLY -> {}
                LogoffViewModel.REMOVE_ACCOUNT_STATE_APPLY -> {
                    if (mRealVM.checkedPolicy.get()==false){
                        getString(R.string.please_agree_to_cancellation_terms).showToast()
                        return
                    }
                    showConfirmPrompt(
                        title = getString(R.string.confirm_account_cancellation),
                        cancelText = getString(R.string.cancel),
                        confirmText = getString(R.string.confirm_ok)
                    ) {
                        mRealVM.commitLogoff()
                    }
                }

                LogoffViewModel.REMOVE_ACCOUNT_STATE_APPLY_NOT_CLICK -> {
                    cancelLogoff()
                }
            }
        }
    }

    /**
     * 取消注销弹窗
     */
    private fun cancelLogoff() {
        showConfirmPrompt(tip = getString(R.string.confirm_cancel_cancellation_request), cancelText = getString(R.string.action_not_now)) {
            mRealVM.cancelLogoff()
        }
    }
}