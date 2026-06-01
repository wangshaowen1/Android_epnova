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
        //默认金额文本
        private const val DEFAULT_AMOUNT_TEXT = "\$0.00"
    }

    //------------------页面数据绑定参数------------------
    //可提现余额
    val availableBalanceText = ObservableField(DEFAULT_AMOUNT_TEXT)
    //累计收益
    val totalEarnedText = ObservableField(DEFAULT_AMOUNT_TEXT)
    //本月待结算金额
    val pendingAmountText = ObservableField(DEFAULT_AMOUNT_TEXT)
    //当前结算时区下的当前月份
    val earningsDetailsYearMonthText =
        ObservableField(DateUtil.formatYearMonthDisplay(DateUtil.getFormatDateTime("yyyy-MM")))

    //用户收益汇总接口原始数据，保留给页面后续扩展使用
    val summaryInfo = ObservableField<IncomeSummaryInfo>()

    //------------------年月选择器相关参数------------------
    //用户注册账号所在年月，格式 yyyy-MM，用作年月选择器最早可选值
    private var registeredYearMonth: String? = null
    //当前结算时区下的当前年月，格式 yyyy-MM，用作年月选择器最晚可选值
    private var currentYearMonth: String? = DateUtil.getFormatDateTime("yyyy-MM")
    //当前收益明细对应筛选的年月，格式 yyyy-MM；遮罩进行取消时不更新该值
    private var selectedYearMonth: String? = null

    override fun onCreate() {
        super.onCreate()
        loadIncomeSummary()
    }

    /**
     * 请求用户收益汇总，并同步年月筛选范围。
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

                //年月选择器最早可选值
                registeredYearMonth = d.registerMonth
                //年月选择器最晚可选值
                currentYearMonth = d.currentMonth ?: currentYearMonth

                //首次进入页面时，默认选中当前结算年月；用户确认选择后不被汇总接口刷新覆盖。
                if (selectedYearMonth.isNullOrBlank()) {
                    selectedYearMonth = currentYearMonth
                }
                earningsDetailsYearMonthText.set(DateUtil.formatYearMonthDisplay(selectedYearMonth))
            }
        )
    }

    /**
     * 获取年月选择器最早可选年月，格式 yyyy-MM。
     */
    fun getMinSelectableYearMonth(): String? = registeredYearMonth ?: currentYearMonth

    /**
     * 获取年月选择器最晚可选年月，格式 yyyy-MM。
     */
    fun getMaxSelectableYearMonth(): String? = currentYearMonth

    /**
     * 获取当前收益明细对应筛选的年月，格式 yyyy-MM。
     */
    fun getSelectedYearMonth(): String? =
        selectedYearMonth ?: currentYearMonth

    /**
     * 确认选择收益明细筛选年月。
     *
     * @param yearMonth 年月选择器筛选的年月，格式 yyyy-MM。
     */
    fun confirmIncomeDetailsYearMonth(yearMonth: String) {
        selectedYearMonth = yearMonth
        earningsDetailsYearMonthText.set(DateUtil.formatYearMonthDisplay(yearMonth))
        //用户收益明细分页（获取对应年月收益明细）
        refreshIncomeDetails(yearMonth)
    }

    /**
     *用户收益明细分页（获取对应年月收益明细）
     *
     * @param yearMonth 收益明细筛选年月，格式 yyyy-MM。
     */
    @Suppress("UNUSED_PARAMETER")
    private fun refreshIncomeDetails(yearMonth: String) {

    }
}
