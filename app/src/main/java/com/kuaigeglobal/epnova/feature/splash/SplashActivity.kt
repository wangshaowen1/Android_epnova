package com.kuaigeglobal.epnova.feature.splash

import android.os.Bundle
import android.text.TextUtils
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ActivityUtils
import com.gyf.immersionbar.ImmersionBar
import com.kuaigeglobal.epnova.App
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.databinding.ActivitySplashBinding
import com.kuaigeglobal.epnova.feature.main.MainActivity
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            val splashScreen = installSplashScreen()
            splashScreen.setOnExitAnimationListener { it.remove() }
            super.onCreate(savedInstanceState)
            if (!isTaskRoot) {
                intent.data?.takeIf { !TextUtils.isEmpty(it.host) }?.toString()?.also {
                    if (ActivityUtils.getActivityList().find { activity -> activity.javaClass == MainActivity::class.java } != null) {
                        AppRouter.jump(it)
                        finish()
                    } else {
                        jumpToTheHomepage()
                    }
                } ?: run { finish() }
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
        val it = intent.data?.takeIf { !TextUtils.isEmpty(it.host) } ?: ""
        MainActivity.start(this@SplashActivity, it.toString())
        finish()
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
