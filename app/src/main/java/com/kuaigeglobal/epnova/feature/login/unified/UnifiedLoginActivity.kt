package com.kuaigeglobal.epnova.feature.login.unified

import android.content.Intent
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.ioScope
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.facebook.login.FacebookSignInHelper
import com.kuaigeglobal.epnova.core.google.login.GoogleSignInHelper
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.databinding.ActivityUnifiedLoginBinding
import com.kuaigeglobal.epnova.feature.settings.SettingsActivity
import com.kuaigeglobal.epnova.feature.web.SimpleBrowserActivity
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.launchNoCrash
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:聚合登录
 * @Data: 2026/2/11/周三  11:37
 * @Version: 1.0
 * *****************************************
 */
class UnifiedLoginActivity : BaseViewModelActivity<ActivityUnifiedLoginBinding, UnifiedLoginViewModel>() {

    companion object {
        var checkedPolicy = false
        fun start(showToast: Boolean = false) {
            if (showToast) {
                showToast(R.string.toast_login_please)
            }
            val activity = topActivity ?: return
            activity.startActivity(Intent(activity, UnifiedLoginActivity::class.java))
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

    val googleSignInHelper by lazy { GoogleSignInHelper(this) }
    val facebookSignInHelper by lazy { FacebookSignInHelper(this) }


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

            R.id.btnFaceBook -> {
                facebookSignInHelper.signIn(
                    onSuccess = { body ->
                        mRealVM.loginByThird(body) {
                            finishOnly()
                        }
                    },
                    onFailure = { msg ->
                        msg?.showToast()
                    }
                )
            }

            R.id.btnGoogle -> {

                googleSignInHelper.signIn(
                    onSuccess = { body ->
                        mRealVM.loginByThird(body) {
//                                    Log.i("Hello","data "+it)
                            finishOnly()
                        }
                    },
                    onFailure = { msg ->
                        msg?.toString()?.showToast()
                    }
                )
            }


            R.id.bt_settings -> {
                SettingsActivity.start(this)
            }

            R.id.ll_describe -> {
                //协议
                mRealVM.checkedPolicy.set(!mRealVM.checkedPolicy.get())
            }
        }
    }

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
            SpannableString(getString(R.string.agree_terms_privacy))
        val userAgreement = getString(R.string.app_user_agreement)
        val userAgreementStartIndex = spannableString.indexOf(userAgreement)
        if (userAgreementStartIndex != -1) {
            spannableString.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        SimpleBrowserActivity.launch(
                            this@UnifiedLoginActivity,
                            Config.get().serviceUrl,
                            userAgreement
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

        val privacyPolicy = getString(R.string.app_privacy_policy)
        val privacyPolicyStartIndex = spannableString.indexOf(privacyPolicy)
        if (privacyPolicyStartIndex != -1) {
            spannableString.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        SimpleBrowserActivity.launch(
                            this@UnifiedLoginActivity,
                            Config.get().privacyUrl,
                            privacyPolicy
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


    override fun onDestroy() {
        if (User.get().isLogin && User.get().loginRewardsValid == 2) {
            ioScope.launchNoCrash {
                val response = ApiFactory.apiService.reportLoginTask()
                withContext(Dispatchers.Main) {
                    if (response.isSuccess) {
                        showRewardToast(response.data?.rewardLabel)
                    } else {
                        response.msg?.showToast()
                    }
                }
            }
        }
        super.onDestroy()
    }

    private fun showRewardToast(rewardText: String?) {
        val activity = topActivity ?: return
        val decorView = activity.window.decorView as ViewGroup

        val toastView = LayoutInflater.from(activity)
            .inflate(R.layout.toast_reward_coins, decorView, false)
        toastView.findViewById<TextView>(R.id.tvCoinsAmount).text =
            activity.getString(R.string.home_login_reward_coins, rewardText)

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )
        // 进场：缩放 + 淡入
        toastView.scaleX = 0.5f
        toastView.scaleY = 0.5f
        toastView.alpha = 0f
        decorView.addView(toastView, params)
        toastView.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(250)
            .start()

        // 2s 后淡出并移除
        toastView.postDelayed({
            toastView.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction { decorView.removeView(toastView) }
                .start()
        }, 2000)
    }


    override fun finish() {
        super.finish()
        overridePendingTransition(enterAnim, exitAnim)
    }
}