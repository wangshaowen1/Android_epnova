package com.kuaigeglobal.epnova.feature.play.more

import androidx.lifecycle.MutableLiveData
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.RecyclerViewVM
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.core.AppRepository
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeRangeInfo

class EpisodeChooseViewModel : AppRepositoryViewModel<AppRepository>() {

    // 从Bundle获取参数
    val videoTitle: String? by lazy { mBundle.getString(Constants.Extra.TITLE) }
    val videoCover: String? by lazy { mBundle.getString(Constants.Extra.COVER) }
    val videoViews: String? by lazy { mBundle.getString(Constants.Extra.BROWSERS) }
    val totalEpisodes by lazy { mBundle.getInt(Constants.Extra.COUNT, 0) }
    val currentEpisode by lazy { mBundle.getInt(Constants.Extra.POSITION, 1) }

    companion object {
        const val RANGE_SIZE = 30  // 每个标签区间的大小
    }

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
        initEpisodes()
        initRangeTabs()
    }

    private fun initEpisodes() {
        for (i in 1..totalEpisodes) {
            val info = EpisodeInfo(sortNo = i)
            val vm = ItemEpisodeNumberVm(info).apply {
                if (i == currentEpisode) {
                    checked.set(true)
                    checkedEpisode = this
                }
                onItemClick.value = {
                    if (checkedEpisode != this) {
                        checkedEpisode?.checked?.set(false)
                        this.checked.set(true)
                        checkedEpisode = this
                        onEpisodeSelected.invoke(data.get())
                    }
                }
            }
            episodeVms.add(vm)
        }
        mEpisodeAdapterVm.notifyDataSetChanged()
    }

    private fun initRangeTabs() {
        val rangeCount = (totalEpisodes + RANGE_SIZE - 1) / RANGE_SIZE
        for (i in 0 until rangeCount) {
            val start = i * RANGE_SIZE + 1
            val end = minOf((i + 1) * RANGE_SIZE, totalEpisodes)
            val label = "$start - $end"
            val rangeInfo = EpisodeRangeInfo(
                label = label,
                startIndex = i * RANGE_SIZE,
                startEpisode = start,
                endEpisode = end
            )
            val vm = ItemEpisodeRangeVm(rangeInfo).apply {
                // 默认选中包含当前集数的标签
                if (currentEpisode in start..end) {
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
                    scrollToPosition.value = rangeInfo.startIndex
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
                checkedRange?.selected?.set(false)
                targetRange.selected.set(true)
                checkedRange = targetRange
            }
        }
    }
}