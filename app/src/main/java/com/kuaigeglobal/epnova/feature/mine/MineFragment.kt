package com.kuaigeglobal.epnova.feature.mine

import android.view.View
import androidx.core.view.isVisible
import com.blankj.utilcode.util.ClipboardUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.databinding.loadGifUrl
import com.kuaigeglobal.epnova.core.ext.onUserStatusChanged
import com.kuaigeglobal.epnova.core.network.apis.response.ActivityConfigData
import com.kuaigeglobal.epnova.core.network.apis.response.FloatWindowInfo
import com.kuaigeglobal.epnova.core.network.apis.response.PopupInfo
import com.kuaigeglobal.epnova.databinding.FragmentMineBinding
import com.kuaigeglobal.epnova.feature.main.ActivityDialog
import com.kuaigeglobal.epnova.feature.main.MainViewModel
import com.kuaigeglobal.epnova.feature.recharge.MyBalanceActivity
import com.kuaigeglobal.epnova.feature.settings.SettingsActivity
import com.kuaigeglobal.epnova.feature.settings.feedback.FeedbackActivity
import com.kuaigeglobal.epnova.feature.web.SimpleBrowserActivity
import com.kuaigeglobal.epnova.router.AppNavigator
import com.kuaigeglobal.epnova.router.AppRouter
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.getResDrawable
import com.nhsd.basic.ext.getViewModelOfActivity
import com.nhsd.basic.ext.showToast

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:我的
 * @Data: 2025/4/18  14:08
 * @Version: 1.0
 * *****************************************
 */
class MineFragment : BaseViewModelFragment<FragmentMineBinding, MineViewModel>() {

    private var userId: String? = null
    private val parentVm by lazy {
        getViewModelOfActivity<MainViewModel>()
    }

    override fun onPrepareView() {
        super.onPrepareView()
        //数据更新
        dataUpdate()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) return
        //更新必要信息
        mRealVM.updateInformation()
    }

    override fun onResume() {
        super.onResume()
        //更新必要信息
        mRealVM.updateInformation()
    }

    private fun dataUpdate() {
        parentVm?.activityConfigCache?.observe(this) {
            handlePopup(it.getPopupInfo(ActivityConfigData.TAB_MINE))
            handleFloatWindow(it.getFloatWindowInfo(ActivityConfigData.TAB_MINE))
        }

        observeEvents {
            /**
             * 登录状态变化
             */
            onUserStatusChanged {
                mRealVM.refreshUserInfo()
                if (userId == User.get().id) return@onUserStatusChanged
                userId = User.get().id
            }

            /**
             * 用户信息变化
             */
            on<Events.UserDataEvent> {
                mRealVM.refreshUserInfo()
            }
        }
    }


    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.siv_avatar,
            R.id.tv_login,
            R.id.tv_name -> {
                //个人资料
                AppNavigator.checkLoginIntercepted(false)
            }

            R.id.ll_qa -> {
                SimpleBrowserActivity.launch(activity, Config.get().appConfig.appHelpCenterUrl)
            }

            R.id.ll_settings -> {
                //设置
                if (AppNavigator.checkLoginIntercepted(false)) {
                    return
                }
                SettingsActivity.start(requireActivity())
            }

            R.id.tv_id, R.id.iv_copy -> {
                //复制
                User.get().userUid?.let {
                    ClipboardUtils.copyText(it)
                    getString(R.string.copy_success).showToast()
                }
            }

            R.id.suggestions_feedback -> {
                if (AppNavigator.checkLoginIntercepted(false)) {
                    return
                }
                //建议与反馈
                FeedbackActivity.start(topActivity)
            }

            R.id.btnMyBalance -> {
                if (AppNavigator.checkLoginIntercepted(false)) {
                    return
                }
                MyBalanceActivity.start()
            }
        }
    }

    private fun handlePopup(popup: PopupInfo?) {
        popup?.let {
            if (popup.isShow()) {
                ActivityDialog.show(popup)
                Config.get().activityPopupShowMark(popup.id)
            }
        }
    }

    private fun handleFloatWindow(float: FloatWindowInfo?) {
        if (float != null && float.isShow()) {
            binding.floatingBall.isVisible = true
            loadGifUrl(binding.ivFloating, float.imageUrl, R.drawable.img_default_cover_1_1.getResDrawable())
            Config.get().activityFloatShowMark(float.id)
            binding.floatingBall.setOnMfClickListener {
                AppRouter.jump(float.jumpUrl)
            }
        } else {
            binding.floatingBall.visibility = View.GONE
        }
    }
}