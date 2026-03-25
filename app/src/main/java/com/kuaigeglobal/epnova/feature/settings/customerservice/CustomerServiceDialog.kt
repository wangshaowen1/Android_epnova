package com.kuaigeglobal.epnova.feature.settings.customerservice

import android.view.Gravity
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.ScreenUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.permissions.requestRead2WritePermissions
import com.kuaigeglobal.epnova.databinding.DialogCustomerServiceBinding
import com.nhsd.basic.BaseVMDialogFragment

/**
 * 微信客服二维码
 */
class CustomerServiceDialog : BaseVMDialogFragment<DialogCustomerServiceBinding, CustomerServiceViewModel>() {

    companion object {
        fun show() {
            val activity = ActivityUtils.getTopActivity() as? FragmentActivity ?: return
            CustomerServiceDialog().show(activity.supportFragmentManager)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.attributes?.let { attr ->
                attr.width = (ScreenUtils.getScreenWidth() * 287f / 375f).toInt()
                attr.gravity = Gravity.CENTER
                dialogWindow.attributes = attr
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        binding.card.setOnLongClickListener {
            requestAlbumsPermissionAndSave(binding.card)
            true
        }
    }

    /**
     * 请求权限并保存图片
     * @param view 需要保存为图片的View
     */
    private fun requestAlbumsPermissionAndSave(view: View? = null) {
        requestRead2WritePermissions { granted ->
            if (granted) {
                //权限申请成功
                mRealVM.saveImage(view = view)
            } else {
                //权限申请失败
            }
        }
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.btnClose -> dismissAllowingStateLoss()
        }
    }
}