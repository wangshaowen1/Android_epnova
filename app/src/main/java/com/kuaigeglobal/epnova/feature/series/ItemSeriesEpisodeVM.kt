package com.kuaigeglobal.epnova.feature.series

import android.annotation.SuppressLint
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.viewModelScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorEpisodeInfo
import com.kuaigeglobal.epnova.feature.works.ItemMultipleChoiceEventsViewModel
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.launch
import kotlin.text.orEmpty

/**
 * 剧集-item
 */
class ItemSeriesEpisodeVM(
    private val eventVm: ItemMultipleChoiceEventsViewModel<SeriesEpisodeEvent, String>,
    val position: Int,
    private val episodeInfo: CreatorEpisodeInfo?,
) : AppItemViewModel<CreatorEpisodeInfo>(episodeInfo) {

    //当前显示状态是查看(true)还是编辑(false)
    var isView = ObservableBoolean(true)

    //是否选中
    val isSelected = ObservableBoolean(false)

    fun onClick() {
        if (isView.get()) {
            eventVm.postEvent(SeriesEpisodeEvent.Click(position, this))
            return
        }
        if (isDeleteLocked()) {
            showToast(R.string.drama_ep_delete_forbidden_review)
            return
        }
        if (isSelected.get()) {
            isSelected.set(false)
            eventVm.removeSelectedItem(data.get()?.storyId ?: "")
        } else {
            isSelected.set(true)
            eventVm.addSelectedItem(data.get()?.storyId ?: "")
        }
        eventVm.postEvent(SeriesEpisodeEvent.ComposeState(isSelected.get(), this))
    }

    fun isDeleteLocked(): Boolean {
        return data.get()?.isPublishedOrUnderReview() == true
    }

    fun hasPublishBadge(): Boolean {
        return !data.get()?.publishBadgeText().isNullOrEmpty()
    }

    fun publishBadgeText(): String {
        return data.get()?.publishBadgeText().orEmpty()
    }

    @ColorRes
    fun publishBadgeTextColor(): Int {
        return data.get()?.publishBadgeTextColor() ?: R.color.fixed_white_1
    }

    @ColorRes
    fun episodeStatusTextColor(): Int {
        return data.get()?.episodeStatusTextColor() ?: R.color.fixed_white_1
    }

    fun episodeStatusText(): String {
        return data.get()?.creationStageText.orEmpty()
    }

    @DrawableRes
    fun publishBadgeBackground(): Int {
        return data.get()?.publishBadgeBackground() ?: R.drawable.bg_60_000000_round
    }

    init {
        viewModelScope.launch {
            eventVm.switchStatus.collect {
                isView.set(it)
            }
        }

        viewModelScope.launch {
            eventVm.selectedItemIds.collect { selectedIds ->
                isSelected.set(selectedIds.contains(episodeInfo?.storyId))
            }
        }
    }

    @SuppressLint("DefaultLocale")
    fun formatDuration(): String {
        val millis=data.get()?.duration?:0L
        if (millis <= 0L) return "0"
        val minutes = millis / 60
        val seconds = millis % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
