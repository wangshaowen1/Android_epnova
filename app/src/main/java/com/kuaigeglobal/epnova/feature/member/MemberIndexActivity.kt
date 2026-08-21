package com.kuaigeglobal.epnova.feature.member

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
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
        fun start(
            entryScene: String = FirebaseEvent.Value.TRIGGER_MEMBERSHIP_ENTRY,
        ) {
            val activity = topActivity ?: return
            activity.startActivity(
                Intent(activity, MemberIndexActivity::class.java).apply {
                    putExtra(Constants.Extra.FROM_PAGE, entryScene)
                }
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
        when (view?.id) {
            R.id.btn_member_subscribe -> {
                // Activity 只转发 UI 事件；套餐资格、prepare、Billing 和服务端确认均由 ViewModel
                // 按单笔购买上下文编排，避免页面层持有支付状态。
                mRealVM.reportMemberPlanClick()
                mRealVM.startMemberSubscription()
            }
        }
    }
}
