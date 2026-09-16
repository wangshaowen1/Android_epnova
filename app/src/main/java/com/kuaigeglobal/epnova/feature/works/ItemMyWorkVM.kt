package com.kuaigeglobal.epnova.feature.works

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.viewModelScope
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.item.AppItemViewModel
import com.kuaigeglobal.epnova.common.Constant
import com.kuaigeglobal.epnova.core.Config
import com.kuaigeglobal.epnova.core.network.apis.entity.CreatorWorkInfo
import com.nhsd.basic.ext.getResColor
import com.nhsd.basic.ext.getResString
import kotlinx.coroutines.launch

class ItemMyWorkVM(
    private val eventVm: ItemMultipleChoiceEventsViewModel<MyWorksEvent, String>,
    workInfo: CreatorWorkInfo?
) : AppItemViewModel<CreatorWorkInfo>(workInfo) {


    //当前显示状态是查看(true)还是编辑(false)
    var isView = ObservableBoolean(true)

    //是否选中
    val isSelected = ObservableBoolean(false)

    // 是否显示按钮
    val switchShow = ObservableBoolean((Config.get().appConfig.workListButtonSwitch ?: 0) == Constant.Switch.SHOW)

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

    @DrawableRes
    fun publishBadgeBackground(): Int {
        return data.get()?.publishBadgeBackground() ?: R.drawable.bg_60_000000_round
    }

    fun episodeProgressBias(finishedEpisodeCnt: Int?, totalEpisodeCnt: Int?): Float {
        val total = totalEpisodeCnt ?: return 0f
        if (total <= 0) return 0f
        return ((finishedEpisodeCnt ?: 0).toFloat() / total).coerceIn(0f, 1f)
    }

    fun showPublishProgress(): Boolean {
        data.get() ?: return false
        return (data.get()?.totalEpisodeCnt ?: 0) > (data.get()?.publishMinEpisodeCnt ?: 0)
    }

    fun publishTip(): CharSequence {
        data.get() ?: return ""
        var tipText: CharSequence = ""
        data.get()?.let { workInfo ->
            // 创作集数 == 总剧集数
            if (workInfo.creationProgressEpisodeCnt == workInfo.totalEpisodeCnt) {
                tipText = R.string.my_creations_publish_tip_over.getResString()
            } else if (
                // 总集数＞发行集数（5集） 且 发行集数（5集）＜=创作剧集数＜总集数
                ((workInfo.totalEpisodeCnt ?: 0) > (workInfo.publishMinEpisodeCnt ?: 0)) &&
                (workInfo.creationProgressEpisodeCnt ?: 0) in ((workInfo.publishMinEpisodeCnt ?: 0) until (workInfo.totalEpisodeCnt ?: 0))
            ) {
                tipText = R.string.my_creations_publish_tip_3.getResString()
            } else if (
                // 总集数＜=发行集数（5集）且 创作集数＜总集数
                ((workInfo.totalEpisodeCnt ?: 0) <= (workInfo.publishMinEpisodeCnt ?: 0)) &&
                ((workInfo.creationProgressEpisodeCnt ?: 0) < (workInfo.totalEpisodeCnt ?: 0))
            ) {
                tipText = R.string.my_creations_publish_tip_2.getResString()
            } else if (
                // 作品必须满足总集数＞发行集数（5集） 且 作品创作集数＜发行集数（5集）
                ((workInfo.totalEpisodeCnt ?: 0) > (workInfo.publishMinEpisodeCnt ?: 0)) &&
                ((workInfo.creationProgressEpisodeCnt ?: 0) < (workInfo.publishMinEpisodeCnt ?: 0))
            ) {
                val remainingEpisodeCount =
                    (workInfo.publishMinEpisodeCnt ?: 0) -
                        (workInfo.creationProgressEpisodeCnt ?: 0)
                tipText = buildPublishTip(remainingEpisodeCount)
            }
        }
        return tipText
    }

    private fun buildPublishTip(remainingEpisodeCount: Int): CharSequence {
        val highlightText =
            R.string.my_creations_publish_tip_highlight.getResString(remainingEpisodeCount)
        val publishTip = R.string.my_creations_publish_tip.getResString(highlightText)
        val highlightStart = publishTip.indexOf(highlightText)
        if (highlightStart < 0) return publishTip

        return SpannableString(publishTip).apply {
            setSpan(
                ForegroundColorSpan(R.color.color_ffce2c.getResColor()),
                highlightStart,
                highlightStart + highlightText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    fun onClick() {
        handleClick(MyWorksEvent.ViewDetails.Target.ITEM)
    }

    fun onPublishTipClick() {
        handleClick(MyWorksEvent.ViewDetails.Target.PUBLISH_TIP)
    }

    fun onAllListClick() {
        handleClick(MyWorksEvent.ViewDetails.Target.ALL_LIST)
    }

    fun onContinueCreatingClick() {
        handleClick(MyWorksEvent.ViewDetails.Target.CONTINUE_CREATING)
    }

    private fun handleClick(target: MyWorksEvent.ViewDetails.Target) {
        val info = data.get()
        if (isView.get()) {
            eventVm.postEvent(MyWorksEvent.ViewDetails(info, target))
            return
        }
        if (isSelected.get()) {
            isSelected.set(false)
            eventVm.removeSelectedItem(info?.workId?:"")
        } else {
            isSelected.set(true)
            eventVm.addSelectedItem(info?.workId?:"")
        }
        eventVm.postEvent(MyWorksEvent.ComposeState(isSelected.get(), info))
    }


    init {
        viewModelScope.launch {
            eventVm.switchStatus.collect {
                isView.set(it)
            }
        }

        viewModelScope.launch {
            eventVm.selectedItemIds.collect { selectedIds ->
                isSelected.set(selectedIds.contains(workInfo?.workId))
            }
        }
    }
}
