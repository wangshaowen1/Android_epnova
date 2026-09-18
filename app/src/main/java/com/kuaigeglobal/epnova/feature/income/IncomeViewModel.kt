package com.kuaigeglobal.epnova.feature.income

import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.databinding.ObservableLong
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.common.Constant
import com.kuaigeglobal.epnova.common.utils.DateUtil
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.ext.request
import com.kuaigeglobal.epnova.core.network.apis.entity.IncomeDetailInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.IncomeSummaryInfo
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import com.nhsd.basic.ext.getResString
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest

class IncomeViewModel : AppRepositoryViewModel<AppRepository>() {
    companion object {
        //默认金额文本
        private const val DEFAULT_AMOUNT_TEXT = "\$0.00"
        private const val DEFAULT_BALANCE = 0L
        // 分单位
        const val BALANCE_UNIT = 100L
    }

    val mToolbarVM = ToolbarViewModel(
        navigationAction = { back() },
        title = R.string.profile_income.getResString()
    )

    //------------------页面数据绑定参数------------------
    //可提现余额
    val availableBalanceText = ObservableField(DEFAULT_AMOUNT_TEXT)
    val availableBalance = ObservableLong(DEFAULT_BALANCE)
    //累计收益
    val totalEarnedText = ObservableField(DEFAULT_AMOUNT_TEXT)
    //本月待结算金额
    val pendingAmountText = ObservableField(DEFAULT_AMOUNT_TEXT)
    //当前结算时区下的当前月份
    val earningsDetailsYearMonthText =
        ObservableField(DateUtil.formatYearMonthDisplay(DateUtil.getFormatDateTime("yyyy-MM")))

    //------------------年月选择器相关参数------------------
    //用户注册账号所在年月，格式 yyyy-MM，用作年月选择器最早可选值
    private var registeredYearMonth: String? = null
    //当前结算时区下的当前年月，格式 yyyy-MM，用作年月选择器最晚可选值
    private var currentYearMonth: String? = DateUtil.getFormatDateTime("yyyy-MM")
    //当前收益明细对应筛选的年月，格式 yyyy-MM；遮罩进行取消时不更新该值
    private var selectedYearMonth: String? = null

    //收益明细分页筛选年月触发器；Confirm 同一个年月也需要刷新列表，所以允许重复发射相同值
    private val incomeDetailsYearMonthFlow = MutableSharedFlow<String?>(replay = 1).apply {
        tryEmit(DateUtil.getFormatDateTime("yyyy-MM"))
    }

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
                //可提现余额
                availableBalanceText.set(d.availableBalanceText ?: DEFAULT_AMOUNT_TEXT)
                availableBalance.set(d.availableBalanceCent ?: DEFAULT_BALANCE)
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
                    incomeDetailsYearMonthFlow.tryEmit(selectedYearMonth)
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
        incomeDetailsYearMonthFlow.tryEmit(yearMonth)
    }

    /**
     * 用户收益明细分页。
     * 年月切换后会重新请求对应年月的收益明细。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getIncomeDetailsPage(): Flow<PagingData<IncomeDetailInfo>> {
        return incomeDetailsYearMonthFlow.flatMapLatest { yearMonth ->
            Pager(
                config = PagingConfig(
                    initialLoadSize = Constant.Paged.PAGE_SIZE,
                    pageSize = Constant.Paged.PAGE_SIZE,
                    prefetchDistance = Constant.Paged.PREFETCH_DISTANCE,
                ),
                pagingSourceFactory = {
                    IncomeDetailsPagingSource(yearMonth)
                }
            ).flow
        }.cachedIn(viewModelScope)
    }
}
