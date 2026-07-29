package com.kuaigeglobal.epnova.feature.mine

import android.view.View
import com.blankj.utilcode.util.ClipboardUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.showConfirmDialog
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.onUserStatusChanged
import com.kuaigeglobal.epnova.databinding.FragmentMineBinding
import com.kuaigeglobal.epnova.feature.income.IncomeActivity
import com.kuaigeglobal.epnova.feature.main.MainViewModel
import com.kuaigeglobal.epnova.feature.recharge.MyBalanceActivity
import com.kuaigeglobal.epnova.feature.rewards.RewardsActivity
import com.kuaigeglobal.epnova.feature.settings.SettingsActivity
import com.kuaigeglobal.epnova.feature.settings.feedback.FeedbackActivity
import com.kuaigeglobal.epnova.feature.settings.lang.LanguagesActivity
import com.kuaigeglobal.epnova.feature.web.SimpleBrowserActivity
import com.kuaigeglobal.epnova.router.AppNavigator
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.getViewModelOfActivity
import com.nhsd.basic.ext.showToast
import kotlin.Boolean

/**
 * 主页 - Profile
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
        initView()
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
        observeEvents {
//            on<Events.MainTabEvent> { event ->
//                when (event.type) {
//                    Events.MainTabEvent.TAB_MY_CREATIONS -> {
//                        binding.viewPager.setCurrentItem(0, false)
//                    }
//                }
//            }
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
            R.id.iv_arrow,
            R.id.tv_name -> {
                AppNavigator.checkRealLogin(false)
            }

            R.id.tv_id, R.id.iv_copy -> {
                //复制
                User.get().userUid?.let {
                    ClipboardUtils.copyText(it)
                    getString(R.string.copy_success).showToast()
                }
            }

            R.id.ll_qa -> {
                SimpleBrowserActivity.launch(activity, Config.get().appConfig.appHelpCenterUrl)
            }

            R.id.ll_store, R.id.tv_recharge -> {
                if (AppNavigator.checkLoginIntercepted(false)) {
                    return
                }
                //商城
                MyBalanceActivity.start()
            }

            R.id.iv_coins_tooltip -> {
                //金币-tooltip
                activity?.showConfirmDialog(
                    title = null,
                    tips = getString(R.string.coins_popup_instructions),
                    positiveButtonText = getString(R.string.btn_ok)
                )
            }

            R.id.iv_credits_tooltip -> {
                //积分 / 点数-tooltip
                activity?.showConfirmDialog(
                    title = null,
                    tips = getString(R.string.credits_popup_instructions),
                    positiveButtonText = getString(R.string.btn_ok)
                )
            }

            R.id.ll_income -> {
                //收益
                IncomeActivity.start(mActivity)
            }

            R.id.ll_rewards -> {
                if (AppNavigator.checkLoginIntercepted(false)) {
                    return
                }
                //福利中心
                RewardsActivity.start(mActivity)
            }

            R.id.ll_languages -> {
                //语言
                LanguagesActivity.start(mActivity)
            }

            R.id.ll_suggestions_feedback -> {
                if (AppNavigator.checkLoginIntercepted(false)) {
                    return
                }
                //建议与反馈
                FeedbackActivity.start(topActivity)
            }

            R.id.ll_settings -> {
                //设置
                if (AppNavigator.checkLoginIntercepted(false)) {
                    return
                }
                SettingsActivity.start(requireActivity())
            }
        }
    }

    private fun initView() {

    }
}
