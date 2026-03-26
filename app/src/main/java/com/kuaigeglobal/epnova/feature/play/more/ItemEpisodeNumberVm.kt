package com.kuaigeglobal.epnova.feature.play.more

import androidx.databinding.ObservableBoolean
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo

class ItemEpisodeNumberVm(info: EpisodeInfo) : AppItemViewModel<EpisodeInfo>(info) {
    val checked = ObservableBoolean(false)
}