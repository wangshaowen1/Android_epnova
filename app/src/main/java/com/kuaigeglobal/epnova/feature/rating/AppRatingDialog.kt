package com.kuaigeglobal.epnova.feature.rating

import android.content.ActivityNotFoundException
import android.content.Intent
import android.view.Gravity
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.network.apis.request.RatingEnterEnum
import com.kuaigeglobal.epnova.databinding.DialogAppRatingBinding
import com.nhsd.basic.BaseVMDialogFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener

class AppRatingDialog : BaseVMDialogFragment<DialogAppRatingBinding, AppRatingViewModel>() {

    companion object {
        fun show(fragmentManager: FragmentManager, entrance: RatingEnterEnum) {
            // 入口不符合
            if (entrance != RatingEnterEnum.SCRIPT && entrance != RatingEnterEnum.SERIES) return
            // 不符合后台配置条件
            val configuredEntrance = Config.get().appConfig.appRatingPopupEntrance
            if (configuredEntrance != entrance.ordinal && configuredEntrance != RatingEnterEnum.ALL.ordinal) return
            // 用户是否需要弹出好评
            if (!User.get().needAppRatingPopup) return

            if (fragmentManager.isStateSaved || fragmentManager.isDestroyed) return

            if (fragmentManager.findFragmentByTag(AppRatingDialog::class.java.name) != null) return

            AppRatingDialog().apply {
                arguments = bundleOf(Constants.Extra.TYPE to entrance.ordinal)
            }.show(fragmentManager)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.attributes?.let { attr ->
                attr.width = 280.dp
                attr.height = 278.dp
                attr.gravity = Gravity.CENTER
                dialogWindow.attributes = attr
            }
        }
        dialog?.setCanceledOnTouchOutside(false)
        if (dialog?.isShowing == true) mRealVM.reportDisplay()
    }

    override fun onPrepareView() {
        super.onPrepareView()
        updateActionTypeface()

        binding.ratingBar.onRatingChanged = { rating ->
            binding.ratingBar.setRating(mRealVM.updateRating(rating))
            updateActionTypeface()
        }

        binding.ivClose.setOnClickListener { dismissAllowingStateLoss() }
        binding.btnAction.setOnSingleClickListener {
            mRealVM.submitRating()
        }
        mRealVM.submittedRating.observe(this) { submittedRating ->
            val score = submittedRating ?: return@observe
            mRealVM.submittedRating.value = null
            if (score in 4..5) openGooglePlay()
            dismissAllowingStateLoss()
        }
    }

    private fun updateActionTypeface() {
        binding.btnAction.typeface = ResourcesCompat.getFont(
            requireContext(),
            if (mRealVM.actionEnabled.get()) R.font.outfit_medium else R.font.outfit_regular
        )
    }

    private fun openGooglePlay() {
        val context = requireContext()
        val packageName = context.packageName
        try {
            startActivity(
                Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()).apply {
                    setPackage("com.android.vending")
                }
            )
        } catch (_: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://play.google.com/store/apps/details?id=$packageName".toUri()
                )
            )
        }
    }
}
