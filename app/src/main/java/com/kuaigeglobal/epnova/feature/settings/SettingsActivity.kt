package com.kuaigeglobal.epnova.feature.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.Log
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.BuildConfig
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.showConfirmPrompt
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.common.utils.DebugUtils
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.openTheCustomerServiceChat
import com.kuaigeglobal.epnova.databinding.ActivitySettingsBinding
import com.kuaigeglobal.epnova.feature.login.flash.PhoneLoginDialogActivity
import com.kuaigeglobal.epnova.feature.mine.PhoneShowActivity
import com.kuaigeglobal.epnova.feature.settings.about.AboutUsActivity
import com.kuaigeglobal.epnova.feature.settings.feedback.FeedbackActivity
import com.kuaigeglobal.epnova.feature.settings.lang.LanguagesActivity
import com.kuaigeglobal.epnova.feature.settings.logoff.LogoffActivity
import com.kuaigeglobal.epnova.feature.web.SimpleBrowserActivity
import com.kuaigeglobal.epnova.kit.SignUtils
import com.kuaigeglobal.epnova.router.AppNavigator
import com.kuaigeglobal.epnova.widget.actionsheet.ActionSheetHelper
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:设置
 * @Data: 2025/4/18  16:03
 * @Version: 1.0
 * *****************************************
 */
class SettingsActivity : BaseViewModelActivity<ActivitySettingsBinding, SettingsViewModel>() {
    companion object {
        fun start(activity: Activity?) {
            activity ?: return
            val intent = Intent(activity, SettingsActivity::class.java)
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (DebugUtils.isDebug()) {
            binding.etIp.setText(Config.get().baseUrlCustom)
            binding.etIp.setSelection(binding.etIp.length())
            binding.btnIPConfirm.setOnSingleClickListener {
                val url = mRealVM.debugIp.get() ?: BuildConfig.SERVER_URL
                Config.get().storeBaseUrlCustom(url)
                "设置好了，自己杀掉app重启一下！".showToast()
            }
            binding.btnIPSelect.setOnSingleClickListener {
                ActionSheetHelper.createDialog(DebugUtils.ipsName) { _, position ->
                    val ip = DebugUtils.ips[position]
                    Config.get().storeBaseUrlCustom(ip)
                    binding.etIp.setText(ip)
                    binding.etIp.setSelection(binding.etIp.length())
                    "设置好了，自己杀掉app重启一下！".showToast()
                }.show(supportFragmentManager)
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        if (User.get().isVisitor) {
            finish()
        }
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)

        when (view?.id) {
            R.id.sll_personal_information -> {
                if (AppNavigator.checkLoginIntercepted(true)) {
                    return
                }
                //个人资料
            }

            R.id.sll_about_us -> {
                //关于我们
                AboutUsActivity.start(this)
            }

            R.id.sll_contact_customer_service -> {
                //联系客服
                openTheCustomerServiceChat()
            }

            R.id.sll_suggestion_and_feedback -> {
                //建议反馈
//                FeedbackActivity.start(this)
                LanguagesActivity.start(this)
            }

            R.id.sll_cancel_the_account -> {
                //注销账号
                if (AppNavigator.checkLoginIntercepted(true)) {
                    return
                }
                LogoffActivity.start(this)
            }

            R.id.stv_log_out -> {
                if (AppNavigator.checkLoginIntercepted(true)) {
                    return
                }
                showConfirmPrompt(tip = R.string.tip_sign_out_confirm.getResString()) {

                    Global.guestLogin({

                        lifecycleScope.launch {

                            withContext(Dispatchers.IO)
                            {
                                mRealVM.logoutApp()
                                SignUtils.signOut()
                                User.get().updateFrom(it)
                                SignUtils.signIn()
                            }
                            getString(R.string.app_log_out).showToast()
                            finish()
                        }
                    },{
                        //todo 游客登录失败
                    })
                }
            }

            R.id.sll_phone -> {
                if (AppNavigator.checkLoginIntercepted(true)) {
                    return
                }
                if (User.get().phoneNum.isNullOrEmpty()) {
                    PhoneLoginDialogActivity.start(this, true)
                } else {
                    PhoneShowActivity.start(this)
                }
            }

            R.id.ll_user_agreement -> {
                //用户协议
                SimpleBrowserActivity.launch(
                    this,
                    Config.get().serviceUrl,
                    getString(R.string.app_user_agreement)
                )
            }

            R.id.ll_privacy_policy -> {
                //隐私政策
                SimpleBrowserActivity.launch(
                    this,
                    Config.get().privacyUrl,
                    getString(R.string.app_privacy_policy)
                )
            }

            R.id.ll_qa -> {
                SimpleBrowserActivity.launch(this, Config.get().appConfig.appHelpCenterUrl)
            }

            R.id.sll_filing_number -> {
                //备案号
                SimpleBrowserActivity.launch(
                    this,
                    Config.get().appConfig.beianNoJumpUrl,
                    getString(R.string.app_filing_number)
                )
            }

            R.id.ll_shared_list -> {
                //第三方信息共享清单
                SimpleBrowserActivity.launch(
                    this,
                    Config.get().appConfig.thirdPartyInfoShareListUrl,
                    getString(R.string.app_shared_list)
                )
            }

            R.id.suggestions_feedback -> {
                if (AppNavigator.checkLoginIntercepted(false)) {
                    return
                }
                //建议与反馈
//                FeedbackActivity.start(this)
                LanguagesActivity.start(this)
            }
        }
    }
}