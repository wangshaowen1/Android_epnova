package com.kuaigeglobal.epnova.feature.member.vm

import android.graphics.Color
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.common.adapter.QuickAdapter
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.ext.request
import com.kuaigeglobal.epnova.core.ext.requestWithLoading
import com.kuaigeglobal.epnova.core.google.billing.BillingManager
import com.kuaigeglobal.epnova.core.network.apis.response.MemberBenefitItemInfo
import com.kuaigeglobal.epnova.core.network.apis.response.MemberPlanMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 会员页
 * @createtime 2026年08月17日 09:51
 * @author TooWhiteT
 */

class MemberIndexViewModel : AppRepositoryViewModel<AppRepository>() {

    val mToolbarVM = ToolbarViewModel(
        navigationAction = { finish() },
        title = "",
        background = Color.TRANSPARENT,
    )

    /** 是否已订阅 */
    val planMode = ObservableBoolean(false)

    // 权益适配器
    val privilegeAdapter = QuickAdapter<ItemMemberPrivilegeViewModel>(
        R.layout.item_member_privilege,
        emptyList(),
    )
    // 订阅档位适配器
    val memberRechargeAdapter = QuickAdapter<ItemMemberRechargeViewModel>(
        R.layout.item_member_recharge,
        emptyList(),
    )
    val selectedPeriodPointNum = ObservableField("")
    val selectedPeriodUnit = ObservableField("")
    // 当前选中档位
    private var currentRechargeItem: ItemMemberRechargeViewModel? = null

    init {
        BillingManager.markActivePage(true)
    }

    override fun onCleared() {
        BillingManager.markActivePage(false)
        super.onCleared()
    }

    fun loadMemberPlans() {
        requestWithLoading(
            apiBlock = { mRepo.api.getMemberPlanList() },
            onSuccess = {
                planMode.set(data?.pageMode == MemberPlanMode.SWITCH.name)

                currentRechargeItem = null
                val items = data?.plans.orEmpty().map { plan ->
                    ItemMemberRechargeViewModel(plan).also { item ->
                        item.onItemClick.value = { selectPlan(item) }
                    }
                }

                val selectedItem = items.firstOrNull { it.data.get()?.defaultSelected == 1 }
                    ?: items.firstOrNull()

                selectedItem?.isSelected?.set(true)

                memberRechargeAdapter.submitList(items)

                updateSelectedPlan(selectedItem)

                loadSubscriptionPrices(items)
            },
            onError = {
                planMode.set(false)
            }
        )
    }

    private fun selectPlan(item: ItemMemberRechargeViewModel) {
        if (currentRechargeItem === item) return
        currentRechargeItem?.isSelected?.set(false)
        item.isSelected.set(true)
        updateSelectedPlan(item)
    }

    private fun updateSelectedPlan(item: ItemMemberRechargeViewModel?) {
        currentRechargeItem = item
        val plan = item?.data?.get()

        selectedPeriodPointNum.set(plan?.periodPointNum?.toString().orEmpty())
        selectedPeriodUnit.set("/${plan?.periodUnit.orEmpty()}")

        submitPrivileges(plan?.benefitItems)
    }

    private fun submitPrivileges(benefitItems: List<MemberBenefitItemInfo>?) {
        privilegeAdapter.submitList(
            benefitItems.orEmpty().map(::ItemMemberPrivilegeViewModel),
        )
    }

    /**
     * 套餐接口只返回商店商品标识，展示价格以 Google Play 本地化价格为准。
     */
    private fun loadSubscriptionPrices(items: List<ItemMemberRechargeViewModel>) {
        val products = items.mapNotNull { it.data.get()?.productId }
            .filter { it.isNotBlank() }
            .distinct()
            .map { it to BillingClient.ProductType.SUBS }
        if (products.isEmpty()) return

        BillingManager.preloadProductDetails(products) { productDetails ->
            viewModelScope.launch(Dispatchers.Main) {
                items.forEach { item ->
                    val plan = item.data.get() ?: return@forEach
                    val details = productDetails
                        ?.firstOrNull { it.productId == plan.productId }
                        ?: return@forEach
                    val configuredOfferId = plan.offerId?.takeIf { it.isNotBlank() }
                    val configuredBasePlanId = plan.basePlanId?.takeIf { it.isNotBlank() }
                    val offers = details.subscriptionOfferDetails.orEmpty()
                    val offer = offers.firstOrNull {
                        (configuredBasePlanId == null || it.basePlanId == configuredBasePlanId) &&
                            it.offerId == configuredOfferId
                    } ?: offers.firstOrNull {
                        configuredBasePlanId == null || it.basePlanId == configuredBasePlanId
                    } ?: return@forEach
                    val phases = offer.pricingPhases.pricingPhaseList
                    val currentPrice = phases.firstOrNull()?.formattedPrice.orEmpty()
                    val standardPrice = phases.lastOrNull()?.formattedPrice.orEmpty()
                    val hasStoreDiscount = phases.size > 1 &&
                        standardPrice.isNotEmpty() && standardPrice != currentPrice
                    item.priceText.set(currentPrice)
                    item.showDiscount.set(
                        hasStoreDiscount && (plan.discountPercent ?: 0) > 0,
                    )
                    item.originalPriceText.set(
                        standardPrice.takeIf { hasStoreDiscount }.orEmpty(),
                    )
                }
            }
        }
    }

}
