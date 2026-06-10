package com.kuaigeglobal.epnova.feature.splash.newer

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.animation.LinearInterpolator
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.network.apis.entity.ExampleWorkInfo
import com.kuaigeglobal.epnova.databinding.ActivityGuideStep3GeneratingBinding
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.getParcelableValue


/**
 * 新手创作流程 第3步 生成中
 */
class GuideStep3GeneratingActivity : BaseViewModelActivity<ActivityGuideStep3GeneratingBinding, GuideStep3ViewModel>() {

    companion object {
        @JvmStatic
        fun start(context: Context, exampleWorkInfo: ExampleWorkInfo?) {
            val starter = Intent(context, GuideStep3GeneratingActivity::class.java)
                .putExtra(Constants.Extra.DATA, exampleWorkInfo)
            context.startActivity(starter)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRealVM.progressEvent.observe(this) { targetPercent ->
            val trackBg = binding.viewTrackBg
            val progressBar = binding.viewProgressBar

            val totalWidth = trackBg.width
            val targetWidth = (totalWidth * targetPercent).toInt()

            val animator = ValueAnimator.ofInt(progressBar.width, targetWidth)
            animator.duration = 3000L // 动画时长和每步的耗时保持一致，实现匀速推进
            animator.interpolator = LinearInterpolator()
            animator.addUpdateListener { animation ->
                val currentWidth = animation.animatedValue as Int
                val layoutParams = progressBar.layoutParams
                layoutParams.width = currentWidth
                progressBar.layoutParams = layoutParams
            }
            animator.start()
        }

        // 确保布局加载完成后启动流程
        binding.viewTrackBg.post {
            mRealVM.startGenerationProcess()
        }

        mRealVM.navigationEvent.observe(this) {
            // 处理跳转逻辑
            GuideStep4VideoActivity.start(this, intent.getParcelableValue(Constants.Extra.DATA, ExampleWorkInfo::class.java))
            finish()
        }
    }
}
