package com.kuaigeglobal.epnova.feature.member

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.databinding.ActivityMemberIndexBinding
import com.kuaigeglobal.epnova.feature.member.vm.MemberIndexViewModel
import com.kuaigeglobal.epnova.widget.decoration.SpaceDecoration
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.dp

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
        mRealVM.loadMemberPlans()
    }

    private fun initView() {
        binding.rvPrivilege.apply {
            layoutManager = LinearLayoutManager(this@MemberIndexActivity)
            adapter = mRealVM.privilegeAdapter
            isNestedScrollingEnabled = false
            addItemDecoration(SpaceDecoration(12.dp, SpaceDecoration.Direction.MIDDLE))
        }
        binding.rvMemberRecharge.apply {
            layoutManager = LinearLayoutManager(this@MemberIndexActivity)
            adapter = mRealVM.memberRechargeAdapter
            addItemDecoration(SpaceDecoration(16.dp, SpaceDecoration.Direction.MIDDLE))
        }
    }

    private fun observeData() {

    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
    }
}