package com.kuaigeglobal.epnova.core.manager

// TODO: Feature
import android.app.ProgressDialog
import android.content.Intent
import android.text.TextUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.blankj.utilcode.util.NetworkUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.ioScope
import com.kuaigeglobal.epnova.common.permissions.PermissionsUtils
import com.kuaigeglobal.epnova.common.permissions.requestRxPermissions
import com.kuaigeglobal.epnova.common.utils.FileUtils
import com.kuaigeglobal.epnova.common.utils.Utils
import com.kuaigeglobal.epnova.core.network.apis.ApiFactory
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import com.kuaigeglobal.epnova.core.network.apis.response.UpgradeData
import com.kuaigeglobal.epnova.feature.upgrade.UpgradeDialogFragment
import com.kuaigeglobal.epnova.service.DownloadService
import com.nhsd.basic.BaseActivity
import com.nhsd.basic.Session
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.isActive
import com.nhsd.basic.ext.launchNoCrash
import com.nhsd.basic.ext.showToast
import com.nhsd.basic.manager.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class UpgradeManager(private val context: BaseActivity) {

    private var mProgressDialog: ProgressDialog? = null
    var needForceUpdate = false
        private set
    private var dialog: UpgradeDialogFragment? = null
    private var isRequesting = false
    private val autoUpgrade = false
    private var upgradeData: UpgradeData? = null

    private var onUpgradeListener: OnUpgradeListener? = null

    interface OnUpgradeListener {
        fun onUpgrade(needUpgrade: Boolean)
    }

    init {
        // 替代 EventBus.register(this)
        observeEvents()
    }

    /**
     * 监听全局事件
     * 使用 Activity 的 lifecycleScope，确保生命周期安全
     */
    private fun observeEvents() {
        context.lifecycleScope.launch {
            // 当 Activity 至少处于 STARTED 状态时才接收事件 (避免后台弹窗崩溃)
            context.repeatOnLifecycle(Lifecycle.State.STARTED) {
                FlowEventBus.events.collect { event ->
                    if (event is DownloadService.DownloadApkEvent) {
                        onFinishDownload(event)
                    }
                }
            }
        }
    }

    fun setOnUpgradeListener(onUpgradeListener: OnUpgradeListener?) {
        this.onUpgradeListener = onUpgradeListener
    }

    @Synchronized
    fun upgrade(showProgress: Boolean) {
        if (isRequesting) return
        if (Utils.isServiceRunning(context, DownloadService::class.java.name)) return

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

                    val url = data.upgradeUrl
                    val md5 = data.md5Hash

                    if (!data.isNeedUpdate()) {
                        if (showProgress) {
                            R.string.already_latest_version.getResString().showToast()
                        }
                    } else {
                        needForceUpdate = data.isNeedForceUpdate()

                        val file = File(
                            FileManager.getInstance().apkFolder,
                            DownloadService.UPDATE_FILE_NAME
                        )

                        // 本地 APK 存在且 MD5 符合 → 直接安装
                        if (file.exists() && file.isFile && md5 != null) {
                            if (TextUtils.equals(md5, FileUtils.getFileMD5(file))) {
                                showInstallDialog(file.path)
                                onUpgradeListener?.onUpgrade(true)
                                return@withContext
                            }
                        }

                        // 不符合或未下载 → 下载或显示升级提示
                        if (!TextUtils.isEmpty(url)) {
                            if (autoUpgrade && NetworkUtils.isWifiConnected()) {
                                downApk(url, md5, true)
                            } else {
                                showUpgradeDialog(data)
                            }
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


    private fun downApk(url: String?, md5: String?, autoUpdate: Boolean) {
        context.requestRxPermissions(
            delayText = "升级新版本需要访问手机存储，请授予SD卡权限",
            permissions = PermissionsUtils.getAndroid12WritePermission()
        ) { aBoolean ->
            if (aBoolean) {
                val intent = Intent(context, DownloadService::class.java)
                intent.putExtra(DownloadService.DOWNLOADURL, url)
                intent.putExtra(DownloadService.MD5_HASH, md5)

                if (autoUpdate) {
                    R.string.wifi_auto_download_update.getResString(Session.getInstance().appName).showToast()
                    intent.putExtra(DownloadService.DOWNLOAD_WITH_NOTIFY, true)
                }
                context.startService(intent)
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
            upgradeData.appVersionDesc,
            upgradeData.isNeedForceUpdate()
        )
        dialog?.onUpgradeClick = {
            if (!needForceUpdate) {
                dialog?.dismissAllowingStateLoss()
            }
            downApk(upgradeData.upgradeUrl, upgradeData.md5Hash, false)
            null
        }
    }

    // 替代原 @Subscribe 方法
    private fun onFinishDownload(downloadApkEvent: DownloadService.DownloadApkEvent) {
        showInstallDialog(downloadApkEvent.path)
    }

    private fun showInstallDialog(path: String) {
        if (dialog != null && dialog!!.isShowing) {
            return
        }
        val data = upgradeData ?: return

        dialog = UpgradeDialogFragment.show(
            context.supportFragmentManager,
            data.appVersion,
            data.appVersionDesc,
            data.isNeedForceUpdate()
        )
        dialog?.onUpgradeClick = {
            dialog?.dismissAllowingStateLoss()
            FileUtils.openFile(context, File(path))
            null
        }

    }

    fun release() {
        onUpgradeListener = null
        // EventBus.getDefault().unregister(this) // 不需要了，协程会自动取消
        mProgressDialog?.dismiss()
        mProgressDialog = null
    }
}