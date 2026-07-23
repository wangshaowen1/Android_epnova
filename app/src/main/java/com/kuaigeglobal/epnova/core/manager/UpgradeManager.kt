package com.kuaigeglobal.epnova.core.manager

import android.app.Activity
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.net.toUri
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.ioScope
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import com.kuaigeglobal.epnova.core.network.apis.response.UpgradeData
import com.kuaigeglobal.epnova.feature.upgrade.UpgradeDialogFragment
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.nhsd.basic.BaseActivity
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.isActive
import com.nhsd.basic.ext.launchNoCrash
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UpgradeManager(private val context: BaseActivity) {

    private var mProgressDialog: ProgressDialog? = null
    var needForceUpdate = false
        private set
    private var dialog: UpgradeDialogFragment? = null
    private var isRequesting = false
    private var isImmediateUpdateFlowActive = false
    private var upgradeData: UpgradeData? = null
    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(context) }

    private var onUpgradeListener: OnUpgradeListener? = null

    interface OnUpgradeListener {
        fun onUpgrade(needUpgrade: Boolean)
    }

    fun setOnUpgradeListener(onUpgradeListener: OnUpgradeListener?) {
        this.onUpgradeListener = onUpgradeListener
    }

    @Synchronized
    fun upgrade(showProgress: Boolean) {
        if (isRequesting) return

        if (showProgress) {
            showUpgradeDialog()
        }

        isRequesting = true

        ioScope.launchNoCrash {
            try {
                // 1. 请求接口（IO 线程）
                val data = withContext(Dispatchers.IO) {
                    ApiFactory.apiService.upgrade(UserTokenBody()).data
                }

                // 切回主线程处理 UI 与逻辑
                withContext(Dispatchers.Main) {
                    if (!context.isActive) {
                        onUpgradeListener?.onUpgrade(false)
                        return@withContext
                    }

                    if (data == null) {
                        onUpgradeListener?.onUpgrade(false)
                        if (showProgress) mProgressDialog?.dismiss()
                        return@withContext
                    }

                    upgradeData = data

                    if (!data.isNeedUpdate()) {
                        needForceUpdate = false
                        if (showProgress) {
                            R.string.already_latest_version.getResString().showToast()
                        }
                    } else {
                        needForceUpdate = data.isNeedForceUpdate()
                        if (needForceUpdate) {
                            startImmediateUpdate(data)
                        } else {
                            showUpgradeDialog(data)
                        }
                    }
                    onUpgradeListener?.onUpgrade(data.isNeedUpdate())
                }

            } catch (e: Throwable) {
                // 请求失败
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onUpgradeListener?.onUpgrade(false)
                    mProgressDialog?.dismiss()
                }
            } finally {
                // 必须执行
                isRequesting = false
                withContext(Dispatchers.Main) {
                    mProgressDialog?.dismiss()
                }
            }
        }
    }

    fun resumeImmediateUpdate() {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (!context.isActive) return@addOnSuccessListener
                if (appUpdateInfo.updateAvailability() ==
                    UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                ) {
                    launchImmediateUpdate(appUpdateInfo)
                }
            }
    }

    private fun showUpgradeDialog() {
        mProgressDialog = ProgressDialog(context).apply {
            setMessage("正在检查新版本")
            setProgressStyle(ProgressDialog.STYLE_SPINNER)
            show()
        }
    }

    private fun showUpgradeDialog(upgradeData: UpgradeData) {
        if (dialog != null && dialog!!.isShowing) {
            return
        }
        dialog = UpgradeDialogFragment.show(
            context.supportFragmentManager,
            upgradeData.appVersion,
            upgradeData.appVersionDesc
        )
        dialog?.onUpgradeClick = {
            dialog?.dismissAllowingStateLoss()
            openGooglePlay()
            null
        }
    }

    private fun startImmediateUpdate(data: UpgradeData) {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (!context.isActive) return@addOnSuccessListener
                val updateAvailable =
                    appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ||
                            appUpdateInfo.updateAvailability() ==
                            UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                if (updateAvailable &&
                    appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                ) {
                    launchImmediateUpdate(appUpdateInfo)
                } else {
                    fallbackToSuggestedUpdate(data)
                }
            }
            .addOnFailureListener {
                if (context.isActive) {
                    fallbackToSuggestedUpdate(data)
                }
            }
    }

    private fun launchImmediateUpdate(appUpdateInfo: AppUpdateInfo) {
        if (isImmediateUpdateFlowActive) return
        isImmediateUpdateFlowActive = true
        appUpdateManager.startUpdateFlow(
            appUpdateInfo,
            context,
            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
        ).addOnSuccessListener { resultCode ->
            isImmediateUpdateFlowActive = false
            if (resultCode == Activity.RESULT_CANCELED && needForceUpdate && context.isActive) {
                upgradeData?.let(::startImmediateUpdate)
            }
        }.addOnFailureListener {
            isImmediateUpdateFlowActive = false
            upgradeData?.takeIf { context.isActive }?.let(::fallbackToSuggestedUpdate)
        }
    }

    /**
     * 服务端已标记强更但 Play 尚未向当前用户提供版本时，不阻断应用使用。
     */
    private fun fallbackToSuggestedUpdate(data: UpgradeData) {
        needForceUpdate = false
        showUpgradeDialog(data)
    }

    private fun openGooglePlay() {
        val packageName = context.packageName
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()).apply {
                    setPackage("com.android.vending")
                }
            )
        } catch (_: ActivityNotFoundException) {
            // 兜底跳转网页版google play
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://play.google.com/store/apps/details?id=$packageName".toUri()
                )
            )
        }
    }

    fun release() {
        onUpgradeListener = null
        mProgressDialog?.dismiss()
        mProgressDialog = null
    }
}
