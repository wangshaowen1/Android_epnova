package com.kuaigeglobal.epnova.feature.settings.report

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.FragmentManager
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.databinding.DialogReportBinding
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:举报
 * @Data: 2026/2/2/周一  15:24
 * @Version: 1.0
 * *****************************************
 */
class ReportDialog : BaseBottomPushVMDialogFragment<DialogReportBinding, ReportDialogViewModel>() {
    companion object {
        // 举报内容字数最低要求
        private const val REPORT_CONTENT_MIN_SIZE = 50
        //被举报的作品用户 ID
        const val USER_ID = "userId"

        //被举报的作品 ID
        const val HOME_ID = "homeId"

        fun newInstance(manager: FragmentManager, userId: String?, homeId: String?): ReportDialog {
            val dialog = ReportDialog()
            dialog.arguments = Bundle().apply {
                putString(USER_ID, userId)
                putString(HOME_ID, homeId)
            }
            dialog.show(manager)
            return dialog
        }
    }


    override fun onPrepareView() {
        super.onPrepareView()
        initView()
        //数据更新
        dataUpdate()
    }

    private fun initView() {
        binding.xet.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }

            override fun afterTextChanged(s: Editable?) {
                if ((s?.length ?: 0) < REPORT_CONTENT_MIN_SIZE) {
                    binding.commit.alpha = 0.5f
                } else {
                    binding.commit.alpha = 1f
                }
            }
        })

        binding.commit.setOnSingleClickListener {
            //提交
            val content = binding.xet.text.toString()
            if (content.length < REPORT_CONTENT_MIN_SIZE) {
                getString(R.string.toast_inspiration_input,REPORT_CONTENT_MIN_SIZE).showToast()
                return@setOnSingleClickListener
            }
            //联系人
            val phoneNumber = binding.contactPerson.text.toString()
            mRealVM.submit(content = content, phoneNumber = phoneNumber)
        }
    }

    private fun dataUpdate() {
        mRealVM.isSubmit.observe(this) {
            if (!it) return@observe
            getString(R.string.report_success).showToast()
            dismissAllowingStateLoss()
        }
    }
}