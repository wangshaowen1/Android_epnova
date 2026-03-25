package com.kuaigeglobal.epnova.feature.play

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableLong
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo


class ItemVideoFeedVm(info: WorkInfo?) :
    AppItemViewModel<WorkInfo>(info) {

    companion object{
        const val LOG_TAG = "ItemVideoFeedVm"
    }

    /**
     * 是否播放中
     */
    val isPlaying by lazy { ObservableBoolean(false) }

    /**
     * 是否播放中
     */
    val isPaused by lazy { ObservableBoolean(false) }

    /**
     * 当前播放进度ms
     */
    val currentPosition by lazy { ObservableLong(0) }

    /**
     * 总时长ms
     */
    val duration by lazy { ObservableLong(0) }

    /**
     * 拖动进度滑块
     */
    val seeking by lazy { ObservableBoolean(false) }
}