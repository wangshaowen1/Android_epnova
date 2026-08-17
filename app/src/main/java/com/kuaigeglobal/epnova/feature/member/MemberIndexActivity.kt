package com.kuaigeglobal.epnova.feature.member

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.databinding.ActivityMemberIndexBinding
import com.kuaigeglobal.epnova.feature.member.vm.MemberIndexViewModel
import com.nhsd.basic.BaseViewModelActivity

/** 会员订阅首页
 * @createtime 2026年08月17日 09:50
 * @author TooWhiteT
 */

class MemberIndexActivity : BaseViewModelActivity<ActivityMemberIndexBinding, MemberIndexViewModel>() {

    companion object {
        fun start() {
            val activity = topActivity ?: return
            activity.startActivity(
                Intent(activity, MemberIndexActivity::class.java)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        observeData()
    }

    private fun initView() {

    }

    private fun observeData() {

    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
    }
}