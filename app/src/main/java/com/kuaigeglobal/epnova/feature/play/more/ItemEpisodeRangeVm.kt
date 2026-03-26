package com.kuaigeglobal.epnova.feature.play.more

import androidx.databinding.ObservableBoolean
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeRangeInfo

class ItemEpisodeRangeVm(info: EpisodeRangeInfo) : AppItemViewModel<EpisodeRangeInfo>(info) {
    val selected = ObservableBoolean(false)
}