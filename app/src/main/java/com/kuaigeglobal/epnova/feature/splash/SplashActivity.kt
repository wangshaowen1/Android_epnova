package com.kuaigeglobal.epnova.feature.splash

import android.os.Bundle
import android.text.TextUtils
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ActivityUtils
import com.gyf.immersionbar.ImmersionBar
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.google.messaging.MyFirebaseMessagingService.Companion.KEY_PUSH_MSG_JUMP_URI
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.databinding.ActivitySplashBinding
import com.kuaigeglobal.epnova.feature.main.MainActivity
import com.kuaigeglobal.epnova.router.AppNavigator
import com.kuaigeglobal.epnova.router.AppRouter
import com.nhsd.basic.BaseViewModelActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * 启动页
 */
class SplashActivity : BaseViewModelActivity<ActivitySplashBinding, SplashViewModel>(),
    PolicyDialog.PolicyCallback {
    companion object {
        private const val TAG = "SplashActivity"
        private const val EXTRA_SCHEME = "scheme"
        private const val EXTRA_FROM_PUSH = "from_push"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            val splashScreen = installSplashScreen()
            splashScreen.setOnExitAnimationListener { it.remove() }
            super.onCreate(savedInstanceState)

            if (!isTaskRoot) {
                val schemeUri = getSchemeUriFromIntent()
                if (!schemeUri.isNullOrEmpty()) {
                    if (ActivityUtils.getActivityList().find { activity -> activity.javaClass == MainActivity::class.java } != null) {
                        AppRouter.jump(schemeUri)
                        finish()
                    } else {
                        jumpToTheHomepage()
                    }
                } else {
                    finish()
                }
                return
            }
            next()
        } catch (e: Throwable) {
            e.printStackTrace()
            next()
        }
    }

    private fun next() {


        if (!App.instance.hasAgree()) {
            PolicyDialog().show(supportFragmentManager)
            return
        }

        lifecycleScope.launch {

            initAdsReporter()
            delay(2000)
            jumpToTheHomepage()
        }
    }

    private fun jumpToTheHomepage() {
        if (intent.getBooleanExtra(EXTRA_FROM_PUSH, false)) {
            EventManager.onFirebaseTrack(FirebaseEvent.PUSH_CLICK)
        }
        val schemeUri = getSchemeUriFromIntent()
        MainActivity.start(this@SplashActivity, schemeUri)
        finish()
    }

    private fun getSchemeUriFromIntent(): String? {
        return intent.getStringExtra(EXTRA_SCHEME)
            ?.takeIf { !TextUtils.isEmpty(it) }
            // FCM notification + data 后台自动展示时，点击会把 data 放到 launcher extra。
            ?: intent.getStringExtra(KEY_PUSH_MSG_JUMP_URI)
                ?.takeIf { !TextUtils.isEmpty(it) }
            // HTTPS 链接（AppsFlyer OneLink / Universal Link）不在此手动路由，
            // 由 AppsFlyerManager.subscribeForDeepLink 回调统一处理。
            ?: intent.data
                ?.takeIf { !TextUtils.isEmpty(it.host) && it.scheme != "https" && it.scheme != "http" }
                ?.toString()
    }


    override fun setTranslucentStatus() {
        ImmersionBar.with(this)
            .statusBarDarkFont(false)
            .transparentStatusBar()
            .transparentNavigationBar()
            .init()
    }

    /**
     * 归因处理
     */
    private fun initAdsReporter() {

    }

    override fun onAgreePolicy(dialog: PolicyDialog) {

        dialog.dismissAllowingStateLoss()
        Config.get().markAppOpenedWithNotify()
        initAdsReporter()
        App.instance.initSdkAfterAgreeLicense()
        jumpToTheHomepage()
    }


    override fun onRejectPolicy(dialog: PolicyDialog) {
        finish()
    }
}
