package com.kuaigeglobal.epnova.feature.login.flash

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.FragmentManager
import com.blankj.utilcode.util.ScreenUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.databinding.DialogAccountExclusionBinding
import com.nhsd.basic.BaseDialogFragment
import com.nhsd.basic.ext.setOnSingleClickListener


/**
 * *****************************************
 * 项目名称: 超会聊
 * @Author: SZYMN
 * @Introduce:
 * @Data: 2024/8/27  11:25
 * @Version: 1.0
 * *****************************************
 */
class AccountExclusionDialog :
    BaseDialogFragment<DialogAccountExclusionBinding>() {

    companion object {
        fun show(
            manager: FragmentManager,
            isMobilePhoneNumber: Boolean = true
        ): AccountExclusionDialog? {
            if (!(User.get().isVisitor )) {
                //不是游客也不是静默登录状态(及正常账号登录状态)，无需显示
                return null
            }
            val dialog = AccountExclusionDialog()
            dialog.arguments = Bundle().apply {
                putBoolean("isMobilePhoneNumber", isMobilePhoneNumber)
            }
            dialog.show(manager)
            return dialog
        }
    }

    //退出登录
    var logOut: (() -> Unit)? = null

    private val isMobilePhoneNumber = arguments?.getBoolean("isMobilePhoneNumber") ?: true

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.attributes?.let { attr ->
                attr.width = (ScreenUtils.getScreenWidth() * 293f / 375f).toInt()
                attr.gravity = Gravity.CENTER
                dialogWindow.attributes = attr
            }
        }
        dialog?.setCanceledOnTouchOutside(false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext(), R.style.Dialog_Common).apply {
            setCanceledOnTouchOutside(false)
        }
    }


    override fun onPrepareView() {
        super.onPrepareView()
        if (isMobilePhoneNumber){
            binding.tvTitle.text="你的手机号已与另一台设备关联，如果需关联此设备，需解绑已绑定的设备，再登录此设备"
        }else{
            binding.tvTitle.text="你的微信已与另一台设备关联，如果需关联此设备，需解绑已绑定的设备，再登录此设备"
        }

        binding.ivCancel.setOnSingleClickListener {
            dismiss()
        }
        binding.ivUntie.setOnSingleClickListener {
            dismiss()
            logOut?.invoke()
        }
    }
}