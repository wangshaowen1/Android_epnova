package com.kuaigeglobal.epnova.feature.member.vm

import android.graphics.Color
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.base.viewmodel.ToolbarViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.adapter.QuickAdapter
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.Global
import com.kuaigeglobal.epnova.core.ext.requestWithLoading
import com.kuaigeglobal.epnova.core.google.billing.BillingManager
import com.kuaigeglobal.epnova.core.google.billing.MemberBillingResult
import com.kuaigeglobal.epnova.core.google.billing.MemberSubscriptionRecoveryManager
import com.kuaigeglobal.epnova.core.google.billing.MemberSubscriptionRecoveryStore
import com.kuaigeglobal.epnova.core.google.billing.MemberSubscriptionStorePreparationResult
import com.kuaigeglobal.epnova.core.manager.AppsFlyerManager
import com.kuaigeglobal.epnova.core.manager.EventManager
import com.kuaigeglobal.epnova.core.manager.FirebaseEvent
import com.kuaigeglobal.epnova.core.network.apis.ApiException
import com.kuaigeglobal.epnova.core.network.apis.request.GoogleMemberSubscriptionBody
import com.kuaigeglobal.epnova.core.network.apis.request.MemberSubscriptionPrepareBody
import com.kuaigeglobal.epnova.core.network.apis.request.MemberSubscriptionPrepareReleaseBody
import com.kuaigeglobal.epnova.core.network.apis.response.MemberActionEnum
import com.kuaigeglobal.epnova.core.network.apis.response.MemberBenefitItemInfo
import com.kuaigeglobal.epnova.core.network.apis.response.MemberBenefitItemType
import com.kuaigeglobal.epnova.core.network.apis.response.MemberPlanInfo
import com.kuaigeglobal.epnova.core.network.apis.response.MemberPlanMode
import com.kuaigeglobal.epnova.core.network.apis.response.MemberSubscriptionPrepareData
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * 会员页
 * @createtime 2026年08月17日 09:51
 * @author TooWhiteT
 */

class MemberIndexViewModel : AppRepositoryViewModel<AppRepository>() {

    companion object {
        private const val MEMBER_PERIOD_UNIT_WEEK = "WEEK"
        private const val MEMBER_PERIOD_UNIT_MONTH = "MONTH"

        /** 返回会员页后给 PurchasesUpdatedListener 的正常回调留出时间，超时才主动查 SUBS。 */
        private const val MEMBER_BILLING_CALLBACK_GRACE_MS = 1_500L

        /** 同进程支付页刚返回且未收到回调时，无购买结果最多确认两次，避免商店状态短暂延迟。 */
        private const val MEMBER_EMPTY_QUERY_MAX_ATTEMPTS = 2

        /** Billing 正在连接或临时断网时最多自动重试一次，之后等待下次前台信号。 */
        private const val MEMBER_QUERY_FAILURE_MAX_ATTEMPTS = 2

        /** 官方凭证交叉校验失败：保留恢复信号，重新查询商店后再决定是否 sync。 */
        private const val MEMBER_ERROR_PURCHASE_CROSS_CHECK_FAILED = 300185

        /**
         * 已有有效订阅、状态不支持切换、账号归属、配置、环境及原平台限制都属于明确业务
         * 结果：刷新后端页面并停止客户端自动重试，不能用 sync 覆盖业务判断。
         */
        private val MEMBER_TERMINAL_BUSINESS_ERRORS = setOf(
            300181,
            300182,
            300184,
            300186,
            300189,
            300191,
        )
    }

    val mToolbarVM = ToolbarViewModel(
        navigationAction = { finish() },
        title = "",
        background = Color.TRANSPARENT,
    )

    /** 是否已订阅 */
    val planMode = ObservableBoolean(false)

    /** 会员页入口场景，仅普通会员商店曝光使用；升级页按表格不携带 Label。 */
    private val entryScene by lazy {
        mBundle.getString(Constants.Extra.FROM_PAGE)
            ?.takeIf { it.isNotBlank() }
            ?: FirebaseEvent.Value.TRIGGER_MEMBERSHIP_ENTRY
    }

    /** 套餐刷新、支付恢复会重复调用 loadMemberPlans，页面曝光在单个 VM 生命周期内只上报一次。 */
    private var hasReportedPageShow = false

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

    /**
     * 当前选中套餐是否已经满足页面购买条件。
     *
     * 该值是订阅按钮 enabled 的唯一来源，统一收敛套餐 action、接口必填字段、Google Offer
     * 解析结果和本地支付互斥状态，XML 不再重复拼业务条件。
     */
    val canSubscribe = ObservableBoolean(false)

    /**
     * 当前是否存在未收敛的会员支付流程，供页面后续扩展 loading 文案或进度展示使用。
     * PENDING、结果未知及恢复同步期间也保持 true，不能只覆盖网络 Loading 阶段。
     */
    val isSubscribing = ObservableBoolean(false)

    /**
     * 底部补充描述文案
     */
    val bottomHint = ObservableField<String>("")

    // 当前选中档位
    private var currentRechargeItem: ItemMemberRechargeViewModel? = null

    /** SWITCH 使用的当前订阅商品；只有套餐列表中唯一 CURRENT 商品存在时才有值。 */
    private var currentSubscriptionProductId: String? = null

    /** 已通过 Google Product + Base Plan + Offer 严格解析的不可变套餐版本。 */
    private val storeReadyPlanVersionCodes = mutableSetOf<String>()

    /** plan/list 成功返回后才允许计算购买资格；刷新或失败期间保持 false。 */
    private var memberPlansReady = false
        set(value) {
            field = value
            updateSubscribeAvailability()
        }

    /**
     * 页面内只允许存在一笔会员购买编排。
     *
     * true 覆盖后端 prepare、商店参数查询、Billing 页面、confirm/release 全阶段；只有明确终态
     * 才复位。PENDING 或结果未知时保持 true，避免用户在官方交易未收敛时重复点击。
     */
    private var memberPurchaseBusy = false
        set(value) {
            field = value
            isSubscribing.set(value)
            updateSubscribeAvailability()
        }

    /** 当前已经取得后端 prepare token 的会员购买上下文，仅保存在 ViewModel 内存中。 */
    private var activeMemberPurchase: ActiveMemberPurchaseContext? = null

    /** 防止 Google 对同一 PURCHASED 重复回调时并发调用 confirm。 */
    private var confirmingMemberPurchase = false

    /** recovery 查询和 sync 都有独立重复启动保护，避免前台回调与 Billing 回调并发补偿。 */
    private var memberRecoveryQueryInProgress = false
    private var memberRecoveryQueryGeneration = 0L
    private var memberEmptyRecoveryQueryCount = 0
    private var memberRecoveryQueryFailureCount = 0
    private var syncingMemberPurchase = false

    /** 明确业务错误后阻止当前页面继续购买，等待重新进入页面取得新的后端资格。 */
    private var memberPurchaseBlocked = false
        set(value) {
            field = value
            updateSubscribeAvailability()
        }

    /** 页面级网络请求、Billing 观察与返回页面后的延迟恢复任务；ViewModel 销毁时统一取消。 */
    private var memberPurchaseRequestJob: Job? = null
    private var memberBillingResultJob: Job? = null
    private var memberProcessRecoveryJob: Job? = null
    private var memberResumeRecoveryJob: Job? = null

    init {
        BillingManager.markActivePage(true)
        observeMemberBillingResults()
        observeProcessRecoverySettled()
    }

    override fun onResume() {
        super.onResume()

        val activeContext = activeMemberPurchase
        if (activeContext != null && activeContext.billingLaunchStarted) {
            // Activity 从 Google 支付页返回时，先等待正常 Billing 回调；若没有回调，再以 SUBS
            // 查询作为官方事实来源。PENDING/PURCHASED 回调到达时会取消这个延迟任务。
            scheduleMemberRecoveryAfterBillingReturn(activeContext)
        } else if (activeContext != null && activeContext.recoveryQueryOnResume) {
            // SWITCH 参数准备阶段发现旧订阅为 PENDING/未知时没有拉起新交易。后续重新前台化
            // 才复查商店，避免在同一次查询回调里形成无界轮询。
            queryMemberSubscriptionsForRecovery()
        } else if (activeContext == null && MemberSubscriptionRecoveryStore.hasPendingForCurrentUser()) {
            // 新 ViewModel 仍存在恢复标记，说明上次流程可能因页面退出或进程重启丢失回调。
            // 若首页的进程级恢复已经占有该标记，页面只展示进行中状态并等待其终态通知，不能
            // 同时查询/sync 同一笔购买；没有后台任务时仍由页面立即接管恢复。
            memberPurchaseBusy = true
            if (!MemberSubscriptionRecoveryManager.isRecovering()) {
                queryMemberSubscriptionsForRecovery()
            }
        }
    }

    override fun onDestroy() {
        memberPurchaseRequestJob?.cancel()
        memberBillingResultJob?.cancel()
        memberProcessRecoveryJob?.cancel()
        memberResumeRecoveryJob?.cancel()
        BillingManager.markActivePage(false)
        super.onDestroy()
    }

    private fun reportMemberPageShowIfNeeded(isUpgradeMode: Boolean) {
        if (hasReportedPageShow) return
        hasReportedPageShow = true
        if (isUpgradeMode) {
            EventManager.onFirebaseTrack(
                FirebaseEvent.Category.MEMBERSHIP_UPGRADE,
                FirebaseEvent.Action.UPGRADE_SHOW,
            )
        } else {
            EventManager.onFirebaseTrack(
                FirebaseEvent.Category.MEMBERSHIP,
                FirebaseEvent.Action.MEMBERSHIP_SHOW,
                entryScene,
            )
        }
    }

    fun reportMemberPlanClick() {
        val plan = currentRechargeItem?.data?.get() ?: return
        if (plan.action == MemberActionEnum.SWITCH.name) {
            EventManager.onFirebaseTrack(
                FirebaseEvent.Category.MEMBERSHIP_UPGRADE,
                FirebaseEvent.Action.PRODUCT_CLICK,
            )
        } else {
            EventManager.onFirebaseTrack(
                FirebaseEvent.Category.MEMBERSHIP,
                FirebaseEvent.Action.PRODUCT_CLICK,
                membershipPeriodLabel(plan.periodUnit),
            )
        }
    }

    private fun reportMemberPurchaseResult(
        context: ActiveMemberPurchaseContext,
        isSuccess: Boolean,
    ) {
        if (context.analyticsResultReported) return
        context.analyticsResultReported = true
        val result = if (isSuccess) {
            FirebaseEvent.Value.RESULT_SUCCESS
        } else {
            FirebaseEvent.Value.RESULT_FAIL
        }
        if (context.actionType == MemberActionEnum.SWITCH.name) {
            EventManager.onFirebaseTrack(
                FirebaseEvent.Category.MEMBERSHIP_UPGRADE,
                FirebaseEvent.Action.UPGRADE_RESULT,
                result,
            )
        } else {
            EventManager.onFirebaseTrack(
                FirebaseEvent.Category.MEMBERSHIP,
                FirebaseEvent.Action.PURCHASE_RESULT,
                membershipPeriodLabel(context.periodUnit),
                result,
            )
        }
    }

    private fun membershipPeriodLabel(periodUnit: String?): String? {
        return when (periodUnit) {
            MEMBER_PERIOD_UNIT_WEEK -> FirebaseEvent.Value.MEMBERSHIP_PERIOD_WEEKLY
            MEMBER_PERIOD_UNIT_MONTH -> FirebaseEvent.Value.MEMBERSHIP_PERIOD_MONTHLY
            else -> null
        }
    }

    /**
     * 点击页面底部按钮后启动会员购买或套餐切换。
     *
     * 套餐 action 是后端资格事实：只有 PURCHASE / SWITCH 可以进入支付，其余 CURRENT、
     * PENDING_SWITCH、UNAVAILABLE 都直接禁止，不能根据页面标题或本地会员状态自行推断动作。
     */
    fun startMemberSubscription() {
        if (memberPurchaseBusy || memberPurchaseBlocked) return

        // 页面重建后 onResume 与点击可能紧邻发生。恢复标记存在时优先查询官方交易，不允许
        // 新建 prepare 覆盖上一笔未完成流程。
        if (MemberSubscriptionRecoveryStore.hasPendingForCurrentUser()) {
            memberPurchaseBusy = true
            if (!MemberSubscriptionRecoveryManager.isRecovering()) {
                queryMemberSubscriptionsForRecovery()
            }
            return
        }

        // enabled 绑定只负责 UI，方法入口仍以同一个语义状态做防御，避免无障碍调用、自动化
        // 点击或状态刷新竞态绕过“Offer 尚未解析成功”等购买前提。
        if (!canSubscribe.get()) return

        val selectedPlan = currentRechargeItem?.data?.get() ?: return
        val actionType = selectedPlan.action?.takeIf { action ->
            action == MemberActionEnum.PURCHASE.name ||
                action == MemberActionEnum.SWITCH.name
        } ?: return

        val planVersionCode = selectedPlan.planVersionCode?.takeIf { it.isNotBlank() }
        val productId = selectedPlan.productId?.takeIf { it.isNotBlank() }
        val basePlanId = selectedPlan.basePlanId?.takeIf { it.isNotBlank() }
        val currentProductId = if (actionType == MemberActionEnum.SWITCH.name) {
            currentSubscriptionProductId
        } else {
            null
        }
        if (
            planVersionCode == null ||
            productId == null ||
            basePlanId == null ||
            (actionType == MemberActionEnum.SWITCH.name && currentProductId == null)
        ) {
            showPaymentFailed()
            return
        }

        // BillingManager 可能仍持有上一个页面留下的 PURCHASED/PENDING/未知交易。先检查再请求
        // 后端 prepare，避免相同目标拿到同一个 token 后被新页面误 release。
        if (!BillingManager.canStartMemberSubscriptionPreparation()) {
            // 全局 BillingClient 可能正被积分购买或上一笔会员流程占用。同步收紧按钮状态，
            // 实际拉起时仍会由 BillingManager 的原子锁做最终二次校验。
            updateSubscribeAvailability()
            return
        }

        memberPurchaseBusy = true
        memberPurchaseRequestJob = requestWithLoading(
            apiBlock = {
                mRepo.api.prepareMemberSubscriptionPurchase(
                    MemberSubscriptionPrepareBody(
                        actionType = actionType,
                        planVersionCode = planVersionCode,
                    )
                )
            },
            onSuccess = {
                memberPurchaseRequestJob = null
                handleMemberPrepareSuccess(
                    selectedPlan = selectedPlan,
                    actionType = actionType,
                    currentProductId = currentProductId,
                    prepareData = data,
                )
            },
            onError = {
                // prepare 未成功时商店流程尚未开始，不存在需要恢复的 Google 交易。
                memberPurchaseRequestJob = null
                memberPurchaseBusy = false
                activeMemberPurchase = null
            }
        )
    }

    /**
     * 把后端 prepare 响应与用户点击时的套餐快照组合为本次购买上下文。
     */
    private fun handleMemberPrepareSuccess(
        selectedPlan: MemberPlanInfo,
        actionType: String,
        currentProductId: String?,
        prepareData: MemberSubscriptionPrepareData?,
    ) {
        val prepareToken = prepareData?.purchasePrepareToken?.takeIf { it.isNotBlank() }
        val preparedProductId = prepareData?.productId?.takeIf { it.isNotBlank() }
        val preparedBasePlanId = prepareData?.basePlanId?.takeIf { it.isNotBlank() }
        val obfuscatedAccountId = prepareData?.obfuscatedAccountId?.takeIf { it.isNotBlank() }

        // plan/list 与 purchase/prepare 应指向同一不可变套餐版本。若商品标识发生漂移，继续购买
        // 会造成页面展示和实际扣款不一致，因此在拉起商店前释放本次 prepare。
        val matchesSelectedPlan = preparedProductId == selectedPlan.productId &&
            preparedBasePlanId == selectedPlan.basePlanId
        if (
            prepareToken == null ||
            preparedProductId == null ||
            preparedBasePlanId == null ||
            obfuscatedAccountId == null ||
            !matchesSelectedPlan
        ) {
            finishInvalidMemberPrepare(prepareToken)
            return
        }

        val context = ActiveMemberPurchaseContext(
            actionType = actionType,
            periodUnit = selectedPlan.periodUnit,
            purchasePrepareToken = prepareToken,
            productId = preparedProductId,
            basePlanId = preparedBasePlanId,
            requestedOfferId = selectedPlan.offerId,
            obfuscatedAccountId = obfuscatedAccountId,
            googleReplacementMode = prepareData.googleReplacementMode,
            currentProductId = currentProductId,
        )
        activeMemberPurchase = context
        memberEmptyRecoveryQueryCount = 0
        memberRecoveryQueryFailureCount = 0

        // prepare 成功即创建恢复信号，而不是等 Google 回调后才保存。这样即使拉起支付后进程
        // 被系统回收，下次进入会员页仍能先查询 SUBS；标记不包含任何 purchaseToken。
        val recoverySignalSaved = MemberSubscriptionRecoveryStore.save(
            productId = context.productId,
            obfuscatedAccountId = context.obfuscatedAccountId,
        )
        if (!recoverySignalSaved) {
            // 无稳定本地账号标识时无法保证进程重启后仍只恢复到当前 EpNova 账号。商店尚未
            // 拉起，可以安全释放 prepare；不能为了继续扣款而跳过恢复能力。
            releaseMemberPrepare(
                context = context,
                toastRes = R.string.system_payment_failed,
                finishBillingContext = false,
            )
            return
        }

        BillingManager.loadMemberSubscriptionLaunchParams(
            actionType = context.actionType,
            productId = context.productId,
            basePlanId = context.basePlanId,
            requestedOfferId = context.requestedOfferId,
            obfuscatedAccountId = context.obfuscatedAccountId,
            googleReplacementMode = context.googleReplacementMode,
            currentProductId = context.currentProductId,
        ) { result ->
            // ProductDetails 回调不作为页面生命周期所有者；切回 viewModelScope 后，页面销毁时
            // 后续状态写入会自动取消，也不会误拉起已经离开的 Activity。
            viewModelScope.launch(Dispatchers.Main) {
                if (activeMemberPurchase !== context) return@launch
                handleStorePreparationResult(context, result)
            }
        }
    }

    /**
     * 处理商店侧参数准备结果。只有 Ready 会真正拉起 Google 支付页。
     */
    private fun handleStorePreparationResult(
        context: ActiveMemberPurchaseContext,
        result: MemberSubscriptionStorePreparationResult,
    ) {
        when (result) {
            is MemberSubscriptionStorePreparationResult.Ready -> {
                // 必须先标记再调用：参数校验失败和 launchBillingFlow 同步失败都可能立即向 Flow
                // 发事件，监听端需要知道该事件属于已经开始拉起的本次上下文。
                val pricingPhase = result.launchParams.resolvedOffer.offerDetails
                    .pricingPhases
                    .pricingPhaseList
                    .firstOrNull()
                // 将预获取到的商店套餐金额和币种存到上下文中方便后续归因上报使用
                context.priceAmountMicros = pricingPhase?.priceAmountMicros
                context.priceCurrencyCode = pricingPhase?.priceCurrencyCode

                context.billingLaunchStarted = true
                BillingManager.launchMemberSubscription(
                    activity = topActivity,
                    launchParams = result.launchParams,
                )
            }

            is MemberSubscriptionStorePreparationResult.Pending,
            is MemberSubscriptionStorePreparationResult.Uncertain -> {
                // SWITCH 查询到旧订阅仍待处理或状态未知：没有新交易被拉起，但也不能继续购买。
                // 保留 prepare 和页面 busy；下次页面重新前台化时再查询 SUBS，不在此递归轮询。
                context.recoveryQueryOnResume = true
            }

            is MemberSubscriptionStorePreparationResult.Failed -> {
                // Product/Offer/旧订阅匹配失败发生在拉起商店之前，能够确定没有新官方交易，
                // 因此可以安全释放后端 prepare。
                releaseMemberPrepare(
                    context = context,
                    toastRes = R.string.system_payment_failed,
                    finishBillingContext = false,
                )
            }
        }
    }

    /**
     * 持续监听会员 Billing 结果；不能 take(1)，因为 PENDING 后 Google 仍可能再次回调 PURCHASED。
     */
    private fun observeMemberBillingResults() {
        memberBillingResultJob?.cancel()
        memberBillingResultJob = viewModelScope.launch {
            BillingManager.memberBillingResultFlow.collect { result ->
                val context = activeMemberPurchase
                val recoverySignal = currentMemberRecoverySignal()
                if (confirmingMemberPurchase || syncingMemberPurchase) return@collect

                when (result) {
                    is MemberBillingResult.Purchased -> {
                        memberResumeRecoveryJob?.cancel()
                        if (context != null && context.matches(result.purchase)) {
                            context.officialPurchaseObserved = true
                        }
                        if (
                            context != null &&
                            context.releaseRequested &&
                            recoverySignal != null &&
                            recoverySignal.matches(result.purchase)
                        ) {
                            // 极少数设备可能在取消/失败后补发 PURCHASED。尽量取消尚未完成的
                            // release；即使后端已释放锁，官方交易仍可继续 confirm/sync 验真。
                            memberPurchaseRequestJob?.cancel()
                            memberPurchaseRequestJob = null
                            context.releaseRequested = false
                        }
                        when {
                            // 正常本次支付仍必须优先 confirm，不能为了复用恢复代码改走 sync。
                            context != null &&
                                context.billingLaunchStarted &&
                                context.matches(result.purchase) &&
                                !context.confirmAttempted -> {
                                invalidateMemberRecoveryQuery()
                                confirmMemberPurchase(context, result.purchase)
                            }

                            context != null && context.billingLaunchStarted &&
                                context.confirmAttempted -> {
                                // confirm 已经提交过但没有得到成功结果，禁止重复 confirm，也不能
                                // 直接凭重复回调 sync；先重新查询 SUBS 再进入恢复同步。
                                queryMemberSubscriptionsForRecovery()
                            }

                            // 进程/页面恢复后即使收到 PURCHASED 更新，也先重新查询当前 SUBS，
                            // 让 sync 始终建立在一次成功的商店查询结果之上。
                            context == null && recoverySignal != null &&
                                recoverySignal.matches(result.purchase) -> {
                                queryMemberSubscriptionsForRecovery()
                            }

                            context?.billingLaunchStarted == true || recoverySignal != null -> {
                                // 回调商品或账号标识不匹配，不能使用该 purchase；重新查询当前
                                // SUBS，让严格筛选逻辑统一处理多订阅和异常回调。
                                queryMemberSubscriptionsForRecovery()
                            }
                        }
                    }

                    is MemberBillingResult.Pending -> {
                        // PENDING 只保留上下文并等待下一次 Google 更新，不 confirm、不 release。
                        if (recoverySignal != null && recoverySignal.matches(result.purchase)) {
                            memberResumeRecoveryJob?.cancel()
                            invalidateMemberRecoveryQuery()
                            if (context != null && context.matches(result.purchase)) {
                                context.officialPurchaseObserved = true
                            }
                            if (context != null && context.releaseRequested) {
                                // PENDING 也是有效官方交易状态；让尚未完成的 release 回调失效，
                                // 保留 prepare 和路由等待后续 PURCHASED。
                                memberPurchaseRequestJob?.cancel()
                                memberPurchaseRequestJob = null
                                context.releaseRequested = false
                            }
                            memberEmptyRecoveryQueryCount = 0
                            memberPurchaseBusy = true
                            BillingManager.restoreMemberBillingContext(recoverySignal.productId)
                        }
                    }

                    is MemberBillingResult.Uncertain -> {
                        // 已拥有、断线、超时等结果不能视为失败。只有存在本次上下文或持久恢复
                        // 标记时才查询 SUBS，普通页面进入不会无条件触发 sync。
                        if (context?.billingLaunchStarted == true || recoverySignal != null) {
                            memberResumeRecoveryJob?.cancel()
                            queryMemberSubscriptionsForRecovery()
                        }
                    }

                    MemberBillingResult.Cancelled -> {
                        memberResumeRecoveryJob?.cancel()
                        invalidateMemberRecoveryQuery()
                        if (context != null && context.officialPurchaseObserved) {
                            // 已经取得过 PURCHASED/PENDING，后到的取消结果不能推翻官方交易
                            // 事实，更不能 release；重新查询 SUBS 后按 sync 恢复。
                            queryMemberSubscriptionsForRecovery()
                        } else if (context != null && context.billingLaunchStarted) {
                            releaseMemberPrepare(
                                context = context,
                                toastRes = R.string.system_payment_cancel,
                                finishBillingContext = true,
                            )
                        } else if (recoverySignal != null) {
                            // 恢复路由下仍收到明确取消，但进程内已没有 prepare token。清除本地
                            // 恢复信号并刷新后端页面，不能构造空 token 调 release。
                            finishMemberRecoveryWithoutPurchase(refreshPage = true)
                        }
                    }

                    MemberBillingResult.Failed.Busy -> {
                        // Busy 表示 BillingManager 中已有另一条支付链路；只能释放本次新 prepare，
                        // 不能 finish 并清掉另一条链路的会员回调上下文。
                        memberResumeRecoveryJob?.cancel()
                        invalidateMemberRecoveryQuery()
                        context?.takeIf { it.billingLaunchStarted }?.let {
                            releaseMemberPrepare(
                                context = it,
                                toastRes = R.string.system_payment_failed,
                                finishBillingContext = false,
                            )
                        }
                    }

                    is MemberBillingResult.Failed -> {
                        memberResumeRecoveryJob?.cancel()
                        invalidateMemberRecoveryQuery()
                        if (context != null && context.officialPurchaseObserved) {
                            // 与取消分支相同，官方购买一旦出现，任何后续失败都只允许走
                            // 查询与 sync，不再允许释放 prepare。
                            queryMemberSubscriptionsForRecovery()
                        } else if (context != null && context.billingLaunchStarted) {
                            releaseMemberPrepare(
                                context = context,
                                toastRes = R.string.system_payment_failed,
                                finishBillingContext = true,
                            )
                        } else if (recoverySignal != null) {
                            finishMemberRecoveryWithoutPurchase(refreshPage = true)
                        }
                    }
                }
            }
        }
    }

    /**
     * 首页可能在当前会员页可见期间完成一次进程级 sync。这里只接收“已经收敛”的通知并重新
     * 读取套餐；全局积分由恢复管理器刷新。PENDING、查询失败等非终态不会发通知，页面继续
     * 保持不可重复购买。
     */
    private fun observeProcessRecoverySettled() {
        memberProcessRecoveryJob?.cancel()
        memberProcessRecoveryJob = viewModelScope.launch {
            MemberSubscriptionRecoveryManager.settled.collect {
                // 页面内存在完整 prepare 上下文时，终态必须仍由 confirm/release 链路处理，不能
                // 被进程恢复事件越权清理。正常情况下二者由恢复管理器的互斥检查保证不并发。
                if (activeMemberPurchase == null) {
                    memberPurchaseBusy = false
                    loadMemberPlans()
                }
            }
        }
    }

    /** Google 已明确 PURCHASED，优先使用本次 prepare token 调用正常 confirm。 */
    private fun confirmMemberPurchase(
        context: ActiveMemberPurchaseContext,
        purchase: Purchase,
    ) {
        if (confirmingMemberPurchase) return
        memberResumeRecoveryJob?.cancel()
        context.confirmAttempted = true
        confirmingMemberPurchase = true
        memberPurchaseRequestJob = requestWithLoading(
            apiBlock = {
                mRepo.api.confirmGoogleMemberSubscriptionPurchase(
                    GoogleMemberSubscriptionBody(
                        purchaseToken = purchase.purchaseToken,
                        productId = context.productId,
                        purchasePrepareToken = context.purchasePrepareToken,
                        basePlanId = context.basePlanId,
                        appsFlyerId = Config.get().appsFlyerId
                    )
                )
            },
            onSuccess = {
                memberPurchaseRequestJob = null
                confirmingMemberPurchase = false
                completeConfirmedMemberPurchase(context)
                // 只上报PURCHASE订阅购买类型，订阅升级不上报  1.3.8 统一后台上报af支付事件
//                if (context.actionType == MemberActionEnum.PURCHASE.name) {
//                    // 未获取到金额和币种不进行归因上报
//                    if (context.priceAmountMicros != null && context.priceCurrencyCode != null) {
//                        AppsFlyerManager.logPurchase(
//                            productId = context.productId,
//                            orderId = purchase.orderId,
//                            priceAmountMicros = context.priceAmountMicros,
//                            currency = context.priceCurrencyCode
//                        )
//                    }
//                }
            },
            onError = { error ->
                // 已取得 PURCHASED 后禁止 release。网络结果未知及 300185 先查询商店再 sync；
                // 其他明确业务 code 按文档刷新并阻止本页面重复购买，不自动 sync。
                memberPurchaseRequestJob = null
                confirmingMemberPurchase = false
                handleMemberConfirmError(context, error)
            }
        )
    }

    /** confirm 成功后以后端为准刷新套餐和积分，不直接在客户端修改会员状态。 */
    private fun completeConfirmedMemberPurchase(context: ActiveMemberPurchaseContext) {
        if (activeMemberPurchase !== context) return

        invalidateMemberRecoveryQuery()
        memberEmptyRecoveryQueryCount = 0
        memberRecoveryQueryFailureCount = 0
        activeMemberPurchase = null
        MemberSubscriptionRecoveryStore.clearForCurrentUser()
        BillingManager.finishMemberBillingFlow()
        memberPurchaseBusy = false
        finishSuccessfulMemberPurchase(context)
    }

    /**
     * 仅在用户取消、拉起前校验失败等“客户端能够确认没有官方交易”的场景释放后端 prepare。
     * release 自身失败不会改变“商店未成交”的事实，准备锁会自动过期，因此本地仍结束本次
     * 流程，避免页面永久卡死。超时、无回调及查询空结果不允许调用此方法。
     */
    private fun releaseMemberPrepare(
        context: ActiveMemberPurchaseContext,
        toastRes: Int?,
        finishBillingContext: Boolean,
    ) {
        if (context.releaseRequested) return
        context.releaseRequested = true

        memberPurchaseRequestJob = requestWithLoading(
            apiBlock = {
                mRepo.api.releaseMemberSubscriptionPurchasePrepare(
                    MemberSubscriptionPrepareReleaseBody(context.purchasePrepareToken)
                )
            },
            showLoading = false,
            showToastError = false,
            onSuccess = {
                finishReleasedMemberPrepare(
                    context,
                    toastRes,
                    finishBillingContext,
                )
            },
            onError = {
                finishReleasedMemberPrepare(
                    context,
                    toastRes,
                    finishBillingContext,
                )
            }
        )
    }

    private fun finishReleasedMemberPrepare(
        context: ActiveMemberPurchaseContext,
        toastRes: Int?,
        finishBillingContext: Boolean,
    ) {
        // release 发出后若 Google 又补发 PURCHASED，购买结果优先，旧 release 回调必须失效。
        // 后端即使已经释放两分钟锁，也不能否定随后取得的官方购买，confirm 仍可正常验真。
        if (!context.releaseRequested) return
        if (context.billingLaunchStarted && !context.officialPurchaseObserved) {
            // 失败埋点延后到 release 收敛，避免迟到的 PURCHASED 被先到的取消/失败误锁成 fail。
            reportMemberPurchaseResult(context, isSuccess = false)
        }
        memberPurchaseRequestJob = null
        confirmingMemberPurchase = false
        if (activeMemberPurchase === context) {
            invalidateMemberRecoveryQuery()
            memberEmptyRecoveryQueryCount = 0
            memberRecoveryQueryFailureCount = 0
            activeMemberPurchase = null
            MemberSubscriptionRecoveryStore.clearForCurrentUser()
        }
        if (finishBillingContext) BillingManager.finishMemberBillingFlow()
        memberPurchaseBusy = false
        toastRes?.getResString()?.showToast()
    }

    /** prepare 成功但响应字段不完整/漂移时，能拿到 token 就主动释放，否则直接结束本地流程。 */
    private fun finishInvalidMemberPrepare(prepareToken: String?) {
        if (prepareToken == null) {
            memberPurchaseBusy = false
            activeMemberPurchase = null
            showPaymentFailed()
            return
        }

        val invalidContext = ActiveMemberPurchaseContext(
            actionType = "",
            periodUnit = null,
            purchasePrepareToken = prepareToken,
            productId = "",
            basePlanId = "",
            requestedOfferId = null,
            obfuscatedAccountId = "",
            googleReplacementMode = null,
            currentProductId = null,
        )
        releaseMemberPrepare(
            context = invalidContext,
            toastRes = R.string.system_payment_failed,
            finishBillingContext = false,
        )
    }

    /**
     * 页面从 Google Billing 返回时等待正常回调；超过宽限时间仍无终态才查询当前 SUBS。
     * 任务属于页面 ViewModel，重复 onResume 会先取消旧任务，onCleared 也会取消。
     */
    private fun scheduleMemberRecoveryAfterBillingReturn(context: ActiveMemberPurchaseContext) {
        if (
            confirmingMemberPurchase ||
            syncingMemberPurchase ||
            memberRecoveryQueryInProgress ||
            context.releaseRequested
        ) {
            return
        }

        memberResumeRecoveryJob?.cancel()
        memberResumeRecoveryJob = viewModelScope.launch {
            delay(MEMBER_BILLING_CALLBACK_GRACE_MS)
            memberResumeRecoveryJob = null
            if (
                activeMemberPurchase === context &&
                context.billingLaunchStarted &&
                !confirmingMemberPurchase &&
                !context.releaseRequested
            ) {
                queryMemberSubscriptionsForRecovery()
            }
        }
    }

    /** 第一次商店空结果后的有界二次确认；任一 Billing 终态到达都会取消该 Job。 */
    private fun scheduleEmptyMemberSubscriptionRequery(context: ActiveMemberPurchaseContext) {
        memberPurchaseBusy = true
        memberResumeRecoveryJob?.cancel()
        memberResumeRecoveryJob = viewModelScope.launch {
            delay(MEMBER_BILLING_CALLBACK_GRACE_MS)
            memberResumeRecoveryJob = null
            if (
                activeMemberPurchase === context &&
                !confirmingMemberPurchase &&
                !syncingMemberPurchase &&
                !context.releaseRequested
            ) {
                queryMemberSubscriptionsForRecovery()
            }
        }
    }

    /** Billing 连接/查询失败后的单次有界重试，避免初始化时与价格查询抢连接导致永久停住。 */
    private fun scheduleFailedMemberSubscriptionQueryRetry(
        recoverySignal: MemberSubscriptionRecoveryStore.Signal,
    ) {
        memberResumeRecoveryJob?.cancel()
        memberResumeRecoveryJob = viewModelScope.launch {
            delay(MEMBER_BILLING_CALLBACK_GRACE_MS)
            memberResumeRecoveryJob = null
            if (
                isCurrentMemberRecoverySignal(recoverySignal) &&
                !confirmingMemberPurchase &&
                !syncingMemberPurchase
            ) {
                queryMemberSubscriptionsForRecovery()
            }
        }
    }

    /**
     * 查询 Google 当前 SUBS，并只处理和当前恢复信号同时匹配商品及账号标识的购买。
     *
     * 该查询只由明确恢复信号触发：Billing 结果未知、confirm 结果未知、支付页返回却无回调，
     * 或持久化标记表明上次流程未收敛。普通打开会员页不会无条件查询或 sync。
     */
    private fun queryMemberSubscriptionsForRecovery() {
        if (memberRecoveryQueryInProgress || syncingMemberPurchase) return
        if (
            activeMemberPurchase == null &&
            MemberSubscriptionRecoveryManager.isRecovering()
        ) {
            // 进程级恢复已经持有同一持久化信号时，页面不能并发查询并重复调用幂等 sync。
            // 后台得到终态会通过 settled 通知刷新；非终态则保留 busy，等下一次前台机会。
            memberPurchaseBusy = true
            return
        }
        val recoverySignal = currentMemberRecoverySignal() ?: run {
            memberPurchaseBusy = false
            return
        }

        // 先恢复全局 Billing 回调路由。若另一条支付链路正在处理，保留标记等待下一次前台
        // 机会，绝不能覆盖 orderId 或另一个会员商品的路由上下文。
        if (!BillingManager.restoreMemberBillingContext(recoverySignal.productId)) {
            memberPurchaseBusy = true
            return
        }

        memberPurchaseBusy = true
        memberRecoveryQueryInProgress = true
        val queryGeneration = ++memberRecoveryQueryGeneration
        BillingManager.queryMemberSubscriptions { billingResult, purchases ->
            viewModelScope.launch(Dispatchers.Main) {
                if (queryGeneration != memberRecoveryQueryGeneration) return@launch
                memberRecoveryQueryInProgress = false
                if (!isCurrentMemberRecoverySignal(recoverySignal)) return@launch

                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    // 查询失败只代表当前无法获知商店事实。保持 busy、恢复标记和 Billing 路由，
                    // 连接并发或临时网络问题只做一次有界重试；仍失败则等下次页面前台化，
                    // 不 release、不使用旧 token 猜测 sync。
                    memberPurchaseBusy = true
                    if (++memberRecoveryQueryFailureCount < MEMBER_QUERY_FAILURE_MAX_ATTEMPTS) {
                        scheduleFailedMemberSubscriptionQueryRetry(recoverySignal)
                    }
                    return@launch
                }

                memberRecoveryQueryFailureCount = 0

                // 成功查询已经取代可能缺失的 PurchasesUpdatedListener 回调，可以释放 SDK 的
                // 瞬时等待位；商品路由仍保留到 sync/release/刷新真正收敛。
                BillingManager.markMemberSubscriptionQueryResolved(recoverySignal.productId)
                handleMemberSubscriptionQueryResult(recoverySignal, purchases)
            }
        }
    }

    private fun handleMemberSubscriptionQueryResult(
        recoverySignal: MemberSubscriptionRecoveryStore.Signal,
        purchases: List<Purchase>,
    ) {
        val matchingPurchases = purchases.filter(recoverySignal::matches)
            .distinctBy { purchase -> purchase.purchaseToken }
        if (matchingPurchases.isNotEmpty()) {
            activeMemberPurchase?.takeIf { context ->
                recoverySignal.hasSameTarget(context.productId, context.obfuscatedAccountId)
            }?.officialPurchaseObserved = true
        }

        if (matchingPurchases.any { it.purchaseState == Purchase.PurchaseState.PENDING }) {
            // PENDING 是明确的商店状态：继续保留上下文并等待后续更新，不 confirm、不 sync、
            // 不 release。恢复 Billing 路由以便进程存活期间接收后续 PURCHASED 回调。
            memberPurchaseBusy = true
            memberEmptyRecoveryQueryCount = 0
            BillingManager.restoreMemberBillingContext(recoverySignal.productId)
            return
        }

        val purchased = matchingPurchases.filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        if (purchased.isNotEmpty()) {
            memberEmptyRecoveryQueryCount = 0
            syncRecoveredMemberPurchases(recoverySignal, purchased)
            return
        }

        if (matchingPurchases.isNotEmpty()) {
            // UNSPECIFIED_STATE 仍不是“无交易”的证明，保留信号等待下一次查询，不能释放。
            memberPurchaseBusy = true
            memberEmptyRecoveryQueryCount = 0
            return
        }

        val activeContext = activeMemberPurchase?.takeIf { context ->
            recoverySignal.hasSameTarget(context.productId, context.obfuscatedAccountId)
        }
        if (activeContext != null) {
            if (
                activeContext.billingLaunchStarted &&
                ++memberEmptyRecoveryQueryCount < MEMBER_EMPTY_QUERY_MAX_ATTEMPTS
            ) {
                // 支付页刚返回时 Google 的购买缓存可能比 Activity 恢复稍晚。第一次空结果不
                // 立即结束恢复，短暂等待后再确认一次；最多两次，避免形成无界轮询。
                scheduleEmptyMemberSubscriptionRequery(activeContext)
                return
            }
            if (activeContext.officialPurchaseObserved) {
                // 客户端曾明确取得 PURCHASED/PENDING 或成功查询到官方购买，即使当前查询已
                // 没有有效购买，也禁止 release 原 prepare。此时不使用历史 token 猜测 sync，
                // 只刷新后端页面，等待 RTDN/对账收敛。
                finishMemberRecoveryWithoutPurchase(refreshPage = true)
                return
            }
            // 结果不确定路径即使成功查到空列表，也不能反推“商店从未创建交易”。严格按恢复
            // 流程只结束本地等待并刷新 plan/list，让两分钟 prepare 锁自行过期；release 只留给
            // 用户取消或拉起前明确失败，避免商店状态延迟时过早释放。
            finishMemberRecoveryWithoutPurchase(refreshPage = true)
        } else {
            memberEmptyRecoveryQueryCount = 0
            // 进程恢复后没有原 prepare token，禁止构造空 token 调 release；直接清理恢复标记并
            // 刷新 plan/list，等待后端通知和对账给出最终页面事实。
            finishMemberRecoveryWithoutPurchase(refreshPage = true)
        }
    }

    /**
     * 对商店重新查询得到的 PURCHASED 逐笔调用 sync。列表通常只有一笔；若 Google 返回多笔
     * 同商品、同账号的当前购买，则逐笔提交真实凭证，由后端永久幂等，绝不 firstOrNull 猜测。
     */
    private fun syncRecoveredMemberPurchases(
        recoverySignal: MemberSubscriptionRecoveryStore.Signal,
        purchases: List<Purchase>,
    ) {
        if (syncingMemberPurchase || purchases.isEmpty()) return
        if (!BillingManager.restoreMemberBillingContext(recoverySignal.productId)) return

        memberResumeRecoveryJob?.cancel()
        memberPurchaseBusy = true
        syncingMemberPurchase = true
        syncNextRecoveredMemberPurchase(recoverySignal, purchases, index = 0)
    }

    private fun syncNextRecoveredMemberPurchase(
        recoverySignal: MemberSubscriptionRecoveryStore.Signal,
        purchases: List<Purchase>,
        index: Int,
    ) {
        if (!isCurrentMemberRecoverySignal(recoverySignal)) {
            syncingMemberPurchase = false
            return
        }
        if (index >= purchases.size) {
            completeRecoveredMemberPurchase()
            return
        }

        val purchase = purchases[index]
        val activeContext = activeMemberPurchase?.takeIf { context ->
            recoverySignal.hasSameTarget(context.productId, context.obfuscatedAccountId)
        }
        memberPurchaseRequestJob = requestWithLoading(
            apiBlock = {
                mRepo.api.syncGoogleMemberSubscription(
                    GoogleMemberSubscriptionBody(
                        purchaseToken = purchase.purchaseToken,
                        productId = recoverySignal.productId,
                        // 同进程 confirm 失败恢复时仍可携带原 prepare 信息；进程重启后为空，
                        // 后端必须以 Google 官方购买为准，不能依赖本地历史 token。
                        purchasePrepareToken = activeContext?.purchasePrepareToken,
                        // queryPurchasesAsync 的 Purchase 不可靠暴露当前 Base Plan。尤其同一个
                        // Product 内 Weekly→Monthly 切换时，不能把目标 basePlanId 猜成查询结果；
                        // sync 留空，由后端 subscriptionsv2.get 获取官方事实。
                        basePlanId = null,
                        appsFlyerId = Config.get().appsFlyerId
                    )
                )
            },
            showLoading = false,
            onSuccess = {
                memberPurchaseRequestJob = null
                syncNextRecoveredMemberPurchase(recoverySignal, purchases, index + 1)
            },
            onError = { error ->
                memberPurchaseRequestJob = null
                syncingMemberPurchase = false
                handleMemberSyncError(error)
            }
        )
    }

    /** 正常 confirm 的失败分流：只有结果未知或 300185 才进入商店查询与 sync。 */
    private fun handleMemberConfirmError(
        context: ActiveMemberPurchaseContext,
        error: Throwable?,
    ) {
        if (activeMemberPurchase !== context) return
        when {
            error !is ApiException -> queryMemberSubscriptionsForRecovery()
            error.errorCode == MEMBER_ERROR_PURCHASE_CROSS_CHECK_FAILED -> {
                queryMemberSubscriptionsForRecovery()
            }

            error.errorCode in MEMBER_TERMINAL_BUSINESS_ERRORS -> {
                finishMemberPurchaseOnBusinessError()
            }

            else -> {
                // 未登记 code 不能擅自猜成某个稳定业务分支。官方交易已经 PURCHASED，按结果
                // 未知处理：查询商店后用后端幂等 sync 收敛，避免服务端已提交但响应异常时丢单。
                queryMemberSubscriptionsForRecovery()
            }
        }
    }

    /**
     * sync 已经建立在一次成功 SUBS 查询之上，失败后不递归立即 query/sync，避免网络或业务
     * 异常形成死循环。300185、网络异常和未登记 code 保留恢复信号，下一次前台机会再查询；
     * 只有文档列出的稳定终态 code 才结束客户端重试、刷新页面并阻止本页面再次购买。
     */
    private fun handleMemberSyncError(error: Throwable?) {
        when {
            error !is ApiException ||
                error.errorCode == MEMBER_ERROR_PURCHASE_CROSS_CHECK_FAILED -> {
                memberPurchaseBusy = true
                currentMemberRecoverySignal()?.let { signal ->
                    BillingManager.restoreMemberBillingContext(signal.productId)
                }
            }

            error.errorCode in MEMBER_TERMINAL_BUSINESS_ERRORS -> {
                finishMemberPurchaseOnBusinessError()
            }

            else -> {
                // sync 已经失败一次，未知 code 不立即递归调用；保留信号等待下一次前台查询。
                memberPurchaseBusy = true
                currentMemberRecoverySignal()?.let { signal ->
                    BillingManager.restoreMemberBillingContext(signal.productId)
                }
            }
        }
    }

    private fun completeRecoveredMemberPurchase() {
        val activeContext = activeMemberPurchase
        memberPurchaseRequestJob = null
        invalidateMemberRecoveryQuery()
        memberEmptyRecoveryQueryCount = 0
        memberRecoveryQueryFailureCount = 0
        syncingMemberPurchase = false
        confirmingMemberPurchase = false
        activeMemberPurchase = null
        MemberSubscriptionRecoveryStore.clearForCurrentUser()
        BillingManager.finishMemberBillingFlow()
        memberPurchaseBusy = false
        if (activeContext != null) {
            finishSuccessfulMemberPurchase(activeContext)
        } else {
            refreshMemberPageState()
        }
    }

    /** 明确业务错误禁止自动 sync，也禁止 release 已存在的官方 PURCHASED。 */
    private fun finishMemberPurchaseOnBusinessError() {
        activeMemberPurchase?.let { context ->
            reportMemberPurchaseResult(context, isSuccess = false)
        }
        memberResumeRecoveryJob?.cancel()
        memberPurchaseRequestJob = null
        invalidateMemberRecoveryQuery()
        memberEmptyRecoveryQueryCount = 0
        memberRecoveryQueryFailureCount = 0
        syncingMemberPurchase = false
        confirmingMemberPurchase = false
        activeMemberPurchase = null
        memberPurchaseBlocked = true
        MemberSubscriptionRecoveryStore.clearForCurrentUser()
        BillingManager.finishMemberBillingFlow()
        memberPurchaseBusy = false
        refreshMemberPageState()
    }

    private fun finishMemberRecoveryWithoutPurchase(refreshPage: Boolean) {
        memberResumeRecoveryJob?.cancel()
        memberPurchaseRequestJob = null
        invalidateMemberRecoveryQuery()
        memberEmptyRecoveryQueryCount = 0
        memberRecoveryQueryFailureCount = 0
        syncingMemberPurchase = false
        confirmingMemberPurchase = false
        activeMemberPurchase = null
        MemberSubscriptionRecoveryStore.clearForCurrentUser()
        BillingManager.finishMemberBillingFlow()
        memberPurchaseBusy = false
        if (refreshPage) refreshMemberPageState()
    }

    private fun currentMemberRecoverySignal(): MemberSubscriptionRecoveryStore.Signal? {
        val activeContext = activeMemberPurchase
        return if (activeContext != null) {
            MemberSubscriptionRecoveryStore.Signal(
                productId = activeContext.productId,
                obfuscatedAccountId = activeContext.obfuscatedAccountId,
            )
        } else {
            MemberSubscriptionRecoveryStore.getForCurrentUser()
        }
    }

    private fun isCurrentMemberRecoverySignal(
        signal: MemberSubscriptionRecoveryStore.Signal,
    ): Boolean {
        return currentMemberRecoverySignal()?.hasSameTarget(signal) == true
    }

    /** 使已经发出的商店查询回调失效；底层只读查询可完成，但不能覆盖更晚到达的 Billing 事实。 */
    private fun invalidateMemberRecoveryQuery() {
        memberRecoveryQueryGeneration++
        memberRecoveryQueryInProgress = false
    }

    private fun refreshMemberPageState() {
        loadMemberPlans()
        Global.refreshCoinsAndPoints()
    }

    /** 同页订阅成功后按目标套餐提示，并返回来源页。 */
    private fun finishSuccessfulMemberPurchase(context: ActiveMemberPurchaseContext) {
        reportMemberPurchaseResult(context, isSuccess = true)
        Global.refreshCoinsAndPoints()
        Global.refreshUserInfo()
        val toastRes = when {
            context.periodUnit == MEMBER_PERIOD_UNIT_WEEK -> {
                R.string.member_membership_activated
            }
            context.periodUnit == MEMBER_PERIOD_UNIT_MONTH -> {
                R.string.member_upgrade_scheduled
            }
            else -> R.string.system_payment_success
        }
        toastRes.getResString().showToast()
        finish()
    }

    private fun showPaymentFailed() {
        R.string.system_payment_failed.getResString().showToast()
    }

    fun loadMemberPlans() {
        // 刷新期间旧套餐仍可能暂时显示，但不能继续使用旧的 action/Offer 发起新购买。
        memberPlansReady = false
        storeReadyPlanVersionCodes.clear()
        currentSubscriptionProductId = null
        requestWithLoading(
            apiBlock = { mRepo.api.getMemberPlanList() },
            onSuccess = {
                val isUpgradeMode = data?.pageMode == MemberPlanMode.SWITCH.name
                planMode.set(isUpgradeMode)
                if (data != null) reportMemberPageShowIfNeeded(isUpgradeMode)

                currentRechargeItem = null
                val items = data?.plans.orEmpty().map { plan ->
                    ItemMemberRechargeViewModel(plan).also { item ->
                        item.onItemClick.value = { selectPlan(item) }
                    }
                }

                // SWITCH 的旧商品必须唯一。若后端异常返回多个 CURRENT，不能用 firstOrNull
                // 猜测，否则后续可能把错误订阅的 purchaseToken 传给 Google ReplacementMode。
                currentSubscriptionProductId = items.mapNotNull { item ->
                    item.data.get()
                        ?.takeIf { plan -> plan.action == MemberActionEnum.CURRENT.name }
                        ?.productId
                        ?.takeIf { productId -> productId.isNotBlank() }
                }.distinct().singleOrNull()

                // 默认选中逻辑
                // 如果子项action都等于pageMode，则取defaultSelected为1的，否则取子项中action等于pageMode的首项
                val pageMode = data?.pageMode
                val selectedItem = if (items.all { it.data.get()?.action == pageMode }) {
                    items.firstOrNull { it.data.get()?.defaultSelected == 1 }
                        ?: items.firstOrNull()
                } else {
                    items.firstOrNull { it.data.get()?.action == pageMode }
                }

                selectedItem?.isSelected?.set(true)

                memberRechargeAdapter.submitList(items)

                memberPlansReady = true
                updateSelectedPlan(selectedItem)

                loadSubscriptionPrices(items)
            },
            onError = {
                planMode.set(false)
                memberPlansReady = false
            }
        )
    }

    private fun selectPlan(item: ItemMemberRechargeViewModel) {
        val plan = item.data.get() ?: return
        val action = plan.action
        if (
            action != MemberActionEnum.PURCHASE.name &&
            action != MemberActionEnum.SWITCH.name
        ) return

        // prepare 已开始后，本次交易必须继续使用点击购买时的套餐快照；支付收敛前禁止切换
        // 页面选中项，避免页面积分/价格展示与即将由 Google 扣款的套餐不一致。
        if (memberPurchaseBusy) return
        if (currentRechargeItem === item) return
        currentRechargeItem?.isSelected?.set(false)
        item.isSelected.set(true)
        updateSelectedPlan(item)
    }

    private fun updateSelectedPlan(item: ItemMemberRechargeViewModel?) {
        currentRechargeItem = item
        val plan = item?.data?.get()

        selectedPeriodPointNum.set(plan?.periodPointNum?.toString().orEmpty())
        selectedPeriodUnit.set("/${plan?.periodUnit.orEmpty().lowercase()}")
        updateBottomHint()

        submitPrivileges(item)
        updateSubscribeAvailability()
    }

    private fun updateBottomHint() {
        val item = currentRechargeItem
        val plan = item?.data?.get()
        if (item == null || plan == null) {
            bottomHint.set("")
            return
        }

        val hint = when {
            plan.action == MemberActionEnum.SWITCH.name -> {
                R.string.member_switch_plan_hint.getResString(plan.planName.orEmpty())
            }

            else -> ""
        }
        bottomHint.set(hint)
    }

    private fun submitPrivileges(item: ItemMemberRechargeViewModel?) {
        val plan = item?.data?.get()
        val benefitItems = plan?.benefitItems.orEmpty().toMutableList()
        if (item != null && plan?.periodUnit == MEMBER_PERIOD_UNIT_WEEK) {
            val currentPrice = item.priceText.get().orEmpty()
            val originalPrice = item.originalPriceText.get().orEmpty()
            // originalPriceText 只有在 Google 仍返回指定优惠且优惠价与原价不同时才会写入。
            if (currentPrice.isNotBlank() && originalPrice.isNotBlank()) {
                // 存在首周优惠 新增权益一条
                benefitItems.add(
                    MemberBenefitItemInfo(
                        itemType = MemberBenefitItemType.TEXT.name,
                        text = R.string.member_intro_offer_hint.getResString(
                            currentPrice,
                            originalPrice,
                        ),
                    ),
                )
            }
        }
        privilegeAdapter.submitList(
            benefitItems.map(::ItemMemberPrivilegeViewModel),
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

                    // 展示价格与购买前校验必须复用同一套严格解析规则：首周优惠不可用时，
                    // 只允许回退到同 Base Plan 的标准 Offer，不能误选其他营销 Offer。
                    val resolvedOffer = BillingManager.resolveMemberSubscriptionOffer(
                        productDetails = details,
                        basePlanId = plan.basePlanId,
                        requestedOfferId = plan.offerId,
                    ) ?: return@forEach
                    val phases = resolvedOffer.offerDetails.pricingPhases.pricingPhaseList
                    val currentPrice = phases.firstOrNull()?.formattedPrice.orEmpty()
                    val planVersionCode = plan.planVersionCode?.takeIf { it.isNotBlank() }
                    if (
                        planVersionCode == null ||
                        resolvedOffer.offerToken.isBlank() ||
                        currentPrice.isBlank()
                    ) {
                        // 无法同时确认购买 Offer 和本地化价格时保持按钮禁用；不能只展示后端
                        // 价格或允许点击后再临时选择其他 Offer。
                        return@forEach
                    }
                    val standardPrice = phases.lastOrNull()?.formattedPrice.orEmpty()
                    val hasStoreDiscount = resolvedOffer.usesRequestedOffer &&
                        phases.size > 1 &&
                        standardPrice.isNotEmpty() && standardPrice != currentPrice
                    storeReadyPlanVersionCodes.add(planVersionCode)
                    item.priceText.set(currentPrice)
                    item.showDiscount.set(
                        hasStoreDiscount && (plan.discountPercent ?: 0) > 0,
                    )
                    item.originalPriceText.set(
                        standardPrice.takeIf { hasStoreDiscount }.orEmpty(),
                    )
                    if (item === currentRechargeItem) {
                        submitPrivileges(item)
                    }
                }
                updateBottomHint()
                updateSubscribeAvailability()
            }
        }
    }

    /**
     * 统一计算订阅按钮资格。
     *
     * action 仍是后端资格事实；Google Offer 就绪只证明商店参数可用，不能把 CURRENT、
     * PENDING_SWITCH 或 UNAVAILABLE 重新解释为可购买。SWITCH 还必须能从本次套餐列表中唯一
     * 找到当前订阅商品，否则无法安全构造 oldPurchaseToken 的匹配条件。
     */
    private fun updateSubscribeAvailability() {
        val plan = currentRechargeItem?.data?.get()
        val action = plan?.action
        val actionSupported = action == MemberActionEnum.PURCHASE.name ||
            action == MemberActionEnum.SWITCH.name
        val hasRequiredPlanFields = !plan?.planVersionCode.isNullOrBlank() &&
            !plan?.productId.isNullOrBlank() &&
            !plan?.basePlanId.isNullOrBlank()
        val hasCurrentPlanForSwitch = action != MemberActionEnum.SWITCH.name ||
            currentSubscriptionProductId != null
        val storeOfferReady = plan?.planVersionCode?.let(storeReadyPlanVersionCodes::contains) == true

        canSubscribe.set(
            memberPlansReady &&
                !memberPurchaseBusy &&
                !memberPurchaseBlocked &&
                actionSupported &&
                hasRequiredPlanFields &&
                hasCurrentPlanForSwitch &&
                storeOfferReady &&
                !MemberSubscriptionRecoveryStore.hasPendingForCurrentUser() &&
                BillingManager.canStartMemberSubscriptionPreparation()
        )
    }

    /**
     * 单笔会员购买的页面内上下文。
     *
     * 使用普通 class，避免 data class 自动生成的 toString 把 prepare token、账号绑定标识等
     * 支付参数展开到日志。进程存活时保留完整 prepare 上下文；进程重启只使用不含交易 token
     * 的 [MemberSubscriptionRecoveryStore] 标记重新查询 Google SUBS。
     */
    private class ActiveMemberPurchaseContext(
        val actionType: String,
        val periodUnit: String?,
        val purchasePrepareToken: String,
        val productId: String,
        val basePlanId: String,
        val requestedOfferId: String?,
        val obfuscatedAccountId: String,
        val googleReplacementMode: String?,
        val currentProductId: String?,
    ) {
        var billingLaunchStarted = false
        var officialPurchaseObserved = false
        var confirmAttempted = false
        var recoveryQueryOnResume = false
        var releaseRequested = false
        var analyticsResultReported = false
        // 微单位金额 例如 7_990_000  相当于 7.99
        var priceAmountMicros: Long? = null
        // 币种，例如 USD
        var priceCurrencyCode: String? = null

        /** 只确认本次商品及账号绑定标识都匹配的 Google Purchase。 */
        fun matches(purchase: Purchase): Boolean {
            return productId in purchase.products &&
                purchase.accountIdentifiers?.obfuscatedAccountId == obfuscatedAccountId
        }
    }

}
