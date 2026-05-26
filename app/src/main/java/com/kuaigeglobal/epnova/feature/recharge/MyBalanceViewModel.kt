package com.kuaigeglobal.epnova.feature.recharge

import androidx.annotation.LayoutRes
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.StringUtils.getString
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.adapter.QuickAdapter
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.google.billing.BillingManager
import com.kuaigeglobal.epnova.core.google.billing.PurchaseResult
import com.kuaigeglobal.epnova.core.manager.AppEvent
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.network.apis.response.SimpleBannerInfo
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.feature.details.ComputingPowerDetailsActivity
import com.kuaigeglobal.epnova.feature.web.SimpleBrowserActivity
import com.kuaigeglobal.epnova.widget.LoadStateViewHolder
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

class MyBalanceViewModel(
    @LayoutRes private val paymentItemLayout: Int,
    initialTab: Int = TAB_COINS
) : AppRepositoryViewModel<CoinRepository>() {

    companion object {
        const val TAB_COINS = 0
        const val TAB_CREDITS = 1
    }

    var currentTab = initialTab
        private set

    var isDialog = false
    val cost by lazy { mBundle.getInt(Constants.Extra.COST, 0) }

    private val _purchaseEvent = MutableSharedFlow<PurchaseResult>(extraBufferCapacity = 1)
    val purchaseEvent: SharedFlow<PurchaseResult> = _purchaseEvent.asSharedFlow()

    private var purchaseJob: Job? = null

    val balance by lazy { ObservableField<String>() }
    val creditsBalance by lazy { ObservableField<String>("0") }
    val displayBalance by lazy { ObservableField<String>() }
    val balanceLabel by lazy { ObservableField<String>(R.string.store_coins.getResString()) }
    val userGiveCoins by lazy { ObservableField<String>() }
    val userPurchasedCoins by lazy { ObservableField<String>() }
    val loadState by lazy { ObservableInt() }
    val workCourseBanner by lazy { ObservableField<SimpleBannerInfo>() }

    val coinsAdapter by lazy { QuickAdapter<ItemRechargePowerVM>(R.layout.item_recharge_power, emptyList()) }
    val creditsAdapter by lazy { QuickAdapter<ItemRechargeCreditsVM>(R.layout.item_recharge_credits, emptyList()) }

    private var currentCoinsItem: ItemRechargePowerVM? = null
    private var currentCreditsItem: ItemRechargeCreditsVM? = null

    init {
        BillingManager.markActivePage(true)
        BillingManager.queryPendingPurchases()
    }

    override fun onCreate() {
        super.onCreate()
        loadConfig()
    }

    override fun onResume() {
        super.onResume()
        updateBalance()
        refreshBalance()
        workCourseBanner.set(if (User.get().isPurchasedCourse()) null else Config.get().appConfig.workCourseBanner)
    }

    fun onTabSelected(index: Int) {
        currentTab = index
        when (index) {
            TAB_COINS -> {
                balanceLabel.set(R.string.store_coins.getResString())
                displayBalance.set(balance.get())
                if (coinsAdapter.items.isEmpty()) loadCoinsConfig()
                else loadState.set(LoadStateViewHolder.CONTENT)
            }
            TAB_CREDITS -> {
                balanceLabel.set(R.string.store_credits.getResString())
                displayBalance.set(creditsBalance.get())
                if (creditsAdapter.items.isEmpty()) loadCreditsConfig()
                else loadState.set(LoadStateViewHolder.CONTENT)
            }
        }
    }

    fun loadConfig() {
        if (currentTab == TAB_COINS) loadCoinsConfig() else loadCreditsConfig()
    }

    private fun loadCoinsConfig() {
        if (coinsAdapter.items.isEmpty()) loadState.set(LoadStateViewHolder.LOADING)
        launch(
            block = {
                val deferred = async { mRepo.coinConfigList().data }
                val data = deferred.await()
                data?.coionsBalanceNum?.let { User.get().storeCoins(it) }
                updateBalance()
                data?.items?.map { ItemRechargePowerVM(it).apply { onItemClick.value = { selectCoins() } } }.let { vms ->
                    coinsAdapter.submitList(vms)
                    loadGoogleProductsForCoins(vms)
                    vms?.find { it.data.get()?.isDiscountValid() == true }?.let { startGlobalCountDown() }
                }
                updateLoadState()
            },
            onError = { updateLoadState() }
        )
    }

    fun loadCreditsConfig() {
        if (creditsAdapter.items.isEmpty()) loadState.set(LoadStateViewHolder.LOADING)
        launch(
            block = {
                val deferred = async { mRepo.pointConfigList().data }
                val data = deferred.await()
                data?.pointBalanceNum?.let {
                    creditsBalance.set(it.toString())
                    if (currentTab == TAB_CREDITS) displayBalance.set(it.toString())
                }
                data?.items?.map { ItemRechargeCreditsVM(it).apply { onItemClick.value = { selectCredits() } } }.let { vms ->
                    creditsAdapter.submitList(vms)
                    loadGoogleProductsForCredits(vms)
                    vms?.find { it.data.get()?.isDiscountValid() == true }?.let { startGlobalCountDown() }
                }
                updateLoadState()
            },
            onError = { updateLoadState() }
        )
    }

    private fun loadGoogleProductsForCoins(vms: List<ItemRechargePowerVM>?) {
        val productIds = vms?.mapNotNull { it.data.get()?.googleProductId } ?: return
        BillingManager.loadPowerPrices(productIds) { map ->
            viewModelScope.launch(Dispatchers.Main) {
                if (map.isEmpty()) {
                    getString(R.string.network_error_fetch_products).showToast()
                    return@launch
                }
                vms.forEach { item ->
                    val productCode = item.data.get()?.googleProductId
                    if (map.containsKey(productCode)) {
                        item.priceText.set(map[productCode]?.first)
                    }
                }
                coinsAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun loadGoogleProductsForCredits(vms: List<ItemRechargeCreditsVM>?) {
        val productIds = vms?.mapNotNull { it.data.get()?.googleProductId } ?: return
        BillingManager.loadPowerPrices(productIds) { map ->
            viewModelScope.launch(Dispatchers.Main) {
                if (map.isEmpty()) {
                    getString(R.string.network_error_fetch_products).showToast()
                    return@launch
                }
                vms.forEach { item ->
                    val productCode = item.data.get()?.googleProductId
                    if (map.containsKey(productCode)) {
                        item.priceText.set(map[productCode]?.first)
                    }
                }
                creditsAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun updateLoadState() {
        val isEmpty = if (currentTab == TAB_COINS) coinsAdapter.items.isEmpty() else creditsAdapter.items.isEmpty()
        loadState.set(if (isEmpty) LoadStateViewHolder.RETRY else LoadStateViewHolder.CONTENT)
    }

    private fun ItemRechargePowerVM.selectCoins() {
        currentCoinsItem?.isChecked?.set(false)
        isChecked.set(true)
        currentCoinsItem = this
        recharge()
    }

    private fun ItemRechargeCreditsVM.selectCredits() {
        currentCreditsItem?.isChecked?.set(false)
        isChecked.set(true)
        currentCreditsItem = this
        recharge()
    }

    fun recharge() {
        purchaseJob?.cancel()
        purchaseJob = viewModelScope.launch {
            BillingManager.purchaseResultFlow.take(1).collect { result ->
                if (result is PurchaseResult.Success) {
                    loadConfig()
                    refreshBalance()
                }
                _purchaseEvent.emit(result)
            }
        }

        if (currentTab == TAB_COINS) {
            rechargeCoins()
        } else {
            rechargeCredits()
        }
    }

    private fun rechargeCoins() {
        val item = currentCoinsItem?.data?.get()
        if (item == null) {
            showToast(R.string.please_select_recharge_amount)
            purchaseJob?.cancel()
            return
        }
        if (isDialog) EventManager.onAppTrack(AppEvent.SF_CLICK_COMPUTE_RECHARGE_PAYMENT_BUTTON)
        launchWithLoading(
            block = {
                response(mRepo.createGoogleOrder(item.coionsConfigCode)) {
                    BillingManager.launchPurchase(topActivity, item.googleProductId ?: "", data?.orderNo ?: "")
                }
            },
            onError = { purchaseJob?.cancel() }
        )
    }

    private fun rechargeCredits() {
        val item = currentCreditsItem?.data?.get()
        if (item == null) {
            showToast(R.string.please_select_recharge_amount)
            purchaseJob?.cancel()
            return
        }
        if (isDialog) EventManager.onAppTrack(AppEvent.SF_CLICK_COMPUTE_RECHARGE_PAYMENT_BUTTON)
        launchWithLoading(
            block = {
                response(mRepo.createGoogleOrder(item.pointConfigCode)) {
                    BillingManager.launchPurchase(topActivity, item.googleProductId ?: "", data?.orderNo ?: "")
                }
            },
            onError = { purchaseJob?.cancel() }
        )
    }

    fun navigateToHistory() {
        ComputingPowerDetailsActivity.start(topActivity, currentTab)
    }

    fun viewPaymentAgreement() {
        val activity = topActivity ?: return
        val url = Config.get().appConfig.clientPaymentProtocolUrl ?: return
        SimpleBrowserActivity.launch(activity, url)
    }

    fun viewServicePurchaseInstructions() {
        val activity = topActivity ?: return
        val url = Config.get().appConfig.clientTermsOfServiceUrl ?: return
        SimpleBrowserActivity.launch(activity, url)
    }

    fun updateBalance() {
        User.get().apply {
            userPurchasedCoins.set((coins - giveCoins).coerceAtLeast(0).toString())
            balance.set(coins.toString())
            userGiveCoins.set(User.get().giveCoins.toString())
        }
        if (currentTab == TAB_COINS) displayBalance.set(balance.get())
    }

    private fun refreshBalance() {
        Global.refreshBalance()
    }

    private var countDownJob: Job? = null

    fun startGlobalCountDown() {
        countDownJob?.cancel()
        countDownJob = viewModelScope.launch {
            while (isActive) {
                coinsAdapter.items.forEach { vm -> vm.updateTime() }
                creditsAdapter.items.forEach { vm -> vm.updateTime() }
                delay(1000)
            }
        }
    }

    override fun onCleared() {
        BillingManager.markActivePage(false)
        countDownJob?.cancel()
        purchaseJob?.cancel()
        super.onCleared()
    }
}
