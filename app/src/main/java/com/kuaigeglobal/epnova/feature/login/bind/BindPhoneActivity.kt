package com.kuaigeglobal.epnova.feature.login.bind

import android.app.Activity
import android.content.Intent
import android.view.View
import com.blankj.utilcode.util.KeyboardUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.databinding.ActivityBindPhoneBinding
import com.nhsd.basic.BaseViewModelActivity

/**
 * 换绑定手机号
 */
class BindPhoneActivity : BaseViewModelActivity<ActivityBindPhoneBinding, BindPhoneViewModel>() {

    companion object {

        /**
         * 绑定手机号
         */
        fun start(activity: Activity?) {
            val av = (activity ?: topActivity) ?: return
            val intent = Intent(activity, BindPhoneActivity::class.java)
            av.startActivity(intent)
        }
    }

    private var showInput = false

    override fun onViewModelCreated() {
        super.onViewModelCreated()
        mRealVM.mobile.observe(this) { mRealVM.checkBtnEnable() }
        mRealVM.code.observe(this) { mRealVM.checkBtnEnable() }
    }

    override fun onResume() {
        super.onResume()
        if (!showInput) {
            showInput = true
            binding.etPhone.requestFocus()
        }
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.btnSend -> {
                mRealVM.checkPhoneBind { KeyboardUtils.showSoftInput(binding.etCode) }
            }

            R.id.btnBind -> {
                mRealVM.bindPhone {
                    finish()
                }
            }

        }
    }

}