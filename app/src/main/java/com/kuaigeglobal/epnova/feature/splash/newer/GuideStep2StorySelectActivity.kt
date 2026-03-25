package com.kuaigeglobal.epnova.feature.splash.newer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.viewpager2.widget.ViewPager2
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.adapter.QuickAdapter
import com.kuaigeglobal.epnova.common.ext.setGalleryEffect
import com.kuaigeglobal.epnova.core.manager.AppEvent
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.databinding.ActivityGuideStep2StorySelectBinding
import com.kuaigeglobal.epnova.feature.main.MainActivity
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.setOnSingleClickListener


/**
 * 新手创作流程 第2步 选故事
 */
class GuideStep2StorySelectActivity : BaseViewModelActivity<ActivityGuideStep2StorySelectBinding, GuideStep2ViewModel>() {
    companion object {
        @JvmStatic
        fun start(context: Context) {
            val starter = Intent(context, GuideStep2StorySelectActivity::class.java)
            context.startActivity(starter)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.banner.setGalleryEffect(20, 20, 0.9f, 0.5f)
        EventManager.onAppTrack(AppEvent.SF_VIEW_BEGINNER_CREATION_STORY_PAGE)
        mRealVM.examples.observe(this) {
            if (binding.banner.adapter == null && !it.isNullOrEmpty()) {
                binding.banner.adapter = QuickAdapter(R.layout.item_guide_step2_vp, it.map { info ->
                    ItemGuideStep2Vm(info)
                })
                binding.ivIndicator.attachToViewPager2(binding.banner)
                startAutoScroll()
            }
        }

        binding.banner.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                when (state) {
                    // 当用户手指按住并开始拖拽时，停止计时
                    ViewPager2.SCROLL_STATE_DRAGGING -> stopAutoScroll()

                    // 当滑动停止（手指抬起且动画结束）时，重新开始 3s 计时
                    ViewPager2.SCROLL_STATE_IDLE -> startAutoScroll()
                }
            }
        })

        binding.btnConfirm.setOnSingleClickListener {
            EventManager.onAppTrack(AppEvent.SF_CLICK_BEGINNER_CREATION_STORY_PAGE_BUTTON)
            val exampleWorkInfo = mRealVM.examples.value?.getOrNull(binding.banner.currentItem)
            GuideStep3GeneratingActivity.start(this, exampleWorkInfo)
            finish()
        }

        binding.btnIgnore.setOnSingleClickListener {
            EventManager.onAppTrack(AppEvent.SF_CLICK_BEGINNER_CREATION_STORY_PAGE_SKIP)
            MainActivity.start(this, null)
            finish()
        }

    }

    private fun startAutoScroll() {
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
        autoScrollHandler.postDelayed(autoScrollRunnable, 3000)
    }

    private fun stopAutoScroll() {
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
    }

    // 4. 生命周期管理：避免内存泄漏或后台继续滚动
    override fun onResume() {
        super.onResume()
        if (binding.banner.adapter != null) {
            startAutoScroll()
        }
    }

    override fun onPause() {
        super.onPause()
        stopAutoScroll()
    }

    private val autoScrollHandler = Handler(Looper.getMainLooper())
    private val autoScrollRunnable = object : Runnable {
        override fun run() {
            val adapter = binding.banner.adapter ?: return
            val itemCount = adapter.itemCount
            if (itemCount > 1) {
                // 计算下一页：如果是最后一页则回到第 0 页
                val nextItem = (binding.banner.currentItem + 1) % itemCount
                binding.banner.setCurrentItem(nextItem, true)
            }
            // 每 3000 毫秒执行一次
            autoScrollHandler.postDelayed(this, 3000)
        }
    }
}
