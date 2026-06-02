package com.kuaigeglobal.epnova.feature.rewards

import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.databinding.ObservableLong
import androidx.lifecycle.viewModelScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.LoadState
import com.kuaigeglobal.epnova.common.adapter.QuickAdapter
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.request
import com.kuaigeglobal.epnova.core.manager.RewardedAdManager
import com.kuaigeglobal.epnova.core.network.apis.entity.AdTaskItem
import com.kuaigeglobal.epnova.core.network.apis.entity.OnceTaskItem
import com.kuaigeglobal.epnova.core.network.apis.request.RewardAdCompleteBody
import com.kuaigeglobal.epnova.core.network.apis.request.RewardAdTaskPrepareBody
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import com.kuaigeglobal.epnova.router.AppNavigator
import com.nhsd.basic.ext.getResString
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RewardsViewModel : AppRepositoryViewModel<AppRepository>() {

    companion object {
        private const val POLL_MAX_RETRY   = 6
        private const val POLL_INTERVAL_MS = 2_000L
    }

    val loadState        = ObservableField<LoadState>(LoadState.NotLoading(false))
    val balance          = ObservableLong(0)
    val checkInDays      = ObservableInt(0)
    val hasCheckedIn     = ObservableField(false)
    val todayAdWatched   = ObservableField(false)
    val nextRefreshText  = ObservableField("")
    val rewardsResetTime = ObservableField("")
    val adRewardCoins    = ObservableInt(0)

    val checkInAdapter = QuickAdapter<ItemCheckInDayVM>(R.layout.item_check_in_day, emptyList())
    val earnAdapter    = QuickAdapter<ItemEarnRewardVM>(R.layout.item_earn_reward,   emptyList())

    val adManager: RewardedAdManager by lazy { RewardedAdManager(mRepo.api, viewModelScope) }

    private var onceTaskItems: List<OnceTaskItem> = emptyList()
    private var adTaskItems: List<AdTaskItem> = emptyList()
    private var pendingPushNotificationTask: OnceTaskItem? = null

    /**
     * Activity 收到后负责加载并展示激励广告。
     * vm == null 表示签到广告（adScene=1）；否则为任务广告（adScene=2）。
     */
    data class AdWatchContext(
        val vm: ItemEarnRewardVM?,
        val nonce: String
    )
    private val _showAdEvent = MutableSharedFlow<AdWatchContext>(extraBufferCapacity = 1)
    val showAdEvent: SharedFlow<AdWatchContext> = _showAdEvent

    /** 签到成功事件，携带本次签到获得的金币数量和广告额外奖励金币数量 */
    private val _showCheckInSuccessEvent = MutableSharedFlow<Pair<Int, Int>>(extraBufferCapacity = 1)
    val showCheckInSuccessEvent: SharedFlow<Pair<Int, Int>> = _showCheckInSuccessEvent

    /** 签到广告播完、服务端确认奖励后通知 Fragment 展示获得金币的 Toast，携带实际奖励金币数量 */
    private val _showCheckInAdRewardEvent = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val showCheckInAdRewardEvent: SharedFlow<Int> = _showCheckInAdRewardEvent

    /** 通知任务需要 Activity 判断系统通知权限；ViewModel 不直接持有界面 Context。 */
    private val _checkPushNotificationEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val checkPushNotificationEvent: SharedFlow<Unit> = _checkPushNotificationEvent

    /** 通知任务未开启权限时，由 Activity 跳转系统通知设置页。 */
    private val _openNotificationSettingEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val openNotificationSettingEvent: SharedFlow<Unit> = _openNotificationSettingEvent

    /** 通知任务领奖成功后通知 Activity 复用现有金币 toast。 */
    private val _showPushNotificationRewardEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val showPushNotificationRewardEvent: SharedFlow<String> = _showPushNotificationRewardEvent

    override fun onResume() {
        super.onResume()
        balance.set(User.get().coins)
    }

    override fun onCleared() {
        super.onCleared()
        adManager.release()
    }

    fun loadData() {
        loadState.set(LoadState.Loading)
        loadCheckInList(updateLoadState = true)
        loadOnceTaskList()
        loadAdTaskList()
    }

    // ─── 签到任务列表 ───────────────────────────────────────────────────────────

    private fun loadCheckInList(updateLoadState: Boolean = false) {
        request(
            apiBlock = { mRepo.api.rewardCheckInList(UserTokenBody()) },
            onSuccess = {
                val d = data ?: return@request
                checkInDays.set(d.checkinDays ?: 0)
                hasCheckedIn.set((d.todayCheckedIn ?: 0) == 1)
                todayAdWatched.set((d.todayAdWatched ?: 0) == 1)
                adRewardCoins.set(d.adRewardCoins ?: 0)
                nextRefreshText.set(formatNextRefreshText(d.nextRefreshTime))
                checkInAdapter.submitList(d.taskList?.map { ItemCheckInDayVM(it) } ?: emptyList())
                if (updateLoadState) loadState.set(LoadState.NotLoading(true))
            }
        )
    }

    // ─── 签到上报 ───────────────────────────────────────────────────────────────

    fun doCheckIn() {
        if (hasCheckedIn.get() == true) return
        request(
            apiBlock = { mRepo.api.rewardCheckInReport(UserTokenBody()) },
            onSuccess = {
                val d = data ?: return@request
                checkInDays.set(d.checkinDays ?: 0)
                hasCheckedIn.set(true)
                balance.set(balance.get() + (d.rewardNum ?: 0))
                User.get().storeCoins(balance.get())
                loadCheckInList(updateLoadState = false)
                _showCheckInSuccessEvent.tryEmit(Pair(d.rewardNum ?: 0, adRewardCoins.get()))
            }
        )
    }

    // ─── 观看广告任务列表 ────────────────────────────────────────────────────────

    private fun loadOnceTaskList() {
        request(
            apiBlock = { mRepo.api.onceTaskList() },
            onSuccess = {
                // 服务端用 showFlag 控制是否展示：已领取的通知任务会返回 0，客户端直接过滤。
                onceTaskItems = data?.taskList?.filter { it.showFlag == 1 } ?: emptyList()
                submitEarnRewardList()
            }
        )
    }

    private fun loadAdTaskList() {
        request(
            apiBlock = { mRepo.api.rewardAdTaskList(UserTokenBody()) },
            onSuccess = {
                val d = data ?: return@request
                rewardsResetTime.set(formatNextRefreshText(data?.nextRefreshTime))
                adTaskItems = d.taskList ?: emptyList()
                submitEarnRewardList()
            }
        )
    }

    // ─── 广告任务-激励视频广告前置准备 ────────────────────────────────────────

    /**
     * 用户点击任务 Watch 时调用：prepare 获取 nonce，成功后通知 Activity 加载广告。
     */
    fun prepareTaskAd(vm: ItemEarnRewardVM) {
        val recordId = vm.data.get()?.taskRecordId ?: return
        request(
            apiBlock = { mRepo.api.rewardAdTaskPrepare(RewardAdTaskPrepareBody(taskRecordId = recordId)) },
            onSuccess = {
                val d = data ?: return@request
                val nonce = d.nonce ?: return@request
                _showAdEvent.tryEmit(
                    AdWatchContext(
                        vm = vm,
                        nonce = nonce
                    )
                )
            }
        )
    }

    /**
     * 任务广告播完后由 Activity 调用。
     * 重复调用 complete 确认结果，adFlowStatus：0=处理中，1=成功，2=失败。
     */
    fun onAdWatched(vm: ItemEarnRewardVM, nonce: String) {
        if (vm.isDone.get()) return
        viewModelScope.launch {
            repeat(POLL_MAX_RETRY) { attempt ->
                val res = runCatching {
                    withContext(Dispatchers.IO) {
                        mRepo.api.rewardAdTaskComplete(RewardAdCompleteBody(nonce = nonce))
                    }
                }.getOrNull()
                when (res?.data?.adFlowStatus) {
                    1 -> {
                        vm.isDone.set(true)
                        vm.isWatch.set(false)
                        balance.set(balance.get() + vm.coins)
                        User.get().storeCoins(balance.get())
                        loadAdTaskList()
                        return@launch
                    }
                    2 -> {
                        showToast(R.string.network_error)
                        loadAdTaskList()
                        return@launch
                    }
                }
                if (attempt < POLL_MAX_RETRY - 1) delay(POLL_INTERVAL_MS)
            }
            showToast(R.string.network_error)
            loadAdTaskList()
        }
    }

    // ─── 签到任务-激励视频广告前置准备 ────────────────────────────────────────

    /**
     * 用户在签到成功弹窗中点击"看广告领额外奖励"时调用。
     */
    fun prepareCheckInAd() {
        request(
            apiBlock = { mRepo.api.rewardAdCheckInPrepare(UserTokenBody()) },
            onSuccess = {
                val d = data ?: return@request
                val nonce = d.nonce ?: return@request
                _showAdEvent.tryEmit(
                    AdWatchContext(
                        vm = null,
                        nonce = nonce
                    )
                )
            }
        )
    }

    /**
     * 签到广告播完后由 Activity 调用。
     * 重复确认后更新余额、标记当天广告已看、通知 Activity 展示获得金币 Toast。
     */
    fun onCheckInAdWatched(nonce: String) {
        viewModelScope.launch {
            repeat(POLL_MAX_RETRY) { attempt ->
                val res = runCatching {
                    withContext(Dispatchers.IO) {
                        mRepo.api.rewardAdCheckInComplete(RewardAdCompleteBody(nonce = nonce))
                    }
                }.getOrNull()
                when (res?.data?.adFlowStatus) {
                    1 -> {
                        todayAdWatched.set(true)
                        val coins = adRewardCoins.get()
                        balance.set(balance.get() + coins)
                        User.get().storeCoins(balance.get())
                        _showCheckInAdRewardEvent.tryEmit(coins)
                        return@launch
                    }
                    2 -> {
                        showToast(R.string.network_error)
                        return@launch
                    }
                }
                if (attempt < POLL_MAX_RETRY - 1) delay(POLL_INTERVAL_MS)
            }
            showToast(R.string.network_error)
        }
    }

    // ─── 一次性任务-打开消息通知 ────────────────────────────────────────────────

    /**
     * 用户点击通知任务 Claim 时调用。
     *
     * FCM token 注册上报是独立链路，不和这个任务绑定；这里仅负责判断通知权限并触发奖励接口。
     */
    private fun preparePushNotificationTask(vm: ItemEarnRewardVM) {
        pendingPushNotificationTask = vm.onceTask ?: return
        _checkPushNotificationEvent.tryEmit(Unit)
    }

    /**
     * Activity 完成通知权限判断后回传结果。
     * 已开启则立即领奖，未开启则跳系统通知设置页。
     */
    fun onPushNotificationPermissionChecked(enabled: Boolean) {
        if (pendingPushNotificationTask == null) return
        if (enabled) {
            reportPendingPushNotificationTask()
        } else {
            _openNotificationSettingEvent.tryEmit(Unit)
        }
    }

    /**
     * 从系统通知设置页返回后回传结果。
     * 用户仍未开启时清掉 pending，下一次点击 Claim 再重新开始流程。
     */
    fun onPushNotificationSettingReturned(enabled: Boolean) {
        if (pendingPushNotificationTask == null) return
        if (enabled) {
            reportPendingPushNotificationTask()
        } else {
            pendingPushNotificationTask = null
        }
    }

    private fun reportPendingPushNotificationTask() {
        val task = pendingPushNotificationTask ?: return
        pendingPushNotificationTask = null
        request(
            apiBlock = { mRepo.api.reportPushNotificationTask() },
            onSuccess = {
                val rewardLabel = data?.rewardLabel?.takeIf { it.isNotBlank() }
                    ?: task.rewardLabel?.takeIf { it.isNotBlank() }
                    ?: task.rewardNum?.toString()
                    ?: "0"
                val rewardCoins = rewardLabel.toLongOrNull() ?: task.rewardNum ?: 0L
                if (rewardCoins > 0L) {
                    balance.set(balance.get() + rewardCoins)
                    User.get().storeCoins(balance.get())
                }
                _showPushNotificationRewardEvent.tryEmit(rewardLabel)
                loadOnceTaskList()
            }
        )
    }

    // ─── 内部辅助 ────────────────────────────────────────────────────────────

    /** 将服务端返回的毫秒时间戳格式化为 "Resets at HH:mm" */
    private fun formatNextRefreshText(timestampMs: Long?): String {
        if (timestampMs == null || timestampMs <= 0) return ""
        val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(timestampMs))
        return timeStr
    }

    private fun buildLoginTaskVM(): ItemEarnRewardVM {
        return ItemEarnRewardVM(data = null, isLoginTask = true).apply {
            onCheckInAction = { AppNavigator.checkRealLogin(showToast = false) }
        }
    }

    private fun buildOnceTaskVM(item: OnceTaskItem): ItemEarnRewardVM {
        return ItemEarnRewardVM(data = null, onceTask = item).apply {
            onCheckInAction = { preparePushNotificationTask(this) }
        }
    }

    private fun buildEarnItemVM(item: AdTaskItem): ItemEarnRewardVM {
        return ItemEarnRewardVM(item).apply {
            when (item.taskStatus) {
                0 -> onWatchAction  = { prepareTaskAd(this) }
                2 -> onLockedAction = { showToast(R.string.rewards_finish_previous_task) }
            }
        }
    }

    private fun submitEarnRewardList() {
        val onceItems = onceTaskItems.map { buildOnceTaskVM(it) }
        val adItems = adTaskItems.map { buildEarnItemVM(it) }
        if (User.get().isLogin) {
            earnAdapter.submitList(onceItems + adItems)
            return
        }
        // 登录任务是本地任务；一次性通知任务由 /reward/onceTask/list 控制展示，优先排在最前面。
        earnAdapter.submitList(onceItems + listOf(buildLoginTaskVM()) + adItems)
    }
}
