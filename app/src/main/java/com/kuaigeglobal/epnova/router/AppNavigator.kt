package com.kuaigeglobal.epnova.router

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.AppUtils
import com.kuaigeglobal.epnova.common.Constant
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.feature.login.flash.PhoneLoginDialogActivity
import com.kuaigeglobal.epnova.feature.login.phone.PhoneLoginActivity
import com.kuaigeglobal.epnova.feature.login.phone.PhoneLoginDialog
import com.kuaigeglobal.epnova.feature.login.unified.UnifiedLoginActivity
import com.kuaigeglobal.epnova.feature.login.unified.UnifiedLoginDialog
import com.kuaigeglobal.epnova.kit.SignUtils

object AppNavigator {

    /**
     * 强制退出并重启
     */
    @JvmStatic
    fun forceLogoutAndRestart()
    {
        if (!User.get().isVisitor) {
            SignUtils.signOut()
            AppUtils.relaunchApp()
        }
    }

    /**
     * 检查是否需要Guest登录
     * 只会返回true(未登录) 返回false(登录)  未登录时 不会强制跳转到登录界面
     */
    @JvmStatic
    fun checkIsNeedToGuestLogin(): Boolean
    {

        if (User.get().isVisitor)
        {

            return true
        }

        return false
    }

    /**
     * 检查是否需要登录
     * 使用场景：点击某个按钮，如果没登录则跳转登录页，返回 true；如果已登录则返回 false，继续执行逻辑。
     * @return true: 已拦截(跳转去登录了) false: 未拦截(用户已登录)
     */
    @JvmStatic
    fun checkLoginIntercepted(
        showToast: Boolean = true,
        isFullScreen: Boolean = true
    ): Boolean {
        if (User.get().isVisitor) {
            startLogin(showToast, isFullScreen)
            return true
        }
        return false
    }

    fun startLogin(
        showToast: Boolean,
        isFullScreen: Boolean = false
    ) {
        if (Constant.SDKConfiguration.IS_SHOW_WE_CHAT.not()) {
            if (isFullScreen) {
                PhoneLoginActivity.start(showToast = showToast, checkedPolicy = false)
            } else if (topActivity != null && topActivity is AppCompatActivity) {
                (topActivity as AppCompatActivity).apply {
                    PhoneLoginDialog.show(
                        showToast = showToast,
                        checkedPolicy = false,
                        manager = this.supportFragmentManager
                    )
                }
            }
            return
        }
        if (isFullScreen) {
            UnifiedLoginActivity.start(showToast = showToast)
        } else if (topActivity != null && topActivity is AppCompatActivity) {
            (topActivity as AppCompatActivity).apply {
                UnifiedLoginDialog.show(
                    showToast = showToast,
                    manager = this.supportFragmentManager
                )
            }
        }
    }
}