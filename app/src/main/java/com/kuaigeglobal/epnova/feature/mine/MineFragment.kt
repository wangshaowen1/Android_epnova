package com.kuaigeglobal.epnova.feature.mine

import android.view.View
import androidx.fragment.app.Fragment
import com.blankj.utilcode.util.ClipboardUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.adapter.FragmentStateCacheAdapter
import com.kuaigeglobal.epnova.common.ext.removeTheRipples
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.common.utils.CommonNavigatorUtils
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.onUserStatusChanged
import com.kuaigeglobal.epnova.databinding.FragmentMineBinding
import com.kuaigeglobal.epnova.feature.main.MainViewModel
import com.kuaigeglobal.epnova.feature.mine.child.MineChildFragment
import com.kuaigeglobal.epnova.feature.recharge.MyBalanceActivity
import com.kuaigeglobal.epnova.feature.settings.SettingsActivity
import com.kuaigeglobal.epnova.feature.settings.feedback.FeedbackActivity
import com.kuaigeglobal.epnova.feature.web.SimpleBrowserActivity
import com.kuaigeglobal.epnova.router.AppNavigator
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getViewModelOfActivity
import com.nhsd.basic.ext.showToast
import com.nhsd.magicindicator.ViewPagerHelper
import com.nhsd.magicindicator.buildins.commonnavigator.indicators.LinePagerIndicator

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

            R.id.ll_qa -> {
                SimpleBrowserActivity.launch(activity, Config.get().appConfig.appHelpCenterUrl)
            }

            R.id.btnSettings -> {
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
                //Toast.makeText(view.context,"hello", Toast.LENGTH_SHORT).show()


            }
        }
    }

    private fun initView() {
        binding.magicIndicator.navigator = CommonNavigatorUtils.getCommonNavigatorVp2(
            mActivity,
            listOf(
                getString(R.string.profile_history),
                getString(R.string.profile_collection)
            ),
            isAdjustMode = true,
            binding.viewPager,
            indicatorView = CommonNavigatorUtils.getLinePagerIndicatorGradient(context = mActivity)
                .also { indicator ->
                    indicator?.lineWidth = (28).dp.toFloat()
                    indicator?.mode = LinePagerIndicator.MODE_EXACTLY
                    indicator?.yOffset = (1).dp.toFloat()
                }
        )
        val fragmentList = arrayListOf<Fragment>()
//        fragmentList.add(MyCreationsFragment.newInstance())
        fragmentList.add(MineChildFragment.newInstance(type = MineChildFragment.TYPE_HISTORY))
        fragmentList.add(MineChildFragment.newInstance(type = MineChildFragment.TYPE_COLLECTION))

        val viewPagerAdapter = FragmentStateCacheAdapter(
            childFragmentManager,
            lifecycle
        )
        viewPagerAdapter.setFragments(fragmentList)
        binding.viewPager.adapter = viewPagerAdapter
        ViewPagerHelper.bind(binding.magicIndicator, binding.viewPager)

        binding.viewPager.setCurrentItem(0, false)
        binding.viewPager.removeTheRipples()
    }
}