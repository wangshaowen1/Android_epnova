package com.kuaigeglobal.epnova.feature.rating

import android.content.ActivityNotFoundException
import android.content.Intent
import android.view.Gravity
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.databinding.DialogAppRatingBinding
import com.nhsd.basic.BaseVMDialogFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener

class AppRatingDialog : BaseVMDialogFragment<DialogAppRatingBinding, AppRatingViewModel>() {

    companion object {
        fun show() {
            val activity = topActivity as? FragmentActivity ?: return
            AppRatingDialog().show(activity.supportFragmentManager)
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
            when (mRealVM.rating.get().toInt()) {
                in 1..3 -> dismissAllowingStateLoss()
                in 4..5 -> {
                    openGooglePlay()
                    dismissAllowingStateLoss()
                }
            }
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
