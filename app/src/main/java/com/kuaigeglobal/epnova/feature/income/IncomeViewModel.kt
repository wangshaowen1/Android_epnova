package com.kuaigeglobal.epnova.feature.income

import androidx.databinding.ObservableField
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.utils.DateUtil
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.ext.request
import com.kuaigeglobal.epnova.core.network.apis.entity.IncomeSummaryInfo
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody

class IncomeViewModel : AppRepositoryViewModel<AppRepository>() {
    companion object {
        //默认值
        private const val DEFAULT_AMOUNT_TEXT = "\$0.00"
    }

    //可提现余额
    val availableBalanceText = ObservableField(DEFAULT_AMOUNT_TEXT)
    //累计收益
    val totalEarnedText = ObservableField(DEFAULT_AMOUNT_TEXT)
    //本月待结算金额
    val pendingAmountText = ObservableField(DEFAULT_AMOUNT_TEXT)
    //当前结算时区下的当前月份
    val currentMonthText = ObservableField<String>()
    val summaryInfo = ObservableField<IncomeSummaryInfo>()

    override fun onCreate() {
        super.onCreate()
        loadIncomeSummary()
    }

    /**
     * 用户收益汇总
     */
    fun loadIncomeSummary() {
        request(
            apiBlock = { mRepo.api.userIncomeSummary(UserTokenBody()) },
            onSuccess = {
                val d = data ?: return@request
                summaryInfo.set(d)
                //可提现余额
                availableBalanceText.set(d.availableBalanceText ?: DEFAULT_AMOUNT_TEXT)
                //累计收益
                totalEarnedText.set(d.totalEarnedText ?: DEFAULT_AMOUNT_TEXT)
                //本月待结算金额
                pendingAmountText.set(d.pendingAmountText ?: DEFAULT_AMOUNT_TEXT)
                //当前结算时区下的当前月份
                currentMonthText.set(DateUtil.formatYearMonthDisplay(d.currentMonth))
            }
        )
    }
}
