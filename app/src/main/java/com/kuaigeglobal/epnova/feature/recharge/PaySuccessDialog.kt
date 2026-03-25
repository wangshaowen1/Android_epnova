package com.kuaigeglobal.epnova.feature.recharge

import android.view.Gravity
import androidx.fragment.app.FragmentActivity
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.ScreenUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.databinding.loadGifUrl
import com.kuaigeglobal.epnova.databinding.DialogPaySuccessBinding
import com.kuaigeglobal.epnova.router.AppRouter
import com.nhsd.basic.BaseDialogFragment
import com.nhsd.basic.ext.getResDrawable
import com.nhsd.basic.ext.setOnSingleClickListener
import kotlin.math.roundToInt

/**
 * 支付成功或其它引导场景弹窗
 */
class PaySuccessDialog : BaseDialogFragment<DialogPaySuccessBinding>() {

    companion object {
        private const val KEY_HAS_ADDED = "has_added_service_"
        private const val KEY_SHOW_COUNT = "show_count_"

        const val SCENE_PAY_SUCCESS = "pay_success"
        const val SCENE_OTHER = "SCENE_OTHER"

        /**
         * @param scene 场景标识，确保不同场景计数互不干扰
         */
        fun checkAndShow(scene: String = SCENE_PAY_SUCCESS) {
            val activity = topActivity as? FragmentActivity ?: return
            val uid = User.get().userId

            // 组合 Key：基础 Key + 场景名 + 用户 ID
            val addedKey = "${KEY_HAS_ADDED}${scene}_${uid}"
            val countKey = "${KEY_SHOW_COUNT}${scene}_${uid}"

            // 1. 判断该场景下是否已经点击过“立即添加”
            if (SPUtils.getInstance().getBoolean(addedKey, false)) return

            // 2. 判断该场景弹出次数是否超过 3 次
            val count = SPUtils.getInstance().getInt(countKey, 0)
            if (count >= 3) return

            // 符合条件，显示弹窗并累加该场景次数
            SPUtils.getInstance().put(countKey, count + 1)

            // 创建实例并传递场景参数（如果弹窗 UI 需要根据场景变化）
            PaySuccessDialog().apply {
                arguments = android.os.Bundle().apply { putString(Constants.Extra.MODE, scene) }
            }.show(activity.supportFragmentManager)
        }
    }

    // 获取当前弹窗所属场景
    private val currentScene: String
        get() = arguments?.getString(Constants.Extra.MODE) ?: SCENE_PAY_SUCCESS

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { dialogWindow ->
            dialogWindow.attributes?.let { attr ->
                attr.width = (ScreenUtils.getScreenWidth() * 0.78).roundToInt()
                attr.gravity = Gravity.CENTER
                dialogWindow.attributes = attr
            }
        }
    }

    override fun onPrepareView() {
        super.onPrepareView()
        val appConfig = Config.get().appConfig
        val uid = User.get().userId

        val imageUrl = when (currentScene) {
            SCENE_PAY_SUCCESS -> appConfig.appPopupPointGuideImageUrl
            else -> appConfig.appPopupCourseGuideImageUrl
        }

        loadGifUrl(binding.image, imageUrl, R.drawable.img_default_cover_3_4.getResDrawable())

        // 暂不添加/关闭
        binding.btnCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }

        val scheme = when (currentScene) {
            SCENE_PAY_SUCCESS -> appConfig.appJumpMiniProgramCustomerServiceUrl
            else -> appConfig.appJumpMiniProgramAdvisorUrl
        }

        // 立即添加
        binding.btnAdd.setOnSingleClickListener {
            // 标记该场景为永久不再弹出
            val addedKey = "${KEY_HAS_ADDED}${currentScene}_${uid}"
            SPUtils.getInstance().put(addedKey, true)

            // 执行跳转逻辑
            AppRouter.jump(scheme)
            dismissAllowingStateLoss()
        }
    }
}