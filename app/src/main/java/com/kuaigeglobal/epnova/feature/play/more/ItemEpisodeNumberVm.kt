package com.kuaigeglobal.epnova.feature.play.more

import androidx.databinding.ObservableBoolean
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo

class ItemEpisodeNumberVm(info: EpisodeInfo) : AppItemViewModel<EpisodeInfo>(info) {
    val checked = ObservableBoolean(false)

    /** 未解锁付费集 */
    val locked: Boolean get() = data.get()?.lockStatus == 2

    /** 已解锁付费集 */
    val unlockedPaid: Boolean get() = data.get()?.lockStatus == 1
}