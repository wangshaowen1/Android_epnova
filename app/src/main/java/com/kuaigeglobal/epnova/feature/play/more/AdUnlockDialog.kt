package com.kuaigeglobal.epnova.feature.play.more

import android.view.Gravity
import androidx.core.os.bundleOf
import com.blankj.utilcode.util.ScreenUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.databinding.DialogAdUnlockBinding
import com.nhsd.basic.BaseVMDialogFragment
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener
import com.nhsd.basic.ext.showToast

/**
 * c3 激励广告解锁弹窗（AdMob RewardedAd + SSV）
 *
 * 流程：
 *  1. 弹窗展示时自动触发 storyUnlockAdPrepare（获取 nonce）
 *  2. 加载 AdMob RewardedAd，并把 nonce 写入 SSV customData
 *  3. 用户点击按钮 → 展示广告
 *  4. 观看完毕 → 轮询 storyUnlockAdComplete 查询 SSV 处理结果
 *  5. 查询成功 → 回调 onUnlockSuccess
 */
class AdUnlockDialog : BaseVMDialogFragment<DialogAdUnlockBinding, AdUnlockViewModel>() {

    companion object {
        private const val AD_DAILY_MAX = 10

        fun newInstance(workId: String?,workCover:String?, episode: EpisodeInfo?, adRemainCount: Int): AdUnlockDialog {
            return AdUnlockDialog().apply {
                arguments = bundleOf(
                    Constants.Extra.WORK_ID to workId,
                    Constants.Extra.COVER to workCover,
                    Constants.Extra.DATA to episode,
                    Constants.Extra.COUNT to adRemainCount
                )
            }
        }
    }

    /** 广告解锁成功（服务端 SSV 验证通过）回调 */
    var onUnlockSuccess: (() -> Unit)? = null

    /** 用户关闭弹窗但未观看广告时回调 */
    var onDismissWithoutWatch: (() -> Unit)? = null

    private var didWatch = false

    override fun onStart() {
        super.onStart()
        // 居中展示，宽度撑满屏幕
        dialog?.window?.let { w ->
            w.attributes = w.attributes?.apply {
                width = ScreenUtils.getScreenWidth() - 64f.dp
                gravity = Gravity.CENTER
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()

        // ViewModel 通过 mBundle 自动读取 workId 和 episode（无需手动 setup）
        val adRemain = arguments?.getInt(Constants.Extra.COUNT, 0) ?: 0
        binding.adCountText = getString(R.string.episode_unlock_watched_today, AD_DAILY_MAX - adRemain, AD_DAILY_MAX)
        binding.executePendingBindings()

        setupVmCallbacks()
        setupClickListeners()

        // 弹窗打开时自动开始准备广告
        mRealVM.prepareAndLoadAd(requireContext())
    }

    private fun setupVmCallbacks() {
        mRealVM.onUnlockSuccess = {
            didWatch = true
            getString(R.string.episode_unlock_success).showToast()
            onUnlockSuccess?.invoke()
            dismissAllowingStateLoss()
        }

        mRealVM.onUnlockFailed = {
            getString(R.string.episode_unlock_ad_not_ready).showToast()
        }
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnSingleClickListener {
            dismissAllowingStateLoss()
        }

        binding.btnUnlockForFree.setOnSingleClickListener {
            val activity = activity ?: return@setOnSingleClickListener
            when {
                mRealVM.isPolling.get() -> Unit  // 验证中，忽略重复点击
                mRealVM.isAdReady.get() -> mRealVM.showAd(activity)
                mRealVM.isAdLoading.get() -> getString(R.string.episode_unlock_ad_loading).showToast()
                else -> mRealVM.prepareAndLoadAd(requireContext())  // 重新触发加载
            }
        }
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        if (!didWatch) {
            onDismissWithoutWatch?.invoke()
        }
    }
}
