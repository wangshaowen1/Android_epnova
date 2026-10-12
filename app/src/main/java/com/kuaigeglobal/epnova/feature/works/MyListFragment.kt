package com.kuaigeglobal.epnova.feature.works

import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.blankj.utilcode.util.ActivityUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.adapter.FragmentStateCacheAdapter
import com.kuaigeglobal.epnova.common.ext.removeTheRipples
import com.kuaigeglobal.epnova.common.utils.CommonNavigatorUtils
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.ext.onUserStatusChanged
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.databinding.FragmentMyListBinding
import com.kuaigeglobal.epnova.feature.main.MainActivity
import com.kuaigeglobal.epnova.feature.main.MainViewModel
import com.kuaigeglobal.epnova.feature.mine.child.MineChildFragment
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.bus.observeEvents
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getViewModelOfActivity
import com.nhsd.magicindicator.ViewPagerHelper
import com.nhsd.magicindicator.buildins.commonnavigator.indicators.LinePagerIndicator

/**
 * 主页 - My List
 */
class MyListFragment : BaseViewModelFragment<FragmentMyListBinding, MyListModel>() {


    companion object {
        const val TAG = "WorksFragment"

        @JvmStatic
        fun start() {
            FlowEventBus.postSync(Events.MainTabEvent(Events.MainTabEvent.TAB_MY_CREATIONS))
            ActivityUtils.finishToActivity(MainActivity::class.java, false)
        }
    }

    private val parentVm by lazy {
        getViewModelOfActivity<MainViewModel>()
    }
    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

    override fun onPrepareView() {
        super.onPrepareView()
        initViewpager()
        dataUpdate()
    }

    private fun initViewpager() {
        binding.magicIndicator.navigator = CommonNavigatorUtils.getCommonNavigatorVp2(
            mActivity,
            listOf(
                getString(R.string.my_list_my_creations),
                getString(R.string.profile_history),
                getString(R.string.profile_collection)
            ),
            isAdjustMode = true,
            binding.viewPager2,
            indicatorView = CommonNavigatorUtils.getLinePagerIndicatorGradient(context = mActivity)
                .also { indicator ->
                    indicator?.lineWidth = (24).dp.toFloat()
                    indicator?.mode = LinePagerIndicator.MODE_EXACTLY
                    indicator?.yOffset = (1).dp.toFloat()
                },
            titleHorizontalPaddingDp = 2
        )
        val fragmentList = arrayListOf<Fragment>()
        fragmentList.add(MyCreationsFragment.newInstance())
        fragmentList.add(MineChildFragment.newInstance(type = MineChildFragment.TYPE_HISTORY))
        fragmentList.add(MineChildFragment.newInstance(type = MineChildFragment.TYPE_COLLECTION))

        val viewPagerAdapter = FragmentStateCacheAdapter(
            childFragmentManager,
            lifecycle
        )
        viewPagerAdapter.setFragments(fragmentList)
        binding.viewPager2.adapter = viewPagerAdapter
        ViewPagerHelper.bind(binding.magicIndicator, binding.viewPager2)

        binding.viewPager2.setCurrentItem(0, false)
        binding.viewPager2.removeTheRipples()

        pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val action = when (position) {
                    1 -> FirebaseEvent.Action.HISTORY_CLICK
                    2 -> FirebaseEvent.Action.COLLECTION_CLICK
                    else -> return
                }
                // history_click|collection_click：用户切换对应 Tab 时上报。
                EventManager.onFirebaseTrack(FirebaseEvent.Category.MY_LIST, action)
            }
        }.also { callback ->
            binding.viewPager2.registerOnPageChangeCallback(callback)
        }
    }

    private fun dataUpdate() {
        mRealVM.editModel.observe(this) { isEditMode ->
            binding.viewPager2.isUserInputEnabled = !isEditMode
        }

        observeEvents {
            on<Events.MainTabEvent> { event ->
                when (event.type) {
                    Events.MainTabEvent.TAB_MY_CREATIONS -> {
                        binding.viewPager2.setCurrentItem(0, false)
                    }
                }
            }
            /**
             * 登录状态变化
             */
            onUserStatusChanged {

            }

            /**
             * 用户信息变化
             */
            on<Events.UserDataEvent> {
            }
        }
    }


    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.btnEdit -> {
                mRealVM.editModel.value = true
            }
        }
    }

    override fun onDestroy() {
        pageChangeCallback?.let { binding.viewPager2.unregisterOnPageChangeCallback(it) }
        pageChangeCallback = null
        binding.viewPager2.adapter = null
        super.onDestroy()
    }
}
