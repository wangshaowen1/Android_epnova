package com.kuaigeglobal.epnova.feature.main

import android.view.Gravity
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import com.blankj.utilcode.util.ScreenUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.getScreenWidth
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.databinding.loadGifUrl
import com.kuaigeglobal.epnova.core.manager.AppEvent
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.network.apis.response.PopupInfo
import com.kuaigeglobal.epnova.databinding.DialogActivityBinding
import com.kuaigeglobal.epnova.router.AppRouter
import com.nhsd.basic.BaseDialogFragment
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.getResDrawable
import com.nhsd.basic.ext.setOnSingleClickListener

/**
 * 活动弹窗
 */
class ActivityDialog : BaseDialogFragment<DialogActivityBinding>() {

    companion object {
        fun show(popup: PopupInfo?) {
            val activity = topActivity as? FragmentActivity ?: return
            ActivityDialog().apply {
                arguments = bundleOf(Constants.Extra.DATA to popup)
            }.show(activity.supportFragmentManager)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.attributes?.let { attr ->
                attr.width = ScreenUtils.getScreenWidth()
                attr.gravity = Gravity.CENTER
                dialogWindow.attributes = attr
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        val popup = arguments?.getParcelableValue(Constants.Extra.DATA, PopupInfo::class.java)
        binding.image.minimumHeight = getScreenWidth()
        loadGifUrl(binding.image, popup?.imageUrl, R.drawable.img_default_cover_1_1.getResDrawable())
        binding.image.setOnSingleClickListener {
            if (popup?.businessType == 1) {
                EventManager.onAppTrack(AppEvent.SF_CLICK_FLOATING_COURSE_ENROLL)
            }
            AppRouter.jump(popup?.jumpUri)
            dismissAllowingStateLoss()
        }
        binding.btnClose.setOnClickListener { dismissAllowingStateLoss() }
    }
}