package com.kuaigeglobal.epnova.feature.rewards

import androidx.databinding.ObservableBoolean
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.network.apis.entity.AdTaskItem
import com.nhsd.basic.ext.getResString

class ItemEarnRewardVM(
    data: AdTaskItem?,
    val isLoginTask: Boolean = false
) : AppItemViewModel<AdTaskItem>(data) {

    private val rewardNum: Long = if (isLoginTask) 50L else (data?.rewardNum ?: 0L)

    /**
     * 任务状态：
     * 0 - 待完成（Watch 可点击）
     * 1 - 已完成/Claimed（置灰）
     * 2 - 未解锁（置灰，未完成前序任务）
     *
     * 登录任务：用户已登录视为已完成
     */
    val isDone = ObservableBoolean(if (isLoginTask) !User.get().isVisitor else data?.taskStatus == 1)
    val isWatch = ObservableBoolean(!isLoginTask && data?.taskStatus == 0)
    val isLocked = ObservableBoolean(!isLoginTask && data?.taskStatus == 2)

    val iconRes: Int = if (isLoginTask) R.drawable.ic_link_circle else R.drawable.ic_video_circle
    val title: String = if (isLoginTask)
        R.string.rewards_login_any_account.getResString()
    else
        R.string.rewards_watch_ad.getResString()

    val coinsTxt: String = "+$rewardNum"
    val coins: Long = rewardNum

    /** 副标题全文，用于 data binding 的 titleText 属性 */
    val subtitleText: String = if (isLoginTask)
        topActivity?.getString(R.string.rewards_50_reward_coins, rewardNum.toString()) ?: ""
    else
        topActivity?.getString(R.string.rewards_watch_video_earn_50, rewardNum.toString()) ?: ""

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
