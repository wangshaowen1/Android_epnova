package com.kuaigeglobal.epnova.feature.creator.set

import android.text.Editable
import android.text.TextWatcher
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.applySmartFocusManager
import com.kuaigeglobal.epnova.core.ext.sensitiveWordCheck
import com.kuaigeglobal.epnova.core.network.apis.entity.VideoConfigData
import com.kuaigeglobal.epnova.databinding.DialogVideoSetBinding
import com.kuaigeglobal.epnova.feature.creator.set.allstyle.CreationStyleChooseDialog
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.launch


class VideoSetDialog : BaseBottomPushVMDialogFragment<DialogVideoSetBinding, VideoSetViewModel>() {

    companion object {
        fun newInstance(workTitle: String?, list: VideoConfigData?, charCount: Int? = null): VideoSetDialog {
            val fragment = VideoSetDialog()
            fragment.arguments = bundleOf(
                Constants.Extra.TITLE to workTitle,
                Constants.Extra.DATA to list,
                Constants.Extra.COUNT to charCount
            )
            return fragment
        }
    }

    /**
     * 按顺序依次是：集数范围，自定义集数，视频比例，视频风格,作品名称
     */
    var onSuccess: ((String?, String?, String?, String?, String?) -> Unit)? = null

    override fun onPrepareView() {
        super.onPrepareView()
        binding.root.applySmartFocusManager()
        binding.nameInputer.setText(mRealVM.workTitle)
        binding.btnConfirm.alpha = if (mRealVM.workTitle.isNullOrEmpty()) 0.6f else 1f
        binding.nameInputer.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                binding.btnConfirm.alpha = if (binding.nameInputer.text.isNullOrEmpty()) 0.6f else 1f
            }
        })
        binding.btnConfirm.setOnClickListener {
            if (binding.nameInputer.text.isNullOrEmpty()) {
                showToast(R.string.please_enter_work_name)
                return@setOnClickListener
            }
            if (mRealVM.checkedEpisodeSet == null) {
                getString(R.string.please_set_total_episodes).showToast()
                return@setOnClickListener
            }
            if (mRealVM.checkedEpisodeSet?.data?.get()?.isCustomInput() == true) {
                if (mRealVM.checkedEpisodeSet?.input?.get().isNullOrEmpty()) {
                    getString(R.string.please_enter_custom_episodes).showToast()
                    return@setOnClickListener
                }
            }
            if (mRealVM.checkedRatio == null) {
                getString(R.string.please_select_video_aspect_ratio).showToast()
                return@setOnClickListener
            }
            if (mRealVM.checkedStyle == null) {
                getString(R.string.please_select_video_style).showToast()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                sensitiveWordCheck(binding.nameInputer.text.toString(), {
                    onSuccess?.invoke(
                        mRealVM.checkedEpisodeSet?.data?.get()?.configCode,
                        mRealVM.checkedEpisodeSet?.input?.get(),
                        mRealVM.checkedRatio?.data?.get()?.configValue,
                        mRealVM.checkedStyle?.data?.get()?.styleCode,
                        binding.nameInputer.text
                    )
                    dismissAllowingStateLoss()
                }, {
                    it?.let {
                        binding.nameInputer.setText(it)
                    }
                })
            }
        }

        binding.btnAllStyle.setOnSingleClickListener {
            val newInstance =
                CreationStyleChooseDialog.newInstance(mRealVM.checkedStyle?.data?.get()?.styleCode, mRealVM.configsData?.styleItemVOList)
            newInstance.onSuccess = {
                it?.let {
                    mRealVM.styleVms.find { vm -> vm.data.get()?.styleCode == it.styleCode }?.let { vm ->
                        mRealVM.checkedStyle?.checked?.set(false)
                        vm.checked.set(true)
                        mRealVM.checkedStyle = vm
                        bindingNullable?.recyclerView?.scrollToPosition(mRealVM.styleVms.indexOf(vm))
                    }
                }
            }
            newInstance.show(childFragmentManager)
        }
    }
}