package com.kuaigeglobal.epnova.feature.rewards

import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.LoadState
import com.kuaigeglobal.epnova.common.adapter.QuickAdapter
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.ext.request
import com.kuaigeglobal.epnova.core.network.apis.entity.AdTaskItem
import com.kuaigeglobal.epnova.core.network.apis.entity.CheckInTaskItem
import com.kuaigeglobal.epnova.core.network.apis.request.AdPreReportBody
import com.kuaigeglobal.epnova.core.network.apis.request.UserTokenBody
import com.kuaigeglobal.epnova.router.AppNavigator
import com.nhsd.basic.ext.showToast

class RewardsViewModel : AppRepositoryViewModel<AppRepository>() {

    val loadState    = ObservableField<LoadState>(LoadState.NotLoading(false))
    val balance      = ObservableInt(0)
    val checkInDays  = ObservableInt(0)
    val hasCheckedIn = ObservableField(false)

    val checkInAdapter = QuickAdapter<ItemCheckInDayVM>(R.layout.item_check_in_day, emptyList())
    val earnAdapter    = QuickAdapter<ItemEarnRewardVM>(R.layout.item_earn_reward,   emptyList())

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
     * 用户点击 Watch 广告前调用此方法获取 adFlowId。
     * 拿到 adFlowId 后由调用方将其设置为 AdMob 的 customRewardString，
     * 广告播放完毕后回调 [onAdWatched]。
     */
    fun preReportAd(vm: ItemEarnRewardVM, onFlowId: (adFlowId: String) -> Unit) {
        val recordId = vm.data.get()?.taskRecordId ?: return
        request(
            apiBlock = { mRepo.api.rewardAdPreReport(AdPreReportBody(adScene = 2, bizId = recordId)) },
            onSuccess = {
                val flowId = data?.nonce ?: return@request
                onFlowId(flowId)
            }
        )
    }

    /** 广告播放完毕后调用，标记任务完成并更新余额，然后刷新列表激活下一条 */
    fun onAdWatched(vm: ItemEarnRewardVM) {
        if (vm.isDone.get()) return
        vm.isDone.set(true)
        balance.set(balance.get() + vm.coins)
        loadAdTaskList()
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
                // 当前可观看：挂载广告前置上报 + 完成回调
                0 -> onWatchAction  = { preReportAd(this) { onAdWatched(this) } }
                // 未解锁：点击提示先完成上一条
                2 -> onLockedAction = { showToast(R.string.rewards_finish_previous_task) }
            }
        }
    }
}
