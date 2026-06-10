package com.kuaigeglobal.epnova.feature.splash.newer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.kuaigeglobal.epnova.databinding.ActivityGuideStep1AdBinding
import com.kuaigeglobal.epnova.feature.splash.SplashViewModel
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.setOnSingleClickListener


/**
 * 新手创作流程 第一步
 */
class GuideStep1AdActivity : BaseViewModelActivity<ActivityGuideStep1AdBinding, SplashViewModel>() {
    companion object {
        @JvmStatic
        fun start(context: Context) {
            val starter = Intent(context, GuideStep1AdActivity::class.java)
            context.startActivity(starter)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.btnNext.setOnSingleClickListener {
            GuideStep2StorySelectActivity.start(this)
            finish()
        }
    }
}
