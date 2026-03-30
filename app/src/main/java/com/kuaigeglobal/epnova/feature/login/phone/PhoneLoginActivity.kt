package com.kuaigeglobal.epnova.feature.login.phone

import android.content.Intent
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import androidx.core.content.ContextCompat
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.databinding.ActivityPhoneLoginBinding
import com.kuaigeglobal.epnova.feature.login.PrivacyPolicyDialog
import com.kuaigeglobal.epnova.feature.settings.SettingsActivity
import com.kuaigeglobal.epnova.feature.web.SimpleBrowserActivity
import com.kuaigeglobal.epnova.widget.GeneratingDialog
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.showToast

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:闪验登录验证
 * @Data: 2026/2/10/周二  17:09
 * @Version: 1.0
 * *****************************************
 */
class PhoneLoginActivity :
    BaseViewModelActivity<ActivityPhoneLoginBinding, PhoneLoginViewModel>(){
    companion object {
        const val TAG = "PhoneLoginActivity"

        //是否同意了隐私政策用户协议这个
        var checkedPolicy = false
        fun start(
            showToast: Boolean = false,
            checkedPolicy: Boolean = false
        ) {
            if (showToast) {
                showToast(R.string.toast_login_please)
            }
            this.checkedPolicy = checkedPolicy
            val activity = topActivity ?: return
            val intent = Intent(activity, PhoneLoginActivity::class.java)
            activity.startActivity(intent)
            activity.overridePendingTransition(
                R.anim.push_bottom_in,
                R.anim.push_bottom_out
            )
        }
    }

    private var enterAnim = 0
    private var exitAnim = 0
    private var dialog: GeneratingDialog? = null


    override fun onViewModelCreated() {
        super.onViewModelCreated()
        initView()
        //数据更新
        dataUpdate()
    }

    private fun initView() {
        mRealVM.checkedPolicy.set(checkedPolicy)
    }

    private fun dataUpdate() {
        mRealVM.promptGenerating.observe(this) {
            if (it.first) {
                dialog?.dismiss()
                dialog = GeneratingDialog(this, it.second)
                dialog?.show()
                return@observe
            }
            dialog?.dismiss()
        }

        mRealVM.checkedPolicyChange.observe(this) {
            if (!it) return@observe
            mRealVM.checkedPolicyChange.value = false
            //用户协议
            termsOfService()
        }

        mRealVM.loginResult.observe(this) {
            if (!it) return@observe
            mRealVM.loginResult.value = false
            finishOnly()
        }
    }



    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.tv_phone -> {
                //一键登录
                if (mRealVM.checkedPolicy.get()) {
//                    mRealVM.flashLoginVerification()
                    return
                }
                val dialog = PrivacyPolicyDialog.show(supportFragmentManager)
                dialog.callback = callback@{ agree ->
                    if (!agree) return@callback
                    mRealVM.checkedPolicy.set(true)
//                    mRealVM.flashLoginVerification()
                }
            }

            R.id.ll_agreement,
            R.id.ll_agreement_captcha -> {
                //协议
                mRealVM.checkedPolicy.set(!mRealVM.checkedPolicy.get())
            }

//            R.id.iv_login_wx -> {
//                //微信登录
//                if (mRealVM.checkedPolicy.get()) {
//                    shareManager.login(SHARE_MEDIA.WEIXIN, this)
//                    return
//                }
//                val dialog = PrivacyPolicyDialog.show(supportFragmentManager)
//                dialog.callback = callback@{ agree ->
//                    if (!agree) return@callback
//                    mRealVM.checkedPolicy.set(true)
//                    shareManager.login(SHARE_MEDIA.WEIXIN, this)
//                }
//            }

            R.id.iv_login_mobile_phone_number -> {
                //手机号登录
                mRealVM.showState.set(PhoneLoginViewModel.SHOW_VERIFY_CODE_LOGIN)
                mRealVM.checkedPolicyChange.value = true
            }

            R.id.btnSend -> {
                mRealVM.sendSmsCode()
            }

            R.id.bt_verification_code_login -> {
                if (mRealVM.isBind.get() || mRealVM.checkedPolicy.get()) {
                    //手机号登录
                    phoneLogin()
                    return
                }
                val dialog = PrivacyPolicyDialog.show(supportFragmentManager)
                dialog.callback = callback@{
                    if (!it) return@callback
                    mRealVM.checkedPolicy.set(true)
                    //手机号登录
                    phoneLogin()
                }
            }

            R.id.btnClose->{
                finishOnly()
            }

            R.id.bt_settings->{
                SettingsActivity.start(this)
            }
        }
    }

    //手机号登录
    private fun phoneLogin() {
        mRealVM.phoneLogin { phone ->
            finishOnly()
            if (mRealVM.isBind.get()) {
                FlowEventBus.postSync(Events.BindingSuccessfulEvent())
                return@phoneLogin
            }
        }
    }


//    override fun onStart(platform: SHARE_MEDIA?) {
//        Logger.d(TAG, "onStart platform:$platform")
//    }
//
//    override fun onAuthed(
//        platform: SHARE_MEDIA?,
//        data: MutableMap<String, String>?
//    ) {
//        Logger.d(TAG, "onAuthed platform:$platform data:$data")
//        val loginType = when (platform) {
//            SHARE_MEDIA.WEIXIN -> LoginByThirdBody.LOGIN_TYPE_WECHAT
//            SHARE_MEDIA.QQ -> LoginByThirdBody.LOGIN_TYPE_QQ
//            else -> {
//                finishOnly()
//                return
//            }
//        }
//        mRealVM.loginByThird(loginType, data) {
//            finishOnly()
//        }
//    }
//
//    override fun onError(
//        e: Exception?,
//        platform: SHARE_MEDIA?,
//        onCancel: Boolean
//    ) {
//        Logger.d(TAG, "onError e:$e platform:$platform onCancel:$onCancel")
//        finishOnly()
//    }


    /**
     * finish动画处理
     */
    private fun finishOnly() {
        enterAnim = 0
        exitAnim = R.anim.push_bottom_out
        finish()
    }

    /**
     * 用户协议
     */
    private fun termsOfService() {
        val protocolName = mRealVM.flashTestPreFetchNumber.get()?.protocolName
        val agreement =
            if (mRealVM.showState.get() == PhoneLoginViewModel.SHOW_ONE_KEY_LOGIN && protocolName?.isNotBlank() == true) {
                "登录即代表同意和《用户协议》和《隐私政策》以及《${protocolName}》"
            } else {
                "登录即代表同意和《用户协议》和《隐私政策》"
            }
        val spannableString =
            SpannableString(agreement)
        val userAgreement = "《用户协议》"
        val userAgreementStartIndex = spannableString.indexOf(userAgreement)
        if (userAgreementStartIndex != -1) {
            spannableString.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        SimpleBrowserActivity.launch(
                            this@PhoneLoginActivity,
                            Config.get().serviceUrl,
                            "用户协议"
                        )
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.color = ContextCompat.getColor(
                            this@PhoneLoginActivity,
                            R.color.text_color_1_dark
                        )
                        ds.isFakeBoldText = true
                        // 去掉下划线
                        ds.isUnderlineText = false
                    }
                },
                userAgreementStartIndex,
                userAgreementStartIndex + userAgreement.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val privacyPolicy = "《隐私政策》"
        val privacyPolicyStartIndex = spannableString.indexOf(privacyPolicy)
        if (privacyPolicyStartIndex != -1) {
            spannableString.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        SimpleBrowserActivity.launch(
                            this@PhoneLoginActivity,
                            Config.get().privacyUrl,
                            "隐私政策"
                        )
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.color = ContextCompat.getColor(
                            this@PhoneLoginActivity,
                            R.color.text_color_1_dark
                        )
                        ds.isFakeBoldText = true
                        // 去掉下划线
                        ds.isUnderlineText = false
                    }
                },
                privacyPolicyStartIndex,
                privacyPolicyStartIndex + privacyPolicy.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        if (protocolName?.isNotBlank() == true) {
            val protocolNameIndex = spannableString.indexOf(protocolName)
            if (protocolNameIndex != -1) {
                spannableString.setSpan(
                    object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            SimpleBrowserActivity.launch(
                                this@PhoneLoginActivity,
                                mRealVM.flashTestPreFetchNumber.get()?.protocolUrl,
                                protocolName
                            )
                        }

                        override fun updateDrawState(ds: TextPaint) {
                            super.updateDrawState(ds)
                            ds.color = ContextCompat.getColor(
                                this@PhoneLoginActivity,
                                R.color.text_color_1_dark
                            )
                            ds.isFakeBoldText = true
                            // 去掉下划线
                            ds.isUnderlineText = false
                        }
                    },
                    protocolNameIndex - 1,
                    protocolNameIndex + protocolName.length + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        binding.tvAgreementCaptcha.movementMethod = LinkMovementMethod.getInstance()
        binding.tvAgreementCaptcha.highlightColor = Color.TRANSPARENT
        binding.tvAgreementCaptcha.text = spannableString
    }
}