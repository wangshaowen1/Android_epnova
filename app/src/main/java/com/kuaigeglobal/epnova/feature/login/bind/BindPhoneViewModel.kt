package com.kuaigeglobal.epnova.feature.login.bind

import android.graphics.Color
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.RegexUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.ext.parseMessage
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.login.data.BindPhoneRepository
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.T
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

/**
 * 绑定手机号
 */
class BindPhoneViewModel : AppRepositoryViewModel<BindPhoneRepository>() {
    var mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = R.string.change_phone.getResString(),
        background = Color.TRANSPARENT
    )

    /**
     * 手机号
     */
    val mobile by lazy { MutableLiveData<String?>() }

    /**
     * 验证码
     */
    val code by lazy { MutableLiveData<String?>() }

    /**
     * 发送验证码按钮文字
     */
    val sendCodeText by lazy { ObservableField(R.string.send_sms_code.getResString()) }

    /**
     * 发送验证码按钮是否可点击
     */
    val sendCodeEnable by lazy { ObservableBoolean(true) }

    /**
     * 登录按钮是否可点击
     */
    val loginEnable by lazy { ObservableBoolean(false) }


    private var countdownId = 0L


    /**
     * 校验手机号绑定状态
     */
    fun checkPhoneBind(onSent: () -> Unit) {
        val mobile = this.mobile.value
        if (!RegexUtils.isMobileSimple(mobile)) {
            T.showShort(R.string.enter_right_phone_num)
            return
        }
        sendCodeEnable.set(false)
        sendSmsCode(onSent)

    }

    /**
     * 发送验证码
     */
    private fun sendSmsCode(onSent: () -> Unit) {
        val mobile = this.mobile.value
        if (!RegexUtils.isMobileSimple(mobile)) {
            T.showShort(R.string.enter_right_phone_num)
            return
        }
        sendCodeEnable.set(false)
        launch(
            block = {
                response(
                    mRepo.sendSmsValidateCode(mobile),
                    result = {
                        T.showShort(R.string.get_validate_code_success)
                        startCountdown()
                        onSent.invoke()
                    },
                    error = {
                        this?.msg?.showToast()
                        sendCodeEnable.set(true)
                    })
            },
            onError = {
                parseMessage(it)?.showToast()
                sendCodeEnable.set(true)
            })
    }

    /**
     * 绑定手机
     */
    fun bindPhone(callback: () -> Unit) {
        val mobile = this.mobile.value
        if (!RegexUtils.isMobileSimple(mobile)) {
            T.showShort(R.string.enter_right_phone_num)
            return
        }
        val code = this.code.value
        if (code?.length != Constants.SMS_CODE_LENGTH) {
            T.showShort(R.string.enter_right_validate_code)
            return
        }
        bindPhone(mobile, code, callback)
    }

    private fun bindPhone(mobile: String?, code: String?, callback: () -> Unit) {
        launchWithLoading(false) {
            response(mRepo.bindPhone(mobile, code)) {
                showToast(R.string.bind_success)
                User.get().apply {
                    storePhoneNum(mobile)
                }
                FlowEventBus.postSync(Events.BindingSuccessfulEvent())
                callback()
            }
        }
    }

    /**
     * 开始搜索卡倒计时
     */
    private fun startCountdown() {
        val remainSeconds = 60
        updateCountdown(remainSeconds)
        countdownId = System.nanoTime()
        launch {
            val id = countdownId
            flow {
                for (time in remainSeconds downTo 0) {
                    delay(1000)
                    if (id != countdownId) break
                    emit(time)
                }
            }.collect { updateCountdown(it) }
        }
    }

    /**
     * 更新倒计时UI
     */
    private fun updateCountdown(seconds: Int) {
        if (seconds <= 0) {
            stopCountdown()
            return
        }
        sendCodeText.set("${seconds}s")
    }

    /**
     * 停止倒计时，隐藏搜索卡
     */
    private fun stopCountdown() {
        countdownId = 0
        sendCodeText.set(R.string.resend_sms_code.getResString())
        sendCodeEnable.set(true)
    }

    /**
     * 校验按钮是否可点击
     */
    fun checkBtnEnable() {
        loginEnable.set(RegexUtils.isMobileSimple(mobile.value) && (code.value?.length ?: 0) >= Constants.SMS_CODE_LENGTH)
    }

    fun clearInput() {
        mobile.value = ""
    }
}