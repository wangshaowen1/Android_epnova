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

    private fun loadAdTaskList() {
        request(
            apiBlock = { mRepo.api.rewardAdTaskList(UserTokenBody()) },
            onSuccess = {
                val d = data ?: return@request
                rewardsResetTime.set(formatNextRefreshText(data?.nextRefreshTime))
                val adItems = d.taskList?.map { buildEarnItemVM(it) } ?: emptyList()
                if (User.get().isLogin) {
                    earnAdapter.submitList(adItems)
                    return@request
                }
                // 登录任务固定排在首位，接口只返回广告任务
                earnAdapter.submitList(listOf(buildLoginTaskVM()) + adItems)
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

    private fun buildEarnItemVM(item: AdTaskItem): ItemEarnRewardVM {
        return ItemEarnRewardVM(item).apply {
            when (item.taskStatus) {
                0 -> onWatchAction  = { prepareTaskAd(this) }
                2 -> onLockedAction = { showToast(R.string.rewards_finish_previous_task) }
            }
        }
    }
}
