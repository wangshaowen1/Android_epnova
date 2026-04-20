package com.kuaigeglobal.epnova.feature.play.more

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.core.os.bundleOf
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.databinding.DialogEpisodeUnlockBinding
import com.kuaigeglobal.epnova.feature.recharge.PowerRechargeDialog
import com.nhsd.basic.BaseBottomPushVMDialogFragment
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast

class EpisodeUnlockDialog :
    BaseBottomPushVMDialogFragment<DialogEpisodeUnlockBinding, EpisodeUnlockViewModel>() {

    companion object {
        fun newInstance(workId: String?, episode: EpisodeInfo?): EpisodeUnlockDialog {
            val fragment = EpisodeUnlockDialog()
            fragment.arguments = bundleOf(
                Constants.Extra.WORK_ID to workId,
                Constants.Extra.DATA to episode
            )
            return fragment
        }
    }

    /** 解锁成功后回调（金币解锁和广告解锁共用） */
    var onUnlockSuccess: ((EpisodeInfo) -> Unit)? = null

    /**
     * 余额不足时触发：已关闭弹窗并展示充值弹窗，Activity 需在 PowerRechargeDialog 关闭后
     * 根据 adRemainCount 决定下一步。
     */
    var onInsufficientCoins: ((adRemainCount: Int) -> Unit)? = null

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setDimAmount(0f)
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()

        // 广告解锁按钮：直接通过 VM 调 AdMob，无需跳转 AdUnlockDialog
        binding.llAdUnlock.setOnSingleClickListener {
            if (mRealVM.isAdLoading.get()) {
                getString(R.string.episode_unlock_ad_loading).showToast()
            } else {
                mRealVM.startAdUnlock(requireContext(), requireActivity())
            }
        }

        // 金币解锁 / 充值 按钮
        binding.btnUnlockNow.setOnSingleClickListener {
            if (mRealVM.coinsInsufficient.get()) {
                val adRemain = mRealVM.getAdRemainCount()
                dismissAllowingStateLoss()
                PowerRechargeDialog.show()
                onInsufficientCoins?.invoke(adRemain)
            } else {
                mRealVM.unlockWithCoins()
            }
        }

        // 自动解锁开关
        binding.llAutoUnlock.setOnSingleClickListener {
            mRealVM.toggleAutoUnlock()
        }

        mRealVM.onUnlockSuccess = { episode ->
            getString(R.string.episode_unlock_success).showToast()
            onUnlockSuccess?.invoke(episode)
            dismissAllowingStateLoss()
        }
    }
}
