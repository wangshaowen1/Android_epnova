package com.kuaigeglobal.epnova.feature.member

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.databinding.ActivityMemberIndexBinding
import com.kuaigeglobal.epnova.feature.member.vm.MemberIndexViewModel
import com.kuaigeglobal.epnova.feature.recharge.MyBalanceViewModel
import com.kuaigeglobal.epnova.feature.web.SimpleBrowserActivity
import com.kuaigeglobal.epnova.widget.decoration.SpaceDecoration
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.getResString

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

        setupServiceAndPolicy()
    }

    private fun observeData() {

    }

    private fun setupServiceAndPolicy() {
        val rawText = R.string.member_service_and_policy.getResString()
        val spannableString = SpannableString(rawText)

        applyClickableSpan(spannableString, getString(R.string.app_user_agreement)) {
            SimpleBrowserActivity.launch(this, Config.get().serviceUrl, getString(R.string.app_user_agreement))
        }
        applyClickableSpan(spannableString, getString(R.string.app_privacy_policy)) {
            SimpleBrowserActivity.launch(this, Config.get().privacyUrl, getString(R.string.app_privacy_policy))
        }

        binding.tvMemberService.movementMethod = LinkMovementMethod.getInstance()
        binding.tvMemberService.highlightColor = Color.TRANSPARENT
        binding.tvMemberService.text = spannableString
    }

    private fun applyClickableSpan(spannable: SpannableString, target: String, onClick: () -> Unit) {
        val startIndex = spannable.indexOf(target)
        if (startIndex == -1) return
        spannable.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) = onClick()
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = ContextCompat.getColor(this@MemberIndexActivity, R.color.text_color_1_dark)
                    ds.isFakeBoldText = true
                }
            },
            startIndex,
            startIndex + target.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
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
