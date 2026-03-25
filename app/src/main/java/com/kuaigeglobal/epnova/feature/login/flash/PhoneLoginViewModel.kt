package com.kuaigeglobal.epnova.feature.login.flash

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.Color
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.KeyboardUtils
import com.blankj.utilcode.util.RegexUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.getInstall
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.ext.loginToast
import com.kuaigeglobal.epnova.core.ext.parseMessage
import com.kuaigeglobal.epnova.core.network.apis.request.SendSmsValidateCodeBody
import com.kuaigeglobal.epnova.core.network.apis.response.UserHomeData
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.login.data.PhoneLoginRepository
import com.kuaigeglobal.epnova.kit.SignUtils
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.T
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

/**
 * 手机号登录
 */
class PhoneLoginViewModel : AppRepositoryViewModel<PhoneLoginRepository>() {

    var mToolbarVM = ToolbarViewModel(navigationAction = { back() }, background = Color.TRANSPARENT)

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

    /**
     * 协议是否勾选
     */
    val checkedPolicy by lazy { ObservableBoolean(false) }

    private var countdownId = 0L

    //手机号已被绑定
    val isMobilePhoneBinding = MutableLiveData(false)

    //进来是登录的还是绑定的
    val isBind = ObservableBoolean(false)


    override fun onCreate() {
        super.onCreate()
        isBind.set(mBundle.getBoolean(PhoneLoginDialogActivity.BIND, false))
        checkedPolicy.set(mBundle.getBoolean(PhoneLoginDialogActivity.CHECKED_POLICY, false))
    }


    /**
     * 发送验证码
     */
    fun sendSmsCode() {
        val mobile = this.mobile.value
        if (!RegexUtils.isMobileSimple(mobile)) {
            T.showShort(R.string.enter_right_phone_num)
            return
        }
        sendCodeEnable.set(false)
        launchWithLoading(
            block = {
                response(
                    mRepo.sendSmsValidateCode(
                        phoneNumber = mobile,
                        bizType = if (isBind.get()) {
                            SendSmsValidateCodeBody.BIZ_TYPE_CHANGE_PHONE
                        } else {
                            SendSmsValidateCodeBody.BIZ_TYPE_LOGIN
                        }
                    ),
                    result = {
                        T.showShort(R.string.get_validate_code_success)
                        startCountdown()
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
     * 登录
     */
    fun phoneLogin(onLogin: ((UserHomeData?) -> Unit)?) {
        if (!isBind.get() && !checkedPolicy.get()) {
            T.showShort(R.string.please_agree_protocol)
            return
        }
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
        KeyboardUtils.hideSoftInput(ActivityUtils.getTopActivity())
        setLoading(true)
        getInstall {
            launchWithLoading(false, block = {
                if (isBind.get()) {
                    response(mRepo.bindPhone(mobile, code), {
                        showToast(R.string.bind_success)
                        User.get().apply {
                            storePhoneNum(mobile)
                        }
                        onLogin?.invoke(null)
                    }, error = {
                        this?.msg?.showToast()
                        setLoading(false)
                    })
                } else {
                    response(mRepo.loginByPhone(mobile, code), {
                        User.get().updateFrom(data)
                        SignUtils.signIn()
                        data.loginToast()
                        onLogin?.invoke(data)
                    }, error = {
                        this?.msg?.showToast()
                        setLoading(false)
                    })
                }
            }, onError = {
                setLoading(false)
                parseMessage(it)?.showToast()
            })
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
     * 校验是否可登录
     */
    fun checkLoginEnable() {
        loginEnable.set(RegexUtils.isMobileSimple(mobile.value) && code.value?.length == Constants.SMS_CODE_LENGTH)
    }

    val inputOffset by lazy { ObservableInt(0.dp) }
    private var inputAnimator: Animator? = null

    fun onSoftInputChanged(height: Int) {
        inputAnimator?.cancel()
        val position = inputOffset.get()
        val animator = ValueAnimator.ofInt(position, if (height > 0) height else 0.dp)
        animator.duration = 150
        animator.addUpdateListener { inputOffset.set(animator.animatedValue as Int) }
        inputAnimator = animator
        animator.start()
    }
}