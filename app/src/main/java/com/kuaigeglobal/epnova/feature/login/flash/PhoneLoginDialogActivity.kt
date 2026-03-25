package com.kuaigeglobal.epnova.feature.login.flash

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import com.blankj.utilcode.util.KeyboardUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.databinding.DialogPhoneLoginBinding
import com.kuaigeglobal.epnova.feature.login.PrivacyPolicyDialog
import com.kuaigeglobal.epnova.feature.web.SimpleBrowserActivity
import com.gyf.immersionbar.ImmersionBar
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.getResString

/**
 * 手机号登录
 */
class PhoneLoginDialogActivity :
    BaseViewModelActivity<DialogPhoneLoginBinding, PhoneLoginViewModel>() {

    companion object {
        const val PHONE = 21

        const val BIND = "bind"

        //是否选中了隐私政策
        const val CHECKED_POLICY = "checkedPolicy"
        fun start(
            activity: Activity?,
            isBind: Boolean? = false,
            checkedPolicy: Boolean? = false
        ) {
            activity ?: return
            val bundle = Bundle()
            bundle.putBoolean(BIND, isBind ?: false)
            bundle.putBoolean(CHECKED_POLICY, checkedPolicy ?: false)
            val intent = Intent(activity, PhoneLoginDialogActivity::class.java)
            intent.putExtras(bundle)
            activity.startActivity(intent)
        }
    }

    private var showInput = false

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 仅在非 Android 8.0 的设备上设置屏幕方向
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.O) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        }
        KeyboardUtils.registerSoftInputChangedListener(this) {
            mRealVM.onSoftInputChanged(it)
        }
        observeEvents {
            on<Events.AgreePolicyEvent> { event ->
                mRealVM.checkedPolicy.set(true)
                when (event.type) {
                    PHONE -> {
                        binding.btnLogin.performClick()
                    }
                }
            }
        }
    }

    override fun onViewModelCreated() {
        super.onViewModelCreated()
        mRealVM.mobile.observe(this) { mRealVM.checkLoginEnable() }
        mRealVM.code.observe(this) { mRealVM.checkLoginEnable() }

        mRealVM.isMobilePhoneBinding.observe(this) {
            if (!it) return@observe
            mRealVM.isMobilePhoneBinding.value = false
            AccountExclusionDialog.show(supportFragmentManager, true)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!showInput) {
            showInput = true
            binding.etPhone.requestFocus()
        }
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.btnClose -> {
                onBackPressed()
            }

            R.id.btnSend -> {
                mRealVM.sendSmsCode()
            }

            R.id.btnLogin -> {
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

            R.id.btnPolicy -> {
                mRealVM.checkedPolicy.set(!mRealVM.checkedPolicy.get())
            }

            R.id.btnPrivacyPolicy -> {
                SimpleBrowserActivity.launch(
                    this,
                    Config.get().privacyUrl,
                    R.string.tvSYPrivateService.getResString()
                )
            }

            R.id.btnUserPolicy -> {
                SimpleBrowserActivity.launch(
                    this,
                    Config.get().serviceUrl,
                    R.string.tvSYUserService.getResString()
                )
            }
        }
    }

    //手机号登录
    private fun phoneLogin() {
        mRealVM.phoneLogin { phone ->
            finish()
            if (mRealVM.isBind.get()) {
                FlowEventBus.postSync(Events.BindingSuccessfulEvent())
                return@phoneLogin
            }
        }
    }

    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(false)
            .navigationBarColor(R.color.white)
            .transparentStatusBar()
            .transparentNavigationBar()
            .navigationBarDarkIcon(false)
            .init()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(
            R.anim.push_bottom_in,
            R.anim.push_bottom_out
        )
    }
}