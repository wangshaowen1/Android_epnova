package com.kuaigeglobal.epnova.feature.rewards

import androidx.databinding.ObservableBoolean
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.entity.AdTaskItem

class ItemEarnRewardVM(data: AdTaskItem?) : AppItemViewModel<AdTaskItem>(data) {

    val isDone   = ObservableBoolean(data?.taskStatus == 1)
    val isWatch  = ObservableBoolean(data?.taskStatus != 2)
    val isLocked = ObservableBoolean(data?.taskStatus == 2)

    val title: String = "Watch Ad"
    val desc: String  = "+ ${data?.rewardNum ?: 0} Reward coins"
    val coins: Int    = data?.rewardNum ?: 0

    var onWatchAction:  (() -> Unit)? = null
    var onLockedAction: (() -> Unit)? = null

    fun onWatch() {
        if (isLocked.get()) onLockedAction?.invoke()
        else onWatchAction?.invoke()
    }
}
