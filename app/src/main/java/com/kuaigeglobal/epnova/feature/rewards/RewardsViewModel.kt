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
import com.kuaigeglobal.epnova.core.network.apis.entity.AdTaskItem
import com.kuaigeglobal.epnova.core.network.apis.request.AdPreReportBody
import com.kuaigeglobal.epnova.core.network.apis.request.RewardAdPollBody
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import com.kuaigeglobal.epnova.router.AppNavigator
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

    val loadState    = ObservableField<LoadState>(LoadState.NotLoading(false))
    val balance      = ObservableLong(0)
    val checkInDays  = ObservableInt(0)
    val hasCheckedIn = ObservableField(false)

    val checkInAdapter = QuickAdapter<ItemCheckInDayVM>(R.layout.item_check_in_day, emptyList())
    val earnAdapter    = QuickAdapter<ItemEarnRewardVM>(R.layout.item_earn_reward,   emptyList())

    /** Fragment 收到后负责加载并展示 AdMob 激励广告 */
    data class AdWatchContext(val vm: ItemEarnRewardVM, val customData: String, val nonce: String)
    private val _showAdEvent = MutableSharedFlow<AdWatchContext>(extraBufferCapacity = 1)
    val showAdEvent: SharedFlow<AdWatchContext> = _showAdEvent

    override fun onCreate() {
        super.onCreate()
        balance.set(User.get().coins)
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
                loadCheckInList(updateLoadState = false)
            }
        )
    }

    // ─── 观看广告任务列表 ────────────────────────────────────────────────────────

    private fun loadAdTaskList() {
        request(
            apiBlock = { mRepo.api.rewardAdTaskList(UserTokenBody()) },
            onSuccess = {
                val d = data ?: return@request
                val adItems = d.taskList?.map { buildEarnItemVM(it) } ?: emptyList()
                // 登录任务固定排在首位，接口只返回广告任务
                earnAdapter.submitList(listOf(buildLoginTaskVM()) + adItems)
            }
        )
    }

    // ─── 广告观看前置上报 ────────────────────────────────────────────────────────

    /**
     * 用户点击 Watch 时调用：向服务端预报获取 customData（SSV 签名）和 nonce。
     * 成功后通过 [showAdEvent] 通知 Fragment 加载并展示广告。
     */
    fun preReportAd(vm: ItemEarnRewardVM) {
        val recordId = vm.data.get()?.taskRecordId ?: return
        request(
            apiBlock = { mRepo.api.rewardAdPreReport(AdPreReportBody(adScene = 2, bizId = recordId)) },
            onSuccess = {
                val d = data ?: return@request
                val customData = d.customData ?: return@request
                val nonce = d.nonce ?: return@request
                _showAdEvent.tryEmit(AdWatchContext(vm, customData, nonce))
            }
        )
    }

    /**
     * 广告播放完毕（用户完成激励）后由 Fragment 调用。
     * 轮询 [rewardAdPoll] 确认服务端 SSV 回调已处理（rewardStatus：0=处理中，1=成功，2=失败）。
     */
    fun onAdWatched(vm: ItemEarnRewardVM, nonce: String) {
        if (vm.isDone.get()) return
        viewModelScope.launch {
            repeat(POLL_MAX_RETRY) { attempt ->
                val res = runCatching {
                    withContext(Dispatchers.IO) { mRepo.api.rewardAdPoll(RewardAdPollBody(adFlowId = nonce)) }
                }.getOrNull()
                when (res?.data?.rewardStatus) {
                    1 -> {
                        vm.isDone.set(true)
                        vm.isWatch.set(false)
                        balance.set(balance.get() + vm.coins)
                        User.get().storeCoins(balance.get())
                        loadAdTaskList()
                        return@launch
                    }
                    2 -> {
                        loadAdTaskList()
                        return@launch
                    }
                }
                if (attempt < POLL_MAX_RETRY - 1) delay(POLL_INTERVAL_MS)
            }
            // 超时兜底：静默刷新列表
            loadAdTaskList()
        }
    }

    // ─── 内部辅助 ────────────────────────────────────────────────────────────

    /** 构建登录任务 VM，点击 Claim 打开登录界面 */
    private fun buildLoginTaskVM(): ItemEarnRewardVM {
        return ItemEarnRewardVM(data = null, isLoginTask = true).apply {
            onCheckInAction = { AppNavigator.checkRealLogin(showToast = false) }
        }
    }

    private fun buildEarnItemVM(item: AdTaskItem): ItemEarnRewardVM {
        return ItemEarnRewardVM(item).apply {
            when (item.taskStatus) {
                // 当前可观看：前置上报获取 SSV customData，再由 Fragment 加载并展示广告
                0 -> onWatchAction  = { preReportAd(this) }
                // 未解锁：点击提示先完成上一条
                2 -> onLockedAction = { showToast(R.string.rewards_finish_previous_task) }
            }
        }
    }
}
