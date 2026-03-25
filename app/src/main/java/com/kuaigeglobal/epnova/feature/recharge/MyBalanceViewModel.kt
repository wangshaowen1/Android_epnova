package com.kuaigeglobal.epnova.feature.recharge

import androidx.annotation.LayoutRes
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.viewModelScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.common.adapter.QuickAdapter
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.manager.AppEvent
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.network.apis.response.SimpleBannerInfo
import com.kuaigeglobal.epnova.feature.details.ComputingPowerDetailsActivity
import com.kuaigeglobal.epnova.feature.web.SimpleBrowserActivity
import com.kuaigeglobal.epnova.widget.LoadStateViewHolder
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MyBalanceViewModel(@LayoutRes private val paymentItemLayout: Int) : AppRepositoryViewModel<PowerRepository>() {
    val toolbar by lazy {
        ToolbarViewModel(
            title = R.string.recharge_credits.getResString(),
            navigationAction = { back() },
            menuText = R.string.compute_details.getResString(),
            menuAction = { ComputingPowerDetailsActivity.start(topActivity) }
        )
    }

    var isDialog = false

    /**
     * 账户余额
     */
    val balance by lazy { ObservableField<String>() }
    val loadState by lazy { ObservableInt() }
    val powerAdapter by lazy { QuickAdapter<ItemRechargePowerVM>(R.layout.item_recharge_power, emptyList()) }
    val paymentAdapter by lazy { QuickAdapter<ItemRechargePaymentVM>(paymentItemLayout, emptyList()) }
    private var currentPower: ItemRechargePowerVM? = null
    private var currentPayment: ItemRechargePaymentVM? = null
    val workCourseBanner by lazy { ObservableField<SimpleBannerInfo>() }

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

    fun loadConfig() {
        if (powerAdapter.items.isEmpty() || paymentAdapter.items.isEmpty()) loadState.set(LoadStateViewHolder.LOADING)
        launch(
            block = {
                val pointConfigListDeferred = async { mRepo.pointConfigList().data }
                val paymentConfigListDeferred = async { mRepo.paymentConfigList().data }
                val pointConfigList = pointConfigListDeferred.await()
                val paymentConfigList = paymentConfigListDeferred.await()
                pointConfigList?.pointBalanceNum?.let { User.get().storeComputingPower(it) }
                pointConfigList?.items?.map { ItemRechargePowerVM(it).apply { onItemClick.value = { select() } } }.let { vms ->
                    powerAdapter.submitList(vms)
                    (vms?.firstOrNull { it.data.get()?.isDefault == 1 } ?: vms?.firstOrNull())?.select()
                    vms?.find { it.data.get()?.isDiscountValid() == true }?.let {
                        startGlobalCountDown()
                    }
                }
                paymentConfigList?.items?.map { ItemRechargePaymentVM(it).apply { onItemClick.value = { select() } } }.let { vms ->
                    paymentAdapter.submitList(vms)
                    (vms?.firstOrNull { it.data.get()?.isDefault == 1 } ?: vms?.firstOrNull())?.select()
                }
                updateLoadState()
            },
            onError = {
                updateLoadState()
            })
    }

    private fun updateLoadState() {
        if (powerAdapter.items.isEmpty() || paymentAdapter.items.isEmpty()) {
            loadState.set(LoadStateViewHolder.RETRY)
        } else {
            loadState.set(LoadStateViewHolder.CONTENT)
        }
    }

    private fun ItemRechargePowerVM.select() {
        currentPower?.isChecked?.set(false)
        isChecked.set(true)
        currentPower = this
    }

    private fun ItemRechargePaymentVM.select() {
        currentPayment?.isChecked?.set(false)
        isChecked.set(true)
        currentPayment = this
    }

    fun recharge() {
        val item = currentPower?.data?.get()
        if (item == null) {
            showToast(R.string.please_select_recharge_amount)
            return
        }
        val payment = currentPayment?.data?.get()
        val paymentMethod = payment?.paymentMethod
        if (paymentMethod == null) {
            showToast(R.string.select_the_payment_method)
            return
        }
        if (isDialog) {
            EventManager.onAppTrack(AppEvent.SF_CLICK_COMPUTE_RECHARGE_PAYMENT_BUTTON)
        }
//        pay(
//            paymentMethod,
//            { paymentMethod, requestBody, responseSender ->
//                launchWithLoading {
//                    response(mRepo.createPowerOrder(paymentMethod, item.pointConfigCode, payment.payMerchantConfigId)) {
//                        responseSender?.sendData(this.data?.toPaymentParams(paymentMethod))
//                    }
//                }
//            },
//            item,
//            { paymentData ->
//                if (isDialog) {
//                    EventManager.onAppTrack(AppEvent.SF_CLICK_COMPUTE_RECHARGE_PAYMENT_POPUP_PAYSUCCESS)
//                } else {
//                    EventManager.onAppTrack(AppEvent.SF_CLICK_RECHARGE_PAGE_PAYSUCCESS)
//                }
//                loadConfig()
//                refreshBalance()
//                PaySuccessDialog.checkAndShow()
//            },
//            { error, paymentData -> })
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
        balance.set(User.get().computingPower.toString())
    }

    private fun refreshBalance() {
        Global.refreshBalance()
    }

    private var countDownJob: Job? = null

    fun startGlobalCountDown() {
        countDownJob?.cancel()
        countDownJob = viewModelScope.launch {
            while (isActive) {
                // 每秒通知所有 Item 更新时间
                powerAdapter.items.forEach { vm ->
                    vm.updateTime()
                }
                delay(1000)
            }
        }
    }

    override fun onCleared() {
        countDownJob?.cancel()
        super.onCleared()
    }

}