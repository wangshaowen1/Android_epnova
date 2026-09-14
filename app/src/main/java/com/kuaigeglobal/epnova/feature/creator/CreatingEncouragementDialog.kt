package com.kuaigeglobal.epnova.feature.creator

import android.view.Gravity
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.core.network.apis.entity.PopupClaimInfo
import com.kuaigeglobal.epnova.databinding.DialogCreatingEncouragementBinding
import com.kuaigeglobal.epnova.router.AppRouter
import com.nhsd.basic.BaseVMDialogFragment
import com.nhsd.basic.ext.dp

/** 创作鼓励弹窗
 * @createtime 2026年09月08日 10:59
 * @author TooWhiteT
 */

class CreatingEncouragementDialog : BaseVMDialogFragment<DialogCreatingEncouragementBinding, CreatingEncouragementViewModel>() {

    companion object {
        private const val DESIGN_HEIGHT_DP = 408
        private const val DESIGN_WIDTH_DP = 295

        fun newInstance(data: PopupClaimInfo): CreatingEncouragementDialog {
            return CreatingEncouragementDialog().apply {
                arguments = bundleOf(Constants.Extra.DATA to data)
            }
        }

        fun show(fragmentManager: FragmentManager, data: PopupClaimInfo): Boolean {
            if (fragmentManager.isStateSaved || fragmentManager.isDestroyed) return false
            if (fragmentManager.findFragmentByTag(CreatingEncouragementDialog::class.java.name) != null) {
                return true
            }
            newInstance(data).show(fragmentManager)
            return true
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.attributes = window.attributes.apply {
                width = DESIGN_WIDTH_DP.dp
                height = DESIGN_HEIGHT_DP.dp
                gravity = Gravity.CENTER
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        EventManager.onFirebaseTrack(
            FirebaseEvent.Category.ENCOURAGEMENT_POPUP,
            FirebaseEvent.Action.POPUP_SHOW,
            mRealVM.data?.title,
            mRealVM.data?.workId
        )
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.ivClose -> {
                dismissAllowingStateLoss()
            }
            R.id.btnNext -> {
                EventManager.onFirebaseTrack(FirebaseEvent.Category.ENCOURAGEMENT_POPUP, FirebaseEvent.Action.WATCH_NOW_CLICK)
                mRealVM.reportDisplay {
                    when (mRealVM.data?.jumpType) {
                        1 -> { // 站内跳转
                            AppRouter.jump(mRealVM.data?.jumpUri)
                        }
                        else -> {
                            topActivity?.let { context ->
                                AppRouter.jump(context, mRealVM.data?.jumpUri.orEmpty(), true)
                            }
                        }
                    }
                    dismissAllowingStateLoss()
                }
            }
        }
    }
}