package com.kuaigeglobal.epnova.feature.series.management

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.paging.filter
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.common.ext.showConfirmPrompt
import com.kuaigeglobal.epnova.common.ext.startRefreshWithAdapter
import com.kuaigeglobal.epnova.common.ext.topActivity
import com.kuaigeglobal.epnova.core.Events
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.databinding.ActivityWorksManagementBinding
import com.kuaigeglobal.epnova.feature.series.EpisodeAdapter
import com.kuaigeglobal.epnova.feature.series.SeriesEpisodeEvent
import com.kuaigeglobal.epnova.feature.works.ItemMultipleChoiceEventsViewModel
import com.kuaigeglobal.epnova.widget.decoration.BottomSpaceDecoration
import com.kuaigeglobal.epnova.widget.stateful.paging.MyMultiStatePagingFooterAdapter
import com.nhsd.basic.BaseViewModelActivity
import com.nhsd.basic.bus.FlowEventBus
import com.nhsd.basic.ext.dp
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.launch
import kotlin.collections.containsAll
import kotlin.compareTo
import kotlin.toString

/**
 * *****************************************
 * 项目名称: FastFilm
 * @Author: SZYMN
 * @Introduce:作品管理
 * @Data: 2026/2/5/周四  20:17
 * @Version: 1.0
 * *****************************************
 */
class WorksManagementActivity :
    BaseViewModelActivity<ActivityWorksManagementBinding, WorksManagementViewModel>() {

    companion object {

        const val WORK_ID = "workId"

        fun start(activity: Activity?, workId: String? = null) {
            val activity = activity ?: topActivity ?: return
            val intent = Intent(activity, WorksManagementActivity::class.java)
            intent.putExtra(WORK_ID, workId)
            activity.startActivity(intent)
        }
    }

    private val eventVm by lazy { ItemMultipleChoiceEventsViewModel<SeriesEpisodeEvent, String>() }

    private val episodeAdapter by lazy { EpisodeAdapter(eventVm) }

    override fun onViewModelCreated() {
        super.onViewModelCreated()
        initView()
        //数据更新
        dataUpdate()
    }

    private fun initView() {
        val noDataButtonTextAs = getString(R.string.refresh)
        val noDataAs = getString(R.string.no_data)
        binding.recyclerView.apply {
            itemAnimator = null
            addItemDecoration(BottomSpaceDecoration(80.dp))
            adapter = episodeAdapter.withLoadStateFooter(
                MyMultiStatePagingFooterAdapter(
                    hostAdapter = episodeAdapter,
                    noData = noDataAs,
                    noDataButtonDisplay = false,
                    requestButtonDisplay = false,
                    noDataButtonText = noDataButtonTextAs,
                    noMoreThreshold = 1000,
                    refresh = { refresh() },
                    requestButtonTextAndImg = false,
                )
            )
        }
        binding.loadStateViewHolder.apply {
            setGravityHorizontalTop(100.dp)
            setRetryListener { refresh() }
            setNoDataButton(noDataButtonTextAs, { refresh() })
            setNoDataPrompt(noDataAs)
        }
        binding.swipeRefreshLayout.startRefreshWithAdapter(episodeAdapter)

        //设置编辑状态
        eventVm.switchStatus(false)
    }

    private fun dataUpdate() {
        lifecycleScope.launch {
            mRealVM.episodeList().collect {
                episodeAdapter.submitData(it.filter {
                    (it.stepProgress ?: 0) > EpisodeInfo.NOT_CREATED
                })
            }
        }

        mRealVM.deleteCompleted.observe(this) {
            if (!it) return@observe
            mRealVM.deleteCompleted.value = false
            eventVm.clearAllSelection()
            mRealVM.selectedCount.value = eventVm.getSelectedItems().size
            FlowEventBus.postSync(Events.DeleteTVSeriesEvent(mRealVM.workId))
            refresh()
        }

        mRealVM.selectedCount.observe(this) { count ->
            val currentIds = currentSelectableIds()

            // 判断是否全选
            val isAllSelected = currentIds.isNotEmpty() && count > 0 && eventVm.getSelectedItems().containsAll(currentIds)

            binding.btnSelectAll.text = if (isAllSelected) {
                getString(R.string.deselect_all)  // 反选
            } else {
                getString(R.string.check_all)     // 全选
            }

            if (count <= 0) {
                binding.tvSelectCount.text = getString(R.string.selected_items_format, "0")
                return@observe
            }

            val spannableString = SpannableString(
                getString(R.string.selected_items_format, count.toString())
            )
            spannableString.setSpan(
                ForegroundColorSpan(Color.WHITE),
                3,
                spannableString.length - 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            binding.tvSelectCount.text = spannableString
        }

        lifecycleScope.launch {
            eventVm.events.collect { event ->
                when (event) {
                    is SeriesEpisodeEvent.ComposeState -> {
                        if (event.status) {
                            mRealVM.selectedCount.value = mRealVM.selectedCount.value?.plus(1)
                        } else {
                            mRealVM.selectedCount.value = mRealVM.selectedCount.value?.minus(1)
                        }
                    }

                    else -> {}
                }
            }
        }
    }


    private fun refresh() {
        episodeAdapter.refresh()
    }

    private fun currentSelectableIds(): HashSet<String> {
        return episodeAdapter.snapshot().items
            .filterNot { it.isPublishedOrUnderReview() }
            .mapNotNull { it.storyId }
            .toHashSet()
    }

    override fun <Data> onClick(view: View?, data: Data?) {
        super.onClick(view, data)
        when (view?.id) {
            R.id.ll_delete -> {
                //删除
                if (eventVm.getSelectedItems().isEmpty()) {
                    return
                }
                topActivity?.showConfirmPrompt(
                    title = getString(R.string.delete_episode),
                    tip = getString(R.string.delete_episodes_confirm_warning),
                    cancelText = getString(R.string.cancel),
                    confirmText =  getString(R.string.del)
                ) {
                    mRealVM.delete(eventVm.getSelectedItems().toList())
                }
            }


            R.id.btnSelectAll -> {
                val currentIds = currentSelectableIds()
                if (currentIds.isEmpty()) {
                    showToast(R.string.delete_episode_published_or_review_toast)
                    return
                }
                val selectedItems = eventVm.getSelectedItems()

                // 判断是否已经全选
                val isAllSelected = currentIds.isNotEmpty() && selectedItems.containsAll(currentIds)

                if (isAllSelected) {
                    // 反选 -> 清空所有选中
                    eventVm.clearAllSelection()
                } else {
                    // 全选 -> 选中当前所有已加载的项
                    eventVm.setSelectedItems(currentIds)
                }
                mRealVM.selectedCount.value = eventVm.getSelectedItems().size
            }

            R.id.btnComplete -> {
                    finish()
            }
        }
    }
}
