package com.kuaigeglobal.epnova.feature.income

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.showConfirmDialog
import com.kuaigeglobal.epnova.databinding.ActivityIncomeBinding
import com.nhsd.basic.BaseViewModelActivity

class IncomeActivity : BaseViewModelActivity<ActivityIncomeBinding, IncomeViewModel>() {

    companion object {
        @JvmStatic
        fun start(context: Context) {
            val starter = Intent(context, IncomeActivity::class.java)
            context.startActivity(starter)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    private fun initView() {

    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.ivBack -> finish()
            R.id.tv_withdraw -> {
                //提现-tooltip
                showConfirmDialog(
                    title = getString(R.string.income_withdraw),
                    tips = getString(R.string.income_withdraw_request_guide),
                    positiveButtonText = getString(R.string.confirm_ok)
                )
            }

            R.id.iv_total_earned_tooltip -> {
                //累计收益-tooltip
                showConfirmDialog(
                    title = getString(R.string.income_total_earned),
                    tips = getString(R.string.income_tooltip_total_earned_desc),
                    positiveButtonText = getString(R.string.confirm_ok)
                )
            }

            R.id.iv_pending_amount_tooltip -> {
                //待结算金额-tooltip
                showConfirmDialog(
                    title = getString(R.string.income_pending_amount),
                    tips = getString(R.string.income_monthly_income_settlement_desc),
                    positiveButtonText = getString(R.string.confirm_ok)
                )
            }
        }
    }
}
