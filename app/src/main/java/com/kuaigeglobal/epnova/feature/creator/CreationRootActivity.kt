package com.kuaigeglobal.epnova.feature.creator

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.applySmartFocusManager
import com.kuaigeglobal.epnova.common.utils.KeyboardUtils
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.databinding.ActivityCreationRootBinding
import com.nhsd.basic.BaseViewModelActivity

/**
 * @Introduce:创作根界面
 */
class CreationRootActivity :
    BaseViewModelActivity<ActivityCreationRootBinding, CreationRootViewModel>() {

    companion object {
        @JvmStatic
        fun start(context: Context?, index: Int? = null) {
            if (context == null) return
            val starter = Intent(context, CreationRootActivity::class.java)
            starter.putExtra(Constants.Extra.INDEX, index)
            context.startActivity(starter)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.root.applySmartFocusManager()
        binding.viewPager.adapter = object : FragmentPagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getItem(position: Int): Fragment {
                return when (position) {
                    0 -> CreationFragment.newInstance()
                    else -> CreationFragment.newInstance(CreationFragment.MODEL_CUSTOM)
                }
            }

            override fun getCount(): Int {
                return 2
            }
        }
        binding.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                mRealVM.tabIndicatorPosition.set(position + positionOffset)
            }

            override fun onPageSelected(position: Int) {
                mRealVM.selectedTab.value = position
                KeyboardUtils.hideKeyboard(binding.root)
            }

            override fun onPageScrollStateChanged(state: Int) {
            }
        })
        intent.getIntExtra(Constants.Extra.INDEX, 0).let {
            binding.viewPager.currentItem = it
        }
        binding.rbLeft.setOnClickListener {
            if (binding.viewPager.currentItem != 0) {
                EventManager.onFirebaseTrack(FirebaseEvent.STORY_INSPIRATION_TAB_CLICK)
            }
            binding.viewPager.currentItem = 0
        }

        binding.rbRight.setOnClickListener {
            if (binding.viewPager.currentItem != 1) {
                EventManager.onFirebaseTrack(FirebaseEvent.NOVEL_ADAPTATION_TAB_CLICK)
            }
            binding.viewPager.currentItem = 1
        }
    }

    override fun finish() {
        super.finish()
    }
}
