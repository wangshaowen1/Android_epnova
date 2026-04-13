package com.kuaigeglobal.epnova.feature.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.gyf.immersionbar.ImmersionBar
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.network.apis.request.LoginByThirdBody
import com.kuaigeglobal.epnova.databinding.DialogLoginBinding
import com.kuaigeglobal.epnova.router.AppNavigator
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.utils.Logger

class LoginActivity : BaseViewModelActivity<DialogLoginBinding, LoginViewModel>(){

    companion object {
        var checkedPolicy = false
        fun show(showToast: Boolean = false) {
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.btnClose -> {
                finishOnly()
            }

            R.id.btnWechat -> {
                if (mRealVM.checkedPolicy.get()) {
//                    shareManager.login(SHARE_MEDIA.WEIXIN, this)
                    return
                }
                val dialog = PrivacyPolicyDialog.show(supportFragmentManager)
                dialog.callback = callback@{ agree ->
                    if (!agree) return@callback
                    mRealVM.checkedPolicy.set(true)
//                    shareManager.login(SHARE_MEDIA.WEIXIN, this)
                }
            }

            R.id.btnPhone -> {
                checkedPolicy = mRealVM.checkedPolicy.get()
//                FlashLoginDialogFragment.show(
//                    supportFragmentManager,
//                    false,
//                    checkedPolicy = mRealVM.checkedPolicy.get()
//                )
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
//                finish()
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

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(false)
            .navigationBarColor(R.color.white)
            .transparentStatusBar()
            .transparentNavigationBar()
            .navigationBarDarkIcon(false)
            .init()
    }

    private var enterAnim = 0
    private var exitAnim = 0


    /**
     * finish动画处理
     */
    private fun finishOnly() {
        enterAnim = 0
        exitAnim = R.anim.push_bottom_out
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(enterAnim, exitAnim)
    }
}