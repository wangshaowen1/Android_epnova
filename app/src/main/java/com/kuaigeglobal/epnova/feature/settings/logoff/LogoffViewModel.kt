package com.kuaigeglobal.epnova.feature.settings.logoff

import android.graphics.Color
import android.os.CountDownTimer
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.network.apis.entity.LogOffStatusInfo
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.kit.SignUtils
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast

/**
 * *****************************************
 * 项目名称: 小元音乐圈
 * @Author: SZYMN
 * @Introduce:注销账号页面
 * @Data: 2025/4/22  13:55
 * @Version: 1.0
 * *****************************************
 */
class LogoffViewModel : AppRepositoryViewModel<AppRepository>() {
    companion object {
        //注销状态未申请，不可点击状态
        const val REMOVE_ACCOUNT_STATE_NOT_APPLY = 0

        //注销状态未申请，可点击状态
        const val REMOVE_ACCOUNT_STATE_APPLY = 1

        //注销状态已申请，不可点击状态
        const val REMOVE_ACCOUNT_STATE_APPLY_NOT_CLICK = 2
    }

    var mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = R.string.app_cancel_the_account.getResString(),
        background = Color.TRANSPARENT
    )
    val checkedPolicy = ObservableField(false)

    //注销账户的状态 0未申请，1提交申请
    val removeAccountState = ObservableInt(REMOVE_ACCOUNT_STATE_NOT_APPLY)

    //提交申请文案
    val submitApplyText = ObservableField(R.string.unregister_request.getResString())
    private var countDownTimer: CountDownTimer? = null
    override fun onStart() {
        super.onStart()
        countdown()
    }


    private fun countdown() {
        // 取消之前的倒计时（如果存在）
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000) + 1
                submitApplyText.set(R.string.cancellation_request_format.getResString(seconds))
            }

            override fun onFinish() {
                submitApplyText.set(R.string.unregister_request.getResString())
                removeAccountState.set(REMOVE_ACCOUNT_STATE_APPLY)
                countDownTimer = null
            }
        }.start()
    }

    fun checkPolicy() {
        checkedPolicy.set(checkedPolicy.get() != true)
    }

    fun logOff() {
        startActivity(ApplyLogOffActivity::class.java)
    }

    /**
     * 取消注销
     */
    fun cancelLogoff() {
        launchWithLoading {
            response(mRepo.api.cancelLogOff(UserTokenBody())) {
                showToast(R.string.cancellation_success)
                finish()
            }
        }
    }


    /**
     * 提交注销申请
     */
    fun commitLogoff() {
        launchWithLoading {
            response(mRepo.api.logOff(UserTokenBody())) {
                SignUtils.signOut()
                finish()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
    }
}