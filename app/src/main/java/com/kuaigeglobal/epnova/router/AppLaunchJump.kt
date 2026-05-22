package com.kuaigeglobal.epnova.router

import android.app.Activity
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.Utils
import com.kuaigeglobal.epnova.common.SP
import com.kuaigeglobal.epnova.feature.BringForegroundActivity
import com.kuaigeglobal.epnova.feature.splash.SplashActivity
import com.kuaigeglobal.epnova.feature.web.SimpleBrowserActivity
import com.nhsd.basic.utils.Logger

/**
 * APP启动后跳转处理
 */
object AppLaunchJump {
    private const val JUMP_URI = "AppLaunchAction.openAppType"
    private val sp get() = SP.getDefaultSP()
    private val activityLifecycleCallbacks by lazy { ActivityLifecycleCallbacks() }

    /**
     * 置顶Activity
     */
    private fun popActivity(cls: Class<out Activity>): Boolean {
        if (ActivityUtils.isActivityExistsInStack(cls)) {
            val stack = ActivityUtils.getActivityList()
            for (activity in stack) {
                if (activity.javaClass == cls) {
                    return true
                }
                activity.finish()
            }
        }
        return false
    }

    /**
     * 启动APP并跳转jumpUri
     */
    @JvmStatic
    fun launchJump(jumpUri: Uri) {
        ActivityUtils.addActivityLifecycleCallbacks(activityLifecycleCallbacks)
        sp.putString(JUMP_URI, jumpUri.toString())
        if (ActivityUtils.getActivityList().isEmpty()) {
            ActivityUtils.startLauncherActivity()
        } else {
            ActivityUtils.startActivity(BringForegroundActivity::class.java)
        }
    }

    /**
     * APP已启动
     */
    private fun onAppLaunched(activity: FragmentActivity) {
        ActivityUtils.removeActivityLifecycleCallbacks(activityLifecycleCallbacks)
        val jumpUri = sp.getString(JUMP_URI, "")
        sp.remove(JUMP_URI)
        if (jumpUri.isNullOrEmpty()) {
            return
        }
        jump(activity, Uri.parse(jumpUri))
    }

    /**
     * 跳转uri
     */
    private fun jump(activity: FragmentActivity, uri: Uri) {
        Logger.i("push jump $activity")
        AppRouter.jump(uri)
    }


    private class ActivityLifecycleCallbacks : Utils.ActivityLifecycleCallbacks() {
        override fun onActivityResumed(activity: Activity) {
            super.onActivityResumed(activity)
            if (activity is FragmentActivity
                && activity !is SplashActivity
                && activity !is BringForegroundActivity
                && activity !is SimpleBrowserActivity
            ) {
                onAppLaunched(activity)
            }
        }
    }
}
