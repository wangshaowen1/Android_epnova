package com.kuaigeglobal.epnova.feature.creation

import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.blankj.utilcode.util.ActivityUtils
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.adapter.FragmentStateCacheAdapter
import com.kuaigeglobal.epnova.common.utils.KeyboardUtils
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.databinding.FragmentCreationDramasBinding
import com.kuaigeglobal.epnova.feature.creation.childs.CreationInspirationFragment
import com.kuaigeglobal.epnova.feature.creation.childs.CreationNovelFragment
import com.kuaigeglobal.epnova.feature.main.MainActivity
import com.nhsd.basic.BaseViewModelFragment
import com.nhsd.basic.bus.FlowEventBus

/** 创作页
 * @createtime 2026年07月10日 14:34
 * @author TooWhiteT
 */

class CreationFragment : BaseViewModelFragment<FragmentCreationDramasBinding, CreationDramasViewModel>() {
    companion object {
        const val KEY_MODEL = "model"

        //灵感模式
        const val MODEL_INSPIRATION = 1

        //小说改编
        const val MODEL_CUSTOM = 2

        fun start() {
            FlowEventBus.postSync(Events.MainTabEvent(Events.MainTabEvent.TAB_CREATE))
            ActivityUtils.finishToActivity(MainActivity::class.java, false)
        }
    }

    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

    override fun onPrepareView() {
        super.onPrepareView()
        reportInspirationShow()
        initViewPager()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && binding.viewPager.currentItem == 0) {
            reportInspirationShow()
        }
    }

    private fun initViewPager() {
        mRealVM.selectedTab.value = 0
        mRealVM.tabIndicatorPosition.set(0f)

        val adapter = FragmentStateCacheAdapter(childFragmentManager, lifecycle)
        adapter.setFragments(
            listOf<Fragment>(
                CreationInspirationFragment.newInstance(),
                CreationNovelFragment.newInstance()
            )
        )
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = 2
        binding.viewPager.isUserInputEnabled = false

        pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                mRealVM.tabIndicatorPosition.set(position + positionOffset)
            }

            override fun onPageSelected(position: Int) {
                mRealVM.selectedTab.value = position
                KeyboardUtils.hideKeyboard(binding.root)
            }
        }.also { callback ->
            binding.viewPager.registerOnPageChangeCallback(callback)
        }
    }

    override fun <Data> onClick(view: View, data: Data?) {
        super.onClick(view, data)
        when (view.id) {
            R.id.tabInspiration -> {
                val changedToInspiration = binding.viewPager.currentItem != 0
                EventManager.onFirebaseTrack(
                    FirebaseEvent.Category.CREATE,
                    FirebaseEvent.Action.INSPIRATION_TAB_CLICK
                )
                binding.viewPager.currentItem = 0
                if (changedToInspiration) {
                    reportInspirationShow()
                }
            }

            R.id.tabNovel -> {
                EventManager.onFirebaseTrack(
                    FirebaseEvent.Category.CREATE,
                    FirebaseEvent.Action.NOVEL_TAB_CLICK
                )
                binding.viewPager.currentItem = 1
            }
        }
    }

    private fun reportInspirationShow() {
        EventManager.onFirebaseTrack(
            FirebaseEvent.Category.CREATE,
            FirebaseEvent.Action.INSPIRATION_SHOW
        )
    }

    override fun onDestroy() {
        pageChangeCallback?.let { binding.viewPager.unregisterOnPageChangeCallback(it) }
        pageChangeCallback = null
        binding.viewPager.adapter = null
        super.onDestroy()
    }
}
