package com.kuaigeglobal.epnova.feature.settings.about

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.databinding.ActivityAboutUsBinding
import com.kuaigeglobal.epnova.feature.web.SimpleBrowserActivity
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.getResColor
import kotlin.math.max
import kotlin.math.min

/**
 * *****************************************
 * 项目名称: KidsTales
 * @Author: SZYMN
 * @Introduce:关于我们
 * @Data: 2025/4/18  16:54
 * @Version: 1.0
 * *****************************************
 */
class AboutUsActivity : BaseViewModelActivity<ActivityAboutUsBinding, AboutUsViewModel>() {
    companion object {
        fun start(activity: Activity?) {
            activity ?: return
            val intent = Intent(activity, AboutUsActivity::class.java)
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.scrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val ratio = (max((scrollY - 50).toDouble(), 0.0) / 50f)
            val realRatio = min(1.0, ratio).toFloat()
            val bgEvaluator = android.animation.ArgbEvaluator()
            val bgColor = bgEvaluator.evaluate(
                realRatio,
                Color.TRANSPARENT,
                R.color.white.getResColor()
            ) as Int
            mRealVM.mToolbarVM.mBackground.set(bgColor)
        }
    }


    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.sll_user_agreement -> {
                //用户协议
                SimpleBrowserActivity.launch(
                    this@AboutUsActivity,
                    Config.get().serviceUrl,
                    getString(R.string.tvSYUserService)
                )
            }

            R.id.sll_privacy_policy -> {
                //隐私政策
                SimpleBrowserActivity.launch(
                    this@AboutUsActivity,
                    Config.get().privacyUrl,
                    getString(R.string.tvSYPrivateService)
                )
            }

            R.id.sll_algorithm_formula -> {
                //算法公示
                SimpleBrowserActivity.launch(
                    this@AboutUsActivity,
                    Config.get().algorithmUrl,
                    getString(R.string.app_algorithm_formula)
                )
            }

            R.id.sll_filing_number -> {
                //备案号
                SimpleBrowserActivity.launch(
                    this@AboutUsActivity,
                    Config.get().appConfig.beianNoJumpUrl,
                    getString(R.string.app_filing_number)
                )
            }


            R.id.sll_information_collection -> {
                //个人信息收集清单
                SimpleBrowserActivity.launch(
                    this@AboutUsActivity,
                    Config.get().appConfig.personalInfoCollectionListUrl,
                    getString(R.string.app_information_collection)
                )
            }

            R.id.sll_shared_list -> {
                //第三方信息共享清单
                SimpleBrowserActivity.launch(
                    this@AboutUsActivity,
                    Config.get().appConfig.thirdPartyInfoShareListUrl,
                    getString(R.string.app_shared_list)
                )
            }
        }
    }
}