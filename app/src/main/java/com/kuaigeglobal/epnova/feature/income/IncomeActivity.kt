package com.kuaigeglobal.epnova.feature.income

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.startRefreshWithAdapter
import com.kuaigeglobal.epnova.common.ext.showConfirmDialog
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.databinding.ActivityIncomeBinding
import com.kuaigeglobal.epnova.feature.income.IncomeViewModel.Companion.BALANCE_UNIT
import com.kuaigeglobal.epnova.widget.BottomYearMonthPickerDialog
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.ext.getResString
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class IncomeActivity : BaseViewModelActivity<ActivityIncomeBinding, IncomeViewModel>() {

    companion object {
        @JvmStatic
        fun start(context: Context) {
            val starter = Intent(context, IncomeActivity::class.java)
            context.startActivity(starter)
        }
    }

    //收益明细分页列表
    private val incomeDetailsAdapter by lazy { IncomeDetailsAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        dataUpdate()
    }

    private fun initView() {
        binding.rvIncomeDetails.apply {
            itemAnimator = null
            isFocusable = false
            adapter = incomeDetailsAdapter.withLoadStateFooter(
                MyMultiStatePagingFooterAdapter(
                    hostAdapter = incomeDetailsAdapter,
                    noMoreThreshold = 12
                ) {
                    incomeDetailsAdapter.retry()
                }
            )
        }
        binding.srl.startRefreshWithAdapter(
            pagingDataAdapter = incomeDetailsAdapter,
            loadStateHolder = binding.lsv
        )
    }

    private fun dataUpdate() {
        lifecycleScope.launch {
            mRealVM.getIncomeDetailsPage().collectLatest {
                incomeDetailsAdapter.submitData(it)
            }
        }
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            //年月选择
            R.id.tv_monthfilter -> {
                BottomYearMonthPickerDialog.show(
                    manager = supportFragmentManager,
                    minYearMonth = mRealVM.getMinSelectableYearMonth(),
                    maxYearMonth = mRealVM.getMaxSelectableYearMonth(),
                    selectedYearMonth = mRealVM.getSelectedYearMonth()
                ) {
                    mRealVM.confirmIncomeDetailsYearMonth(it)
                }
            }

            R.id.tv_withdraw -> {
                val limit = Config.get().appConfig.incomeWithdrawMinLimit ?: 0L
                val availableBalance = mRealVM.availableBalance.get()
                val flag = availableBalance >= limit
                //提现-tooltip
                showConfirmDialog(
                    title = if (flag) getString(R.string.income_withdraw) else getString(R.string.income_withdraw_minium),
                    tips = if (flag) getString(R.string.income_withdraw_request_guide) else R.string.income_withdraw_minium_tips.getResString(limit / BALANCE_UNIT),
                    positiveButtonText = if (flag) getString(R.string.confirm_ok) else getString(R.string.income_withdraw_minium_confirm)
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
