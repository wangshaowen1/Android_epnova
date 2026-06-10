package com.kuaigeglobal.epnova.feature.splash.newer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.network.apis.entity.ExampleWorkInfo
import com.kuaigeglobal.epnova.databinding.ActivityGuideStep4VideoBinding
import com.kuaigeglobal.epnova.feature.main.MainActivity
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.setOnSingleClickListener


/**
 * 新手创作流程 第4步
 */
class GuideStep4VideoActivity : BaseViewModelActivity<ActivityGuideStep4VideoBinding, GuideStep4ViewModel>() {

    companion object {
        @JvmStatic
        fun start(context: Context, exampleWorkInfo: ExampleWorkInfo?) {
            val starter = Intent(context, GuideStep4VideoActivity::class.java)
                .putExtra(Constants.Extra.DATA, exampleWorkInfo)
            context.startActivity(starter)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.btnNext.setOnSingleClickListener {
            MainActivity.startFromNewer(this)
            finish()
        }
    }

}
