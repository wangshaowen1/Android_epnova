package com.kuaigeglobal.epnova.feature.rewards

import androidx.databinding.ObservableBoolean
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.network.apis.entity.AdTaskItem
import com.kuaigeglobal.epnova.core.network.apis.entity.OnceTaskItem
import com.nhsd.basic.ext.getResString

class ItemEarnRewardVM(
    data: AdTaskItem?,
    val isLoginTask: Boolean = false,
    val onceTask: OnceTaskItem? = null,
) : AppItemViewModel<AdTaskItem>(data) {

    private val isOnceTask: Boolean = onceTask != null
    private val rewardLabel: String = when {
        isLoginTask -> "50"
        isOnceTask  -> onceTask?.rewardLabel?.takeIf { it.isNotBlank() } ?: onceTask?.rewardNum?.toString().orEmpty()
        else        -> data?.rewardNum?.toString().orEmpty()
    }.trim().removePrefix("+").trim()
    private val rewardNum: Long = rewardLabel.toLongOrNull() ?: when {
        isLoginTask -> 50L
        isOnceTask  -> onceTask?.rewardNum ?: 0L
        else        -> data?.rewardNum ?: 0L
    }

    /**
     * 任务状态：
     * 0 - 待完成（Watch 可点击）
     * 1 - 已完成/Claimed（置灰）
     * 2 - 未解锁（置灰，未完成前序任务）
     *
     * 登录任务：用户已登录视为已完成。
     * 一次性任务：服务端已通过 showFlag 过滤掉已领取任务，能展示时默认就是可领取状态。
     */
    val isDone = ObservableBoolean(if (isLoginTask) !User.get().isVisitor else !isOnceTask && data?.taskStatus == 1)
    val isWatch = ObservableBoolean(!isLoginTask && !isOnceTask && data?.taskStatus == 0)
    val isLocked = ObservableBoolean(!isLoginTask && !isOnceTask && data?.taskStatus == 2)

    val iconUrl: String? = onceTask?.iconUrl?.takeIf { it.isNotBlank() } ?: data?.iconUrl?.takeIf { it.isNotBlank() }
    val hasRemoteIcon: Boolean = !iconUrl.isNullOrEmpty()
    val iconRes: Int = when {
        isLoginTask -> R.drawable.ic_link_circle
        isOnceTask  -> R.drawable.ic_notification_circle
        else        -> R.drawable.ic_video_circle
    }
    val title: String = when {
        isLoginTask -> R.string.rewards_login_any_account.getResString()
        isOnceTask -> onceTask?.taskName ?: ""
        else        -> R.string.rewards_watch_ad.getResString()
    }

    val coinsTxt: String = "+$rewardLabel"
    val coins: Long = rewardNum

    /** 副标题全文，用于 data binding 的 titleText 属性 */
    val subtitleText: String = when {
        // 一次性任务接口的 taskDesc 只作为服务端描述，不直接落 UI；
        // 奖励文案统一走客户端多语言模板，避免出现“+ 150 Reward coins”等格式不一致的问题。
        isOnceTask  -> topActivity?.getString(R.string.rewards_50_reward_coins, rewardLabel) ?: ""
        isLoginTask -> topActivity?.getString(R.string.rewards_50_reward_coins, rewardNum.toString()) ?: ""
        else        -> topActivity?.getString(R.string.rewards_watch_video_earn_50, rewardNum.toString()) ?: ""
    }

    var onWatchAction: (() -> Unit)? = null
    var onLockedAction: (() -> Unit)? = null
    var onCheckInAction: (() -> Unit)? = null

    fun onCheckIn() {
        onCheckInAction?.invoke()
    }

    fun onWatch() {
        if (isLocked.get()) onLockedAction?.invoke()
        else onWatchAction?.invoke()
    }
}
