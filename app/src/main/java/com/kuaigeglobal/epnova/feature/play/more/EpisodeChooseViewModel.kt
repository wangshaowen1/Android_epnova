package com.kuaigeglobal.epnova.feature.play.more

import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.RecyclerViewVM
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.request.WorkIdBody
import com.kuaigeglobal.epnova.core.network.apis.response.EpisodeDirectoryData
import com.kuaigeglobal.epnova.core.response
import com.nhsd.basic.ext.getParcelableArrayListValue
import com.nhsd.basic.ext.getParcelableValue
import com.nhsd.basic.ext.showToast

class EpisodeChooseViewModel : AppRepositoryViewModel<AppRepository>() {

    val workId by lazy { mBundle.getString(Constants.Extra.WORK_ID) }
    val episodeInfos by lazy { mBundle.getParcelableArrayListValue(Constants.Extra.DATA, EpisodeInfo::class.java) }
    val currentEpisode by lazy { mBundle.getParcelableValue(Constants.Extra.CURRENT, EpisodeInfo::class.java) }

    companion object {
        const val RANGE_SIZE = 30  // 每个标签区间的大小
    }

    val episodeInfo = ObservableField<EpisodeDirectoryData>()

    // 当前选中的集数VM
    var checkedEpisode: ItemEpisodeNumberVm? = null

    var onEpisodeSelected: (EpisodeInfo?) -> Unit = {}

    // 集数列表
    val episodeVms = arrayListOf<ItemEpisodeNumberVm>()
    val mEpisodeAdapterVm =
        RecyclerViewVM().Builder()
            .setQuickAdapter(R.layout.item_episode_number, episodeVms)
            .build()

    // 标签列表
    val rangeVms = arrayListOf<ItemEpisodeRangeVm>()
    val mRangeAdapterVm =
        RecyclerViewVM().Builder()
            .setQuickAdapter(R.layout.item_episode_range_tab, rangeVms)
            .build()

    // 当前选中的标签
    var checkedRange: ItemEpisodeRangeVm? = null

    override fun onCreate() {
        super.onCreate()
        getHeaderInfo()
        initEpisodes()
    }

    private fun getHeaderInfo() {
        launchWithLoading {
            response(mRepo.api.episodeDirectoryInfo(WorkIdBody(workId))) {
                episodeInfo.set(data)
                initRangeTabs()
            }
        }
    }

    private fun initEpisodes() {
        // 找到第一个未解锁的付费集（sortNo 最小的 locked 集），不允许越集解锁
        val firstLockedSortNo = episodeInfos
            ?.filter { it.isLocked() }
            ?.minByOrNull { it.sortNo ?: Int.MAX_VALUE }
            ?.sortNo

        episodeInfos?.forEach { info ->
            val vm = ItemEpisodeNumberVm(info).apply {
                if (info.sortNo == currentEpisode?.sortNo) {
                    checked.set(true)
                    checkedEpisode = this
                }
                onItemClick.value = {
                    val episode = data.get()
                    // 有锁的情况下只能点击最前面那一个带锁的剧集，否则不让点并弹 toast
                    if (episode?.isLocked() == true && episode.sortNo != firstLockedSortNo) {
                        showToast(R.string.episode_unlock_previous_required)
                    } else {
                        if (checkedEpisode != this) {
                            checkedEpisode?.checked?.set(false)
                            this.checked.set(true)
                            checkedEpisode = this
                            onEpisodeSelected.invoke(data.get())
                        }
                    }
                }
            }
            episodeVms.add(vm)
        }
        mEpisodeAdapterVm.notifyDataSetChanged()
    }

    private fun initRangeTabs() {
        episodeInfo.get()?.segmentTabs?.forEachIndexed { index, tab ->
            val vm = ItemEpisodeRangeVm(tab).apply {
                val startSortNo = tab.startSortNo ?: 0
                val endSortNo = tab.endSortNo ?: 0
                // 默认选中包含当前集数的标签
                if (currentEpisode?.sortNo in (startSortNo..endSortNo)) {
                    selected.set(true)
                    checkedRange = this
                }
                onItemClick.value = {
                    if (checkedRange != this) {
                        checkedRange?.selected?.set(false)
                        this.selected.set(true)
                        checkedRange = this
                    }
                    // 滚动到对应位置 - 通过LiveData通知Dialog
                    val targetScrollPosition = index * RANGE_SIZE
                    scrollToPosition.value = targetScrollPosition
                }
            }
            rangeVms.add(vm)
        }
        mRangeAdapterVm.notifyDataSetChanged()
    }

    // 用于通知Dialog滚动RecyclerView
    val scrollToPosition = MutableLiveData<Int>()

    /**
     * 根据当前可见位置更新选中的标签
     */
    fun updateRangeTabByPosition(firstVisiblePosition: Int) {
        val rangeIndex = firstVisiblePosition / RANGE_SIZE
        if (rangeIndex in rangeVms.indices) {
            val targetRange = rangeVms[rangeIndex]
            if (checkedRange != targetRange) {
                // 这里必须用 UI 线程安全的方式更新
                checkedRange?.selected?.set(false)
                targetRange.selected.set(true)
                checkedRange = targetRange
            }
        }
    }
}