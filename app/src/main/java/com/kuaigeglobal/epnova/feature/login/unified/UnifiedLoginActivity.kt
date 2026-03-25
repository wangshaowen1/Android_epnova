package com.kuaigeglobal.epnova.feature.login.unified

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
import com.kuaigeglobal.epnova.core.network.apis.request.LoginByThirdBody
import com.kuaigeglobal.epnova.databinding.ActivityUnifiedLoginBinding
import com.kuaigeglobal.epnova.feature.login.LoginActivity
import com.kuaigeglobal.epnova.feature.login.PrivacyPolicyDialog
import com.kuaigeglobal.epnova.feature.login.phone.PhoneLoginActivity
import com.kuaigeglobal.epnova.feature.settings.SettingsActivity
import com.kuaigeglobal.epnova.feature.web.SimpleBrowserActivity
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:聚合登录
 * @Data: 2026/2/11/周三  11:37
 * @Version: 1.0
 * *****************************************
 */
class UnifiedLoginActivity : BaseViewModelActivity<ActivityUnifiedLoginBinding, UnifiedLoginViewModel>(){

    companion object {
        var checkedPolicy = false
        fun start(showToast: Boolean = false) {
            if (showToast) {
                showToast(R.string.toast_login_please)
            }
            val activity = topActivity ?: return
            activity.startActivity(Intent(activity, LoginActivity::class.java))
            activity.overridePendingTransition(
                R.anim.push_bottom_in,
                R.anim.push_bottom_out
            )
        }
    }


    private var enterAnim = 0
    private var exitAnim = 0


    override fun onViewModelCreated() {
        super.onViewModelCreated()
        termsOfService()
    }


    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.btnClose -> {
                finishOnly()
            }

//            R.id.btnWechat -> {
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

            R.id.btnPhone -> {
                checkedPolicy = mRealVM.checkedPolicy.get()
                PhoneLoginActivity.start(false, checkedPolicy = mRealVM.checkedPolicy.get())
                finishOnly()
            }

            R.id.bt_settings->{
                SettingsActivity.start(this)
            }
            R.id.ll_describe->{
                //协议
                mRealVM.checkedPolicy.set(!mRealVM.checkedPolicy.get())
            }
        }
    }

//    override fun onStart(platform: SHARE_MEDIA?) {
//        Logger.d("onStart platform:$platform")
//    }
//
//    override fun onAuthed(platform: SHARE_MEDIA?, data: MutableMap<String, String>?) {
//        Logger.d("onAuthed platform:$platform data:$data")
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
//    override fun onError(e: Exception?, platform: SHARE_MEDIA?, onCancel: Boolean) {
//        Logger.d("onError e:$e platform:$platform onCancel:$onCancel")
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
        val spannableString =
            SpannableString( "登录即代表同意和《用户协议》和《隐私政策》")
        val userAgreement = "《用户协议》"
        val userAgreementStartIndex = spannableString.indexOf(userAgreement)
        if (userAgreementStartIndex != -1) {
            spannableString.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        SimpleBrowserActivity.launch(
                            this@UnifiedLoginActivity,
                            Config.get().serviceUrl,
                            "用户协议"
                        )
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.color = ContextCompat.getColor(
                            this@UnifiedLoginActivity,
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
                            this@UnifiedLoginActivity,
                            Config.get().privacyUrl,
                            "隐私政策"
                        )
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.color = ContextCompat.getColor(
                            this@UnifiedLoginActivity,
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


        binding.tvAgreement.movementMethod = LinkMovementMethod.getInstance()
        binding.tvAgreement.highlightColor = Color.TRANSPARENT
        binding.tvAgreement.text = spannableString
    }


    override fun finish() {
        super.finish()
        overridePendingTransition(enterAnim, exitAnim)
    }
}