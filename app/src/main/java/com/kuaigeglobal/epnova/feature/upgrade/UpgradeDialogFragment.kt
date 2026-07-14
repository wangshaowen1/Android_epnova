package com.kuaigeglobal.epnova.feature.upgrade

import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import com.blankj.utilcode.util.ScreenUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.EmptyViewModel
import com.kuaigeglobal.epnova.common.utils.FileUtils
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.databinding.DialogUpgradeBinding
import com.nhsd.basic.BaseVMDialogFragment
import com.nhsd.basic.bus.observeEvents
import java.io.File
import kotlin.math.min


class UpgradeDialogFragment : BaseVMDialogFragment<DialogUpgradeBinding, EmptyViewModel>() {


    var onUpgradeClick: (() -> Unit)? = null
    val force by lazy { arguments?.getBoolean(FORCE_UPGRADE, false) }

//    private var lifecycleCallbacksRegistered = false
//    private var bringToFrontPosted = false
//    private val fragmentLifecycleCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
//        override fun onFragmentStarted(fragmentManager: FragmentManager, fragment: Fragment) {
//            if (fragment is DialogFragment && fragment !== this@UpgradeDialogFragment) {
//                bringToFront()
//            }
//        }
//    }
//
//    private fun bringToFront() {
//        val upgradeDialog = dialog?.takeIf { it.isShowing } ?: return
//        val decorView = upgradeDialog.window?.decorView ?: return
//        if (bringToFrontPosted) return
//        bringToFrontPosted = true
//        decorView.post {
//            bringToFrontPosted = false
//            if (!isAdded ||
//                !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) ||
//                dialog !== upgradeDialog ||
//                !upgradeDialog.isShowing
//            ) {
//                return@post
//            }
//
//            // Window 层级取决于添加顺序，重新加入以覆盖后续弹出的 DialogFragment。
//            upgradeDialog.setOnDismissListener(null)
//            upgradeDialog.dismiss()
//            upgradeDialog.show()
//            upgradeDialog.setOnDismissListener(this@UpgradeDialogFragment)
//        }
//    }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.version.text = arguments?.getString(VERSION_TEXT)
        binding.content.text = arguments?.getString(UPGRADE_DESCRIPTION)
        if (force == true) {
            dialog?.setCancelable(false)
        }
        binding.cancel.visibility = force?.let {
            if (it) View.GONE else View.VISIBLE
        } ?: View.GONE
        binding.close.visibility = force?.let {
            if (it) View.GONE else View.VISIBLE
        } ?: View.GONE

        observeEvents {
            on<Events.DownloadProgressEvent> { event ->
                if (isAdded) {
                    binding.upgrade.text = "${event.percent}%"
                    if (event.percent == 100) {
                        if (force == false) {
                            dismissAllowingStateLoss()
                        } else if (!TextUtils.isEmpty(event.path)) {
                            updateInstall(event.path)
                        }
                    }
                }
            }
        }
    }

    fun updateInstall(path: String) {
        if (isAdded) {
            binding.upgrade.text = "立即安装"
            binding.upgrade.isEnabled = true
            binding.upgrade.setOnClickListener {
                FileUtils.openFile(context, File(path))
                if (force == false) {
                    dismissAllowingStateLoss()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
//        if (!lifecycleCallbacksRegistered) {
//            parentFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, true)
//            lifecycleCallbacksRegistered = true
//        }
        dialog?.window?.let { dialogWindow ->
            dialogWindow.attributes?.let { attr ->
                val screenWidth = min(ScreenUtils.getScreenWidth(), ScreenUtils.getScreenHeight())
                attr.width = (294 / 375f * screenWidth).toInt()
                attr.gravity = Gravity.CENTER
                // 不透明遮罩
                attr.dimAmount = if (force == true) 1f else 0.5f
                dialogWindow.attributes = attr
            }
        }
    }

//    override fun onStop() {
//        if (lifecycleCallbacksRegistered) {
//            parentFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks)
//            lifecycleCallbacksRegistered = false
//        }
//        bringToFrontPosted = false
//        super.onStop()
//    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.upgrade -> {
                onUpgradeClick?.invoke()
            }

            R.id.cancel, R.id.close -> dismissAllowingStateLoss()
        }
    }

    companion object {

        const val VERSION_TEXT: String = "versionText"
        const val UPGRADE_DESCRIPTION: String = "upgradeDescription"
        const val FORCE_UPGRADE: String = "forceUpgrade"

        fun show(manager: FragmentManager, versionText: String?, upgradeDescription: String?, forceUpgrade: Boolean): UpgradeDialogFragment {
            val dialog = UpgradeDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(VERSION_TEXT, versionText)
                    putString(UPGRADE_DESCRIPTION, upgradeDescription)
                    putBoolean(FORCE_UPGRADE, forceUpgrade)
                }
            }
            dialog.show(manager)
            return dialog
        }
    }
}