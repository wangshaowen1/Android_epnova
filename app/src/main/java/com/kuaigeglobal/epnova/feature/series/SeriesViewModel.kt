package com.kuaigeglobal.epnova.feature.series

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kuaigeglobal.epnova.R
import com.kuaigeglobal.epnova.base.viewmodel.AppRepositoryViewModel
import com.kuaigeglobal.epnova.common.Constants
import com.kuaigeglobal.epnova.common.PagedProxy
import com.kuaigeglobal.epnova.common.adapter.QuickAdapter
import com.kuaigeglobal.epnova.core.User
import com.kuaigeglobal.epnova.core.ext.launch
import com.kuaigeglobal.epnova.core.ext.launchWithLoading
import com.kuaigeglobal.epnova.core.network.apis.entity.EpisodeInfo
import com.kuaigeglobal.epnova.core.network.apis.entity.WorkInfo
import com.kuaigeglobal.epnova.core.network.apis.request.DeleteWorkBody
import com.kuaigeglobal.epnova.core.network.apis.request.LatestEpisodeBody
import com.kuaigeglobal.epnova.core.response
import com.kuaigeglobal.epnova.widget.LoadStateViewHolder
import com.nhsd.basic.ext.showToast
import kotlinx.coroutines.flow.Flow

/**
 * 剧集详情页
 */
class SeriesViewModel : AppRepositoryViewModel<SeriesRepository>() {
    val workId by lazy { mBundle.getString(Constants.Extra.ID) }
    val loadState by lazy { ObservableInt(LoadStateViewHolder.CONTENT) }
    val workInfo by lazy { MutableLiveData<WorkInfo?>() }

    //创建续集信息
    val episodeInfo = MutableLiveData<EpisodeInfo?>()
    val publishedPlatformAdapter by lazy { QuickAdapter<ItemStringVm>(R.layout.item_publish_platform, emptyList()) }

    override fun onResume() {
        super.onResume()
        getWorkDtl()
    }

    /**
     *作品详情
     */
    fun getWorkDtl() {
        if (workInfo.value == null) loadState.set(LoadStateViewHolder.LOADING)
        launch(
            block = {
                mRepo.getWorkInfo(workId).data?.let {
                    it.userId = User.get().userId
                    workInfo.value = it
                    it.publishPlatformImageList?.map { ItemStringVm(it) }.let { publishedPlatformAdapter.submitList(it) }
                }
                updateLoadState()
            },
            onError = {
                updateLoadState()
            }
        )
    }

    private fun updateLoadState() {
        loadState.set(LoadStateViewHolder.CONTENT)
        if (workInfo.value == null) {
            loadState.set(LoadStateViewHolder.RETRY)
        } else {
            loadState.set(LoadStateViewHolder.CONTENT)
        }
    }

    fun episodeList(): Flow<PagingData<EpisodeInfo>> {
        return Pager(
            config = PagingConfig(
                initialLoadSize = PagedProxy.DEFAULT_PAGE_SIZE * 3,
                pageSize = PagedProxy.DEFAULT_PAGE_SIZE
            ),
            pagingSourceFactory = { EpisodePagingSource(workId) }
        ).flow.cachedIn(viewModelScope)
    }

    /**
     *获取剧集最新一集剧本详情
     */
    fun getEpisodeInfo(workType: Int?) {
        launchWithLoading {
            response(
                mRepo.api.latestEpisode(
                    LatestEpisodeBody(
                        workType = workType,
                        workId = workId
                    )
                )
            ) {
                episodeInfo.value = data
            }
        }
    }

    /**
     * 删除作品
     */
    fun delete() {
        if (workId.isNullOrEmpty()) return
        val workIds = listOf(workId ?: "")
        launchWithLoading {
            response(
                bean = mRepo.api.batchDeleteWork(DeleteWorkBody(ids = workIds)),
                result = {
                    launch {
                        showToast(R.string.delete_success)
                        mRepo.appDatabase.workInformationDao().deleteByWorkIds(workIds)
                        finish()
                    }
                })
        }
    }

    fun publishFailed(workInfo: WorkInfo?) = workInfo?.contractStatus == 3

    @ColorRes
    fun publishStatusColor(workInfo: WorkInfo?) = when (workInfo?.contractStatus) {
        /**[枚举]签约状态(0-未申请 1-审核中 2-已通过 3-已驳回)*/
        0 -> R.color.text_color_3_dark
        1 -> R.color.func_warning_dark
        2 -> when (workInfo.publishStatus) {
            /**[枚举]发行状态(0-未发行 1-待签约 2-发行上架中 3-已发行)*/
            0 -> R.color.text_color_3_dark
            1 -> R.color.func_warning_dark
            2 -> R.color.func_special_dark
            3 -> R.color.func_success_dark
            else -> R.color.func_failure_dark
        }

        3 -> R.color.func_failure_dark
        else -> R.color.func_failure_dark
    }

    fun publishStatusText(workInfo: WorkInfo?) = when (workInfo?.contractStatus) {
        /**[枚举]签约状态(0-未申请 1-审核中 2-已通过 3-已驳回)*/
        0 -> "未发行"
        1 -> "签约审核中"
        2 -> when (workInfo.publishStatus) {
            /**[枚举]发行状态(0-未发行 1-待签约 2-发行上架中 3-已发行)*/
            0 -> "未发行"
            1 -> "待签约"
            2 -> "发行上架中"
            3 -> "已发行"
            else -> "未知状态"
        }

        3 -> "签约失败"
        else -> "未知状态"
    }

    fun publishButtonText(workInfo: WorkInfo?) = when (workInfo?.contractStatus) {
        /**[枚举]签约状态(0-未申请 1-审核中 2-已通过 3-已驳回)*/
        0 -> "去发行"
        1 -> "签约审核中"
        3 -> "申请签约"
        else -> ""
    }

    /**
     * @param contractLevel 签约评级（S/A/B/C等）
     */
    @DrawableRes
    fun workLevelIcon(contractLevel: String?) = when (contractLevel) {
        /**[枚举]签约等级(0-普通 1-专业 2-金牌)*/
        "S" -> R.drawable.icon_work_level_s
        "A" -> R.drawable.icon_work_level_a
        "B" -> R.drawable.icon_work_level_b
        "C" -> R.drawable.icon_work_level_c
        else -> 0
    }
}